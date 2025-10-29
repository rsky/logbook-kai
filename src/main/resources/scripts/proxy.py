"""
logbook-kaiから起動するmitmdump用アドオン
"""

import asyncio
import base64
import enum
import os
import time
from dataclasses import dataclass
from logging import getLogger
from typing import Optional

import h11
from mitmproxy import ctx
from mitmproxy.addonmanager import Loader
from mitmproxy.http import HTTPFlow, Request, Response

KANCOLLE_SERVER_SUFFIX: str = ".kancolle-server.com"
PATH_PREFIXES_TO_HANDLE: tuple[str, ...] = (
    "/kcsapi/",
    "/kcs2/resources/ship/",
    "/kcs2/img/common/",
    "/kcs2/img/duty/",
    "/kcs2/img/sally/",
)

LOGBOOK_DEFAULT_HOST: str = "127.0.0.1"
LOGBOOK_DEFAULT_PORT: int = 8888

HTTP_OK: int = 200
BUFF_SIZE: int = 1024
KEEP_ALIVE_TIMEOUT: int = 15
MAX_ATTEMPTS: int = 3
QUEUE_MAX_SIZE: int = 128
CONNECTION_COUNT: int = 2

logger = getLogger(__name__)


@dataclass(frozen=True, kw_only=True, slots=True, eq=False)
class PassiveServerParams:
    path: str
    headers: list[tuple[str, str]]
    content: bytes | None
    attempts: int = 1

    def clone_for_retry(self) -> "PassiveServerParams":
        return PassiveServerParams(
            path=self.path,
            headers=self.headers,
            content=self.content,
            attempts=self.attempts + 1,
        )


def create_params(req: Request, res: Response) -> PassiveServerParams:
    return PassiveServerParams(path=req.path, headers=create_headers_by_mitmproxy(req, res), content=res.content)


def create_headers_by_mitmproxy(req: Request, res: Response) -> list[tuple[str, str]]:
    return create_headers(
        request_host=req.host,
        request_method=req.method,
        request_content_type=req.headers.get("content-type"),
        request_content=req.content,
        response_content_type=res.headers.get("content-type"),
        response_content=res.content,
    )


def create_headers(
    request_host: str,
    request_method: str,
    request_content_type: Optional[str],
    request_content: Optional[bytes],
    response_content_type: Optional[str],
    response_content: Optional[bytes],
) -> list[tuple[str, str]]:
    # h11ではこれらの低水準なHTTPヘッダも自前で指定する必要がある
    headers = [
        ("Host", request_host),
        # ("Connection", "keep-alive"),  # HTTP/1.1 ではデフォルトでKeep-Alive
        ("Keep-Alive", f"timeout={KEEP_ALIVE_TIMEOUT}, max=1000"),
    ]

    # Content-Type & Content-Length
    if response_content is not None:
        if response_content_type is not None:
            headers.append(("Content-Type", response_content_type))
        headers.append(("Content-Length", str(len(response_content))))

    # Passive Modeカスタムヘッダ
    headers.append(("X-Pasv-Request-Method", request_method))

    if (
        request_content is not None
        and request_content_type is not None
        and (
            request_content_type.startswith("text/")
            or request_content_type in {"application/json", "application/x-www-form-urlencoded"}
        )
    ):
        headers.append(("X-Pasv-Request-Content-Type", request_content_type))
        header_safe_body = base64.b64encode(request_content).decode("utf-8")
        headers.append(("X-Pasv-Request-Body", header_safe_body))

    return headers


def check_path(path: str) -> bool:
    return any(path.startswith(prefix) for prefix in PATH_PREFIXES_TO_HANDLE)


def is_passive_mode_request(req: Request) -> bool:
    return (
        req.method == "POST"
        and req.host in {"localhost", "127.0.0.1"}
        and req.path.startswith("/pasv/")
        and any(k.lower().startswith("x-pasv-") for k, _ in req.headers.items())  # type: ignore[no-untyped-call]
    )


def is_proxy_pac_request(req: Request) -> bool:
    return req.method == "GET" and req.host in {"localhost", "127.0.0.1"} and req.path == "/logbook-kai/proxy.pac"


class LogbookKaiAddon:
    """
    mitmproxyが取得したレスポンスデータをlogbook-kai passive serverに送信するaddon。

    x-ray-proxyのXRayAddonではrequest()にて自前で実装したリトライ機能付きリクエストを
    使っているが、こちらでは上流へのリクエストはmitmproxyに任せている。
    現在の艦これサーバーはHTTP/2に対応しているため、通信エラーが減少すればリトライは
    不要となる想定。必要に応じて将来的にリトライ機能の追加を検討する。
    """

    _queue: asyncio.Queue[PassiveServerParams]
    _clients: asyncio.Queue[Optional["AsyncKeepAliveClient"]]
    _tasks: tuple[asyncio.Task[None], ...]
    _logbook_host: str
    _logbook_port: int
    _logbook_hostspec: str

    def __init__(self) -> None:
        self._queue = asyncio.Queue(maxsize=QUEUE_MAX_SIZE)
        self._clients = asyncio.Queue()
        self._tasks = ()
        self._logbook_host = LOGBOOK_DEFAULT_HOST
        self._logbook_port = LOGBOOK_DEFAULT_PORT
        self._update_logbook_hostspec()

    def load(self, loader: Loader) -> None:
        loader.add_option(
            name="logbook_host",
            typespec=str,
            default=LOGBOOK_DEFAULT_HOST,
            help="Host that logbook-kai is listening on.",
        )
        loader.add_option(
            name="logbook_port",
            typespec=int,
            default=LOGBOOK_DEFAULT_PORT,
            help="Port that logbook-kai is listening on.",
        )
        loader.add_option(
            name="pid_file",
            typespec=str,
            default="",
            help="PID file path.",
        )

    def configure(self, updated: set[str]) -> None:
        if "logbook_host" in updated:
            self._logbook_host = ctx.options.logbook_host

        if "logbook_port" in updated:
            self._logbook_port = ctx.options.logbook_port

        self._update_logbook_hostspec()

        if "pid_file" in updated:
            self._write_pid(ctx.options.pid_file)

    def configure_connection(self, host: str, port: int) -> None:
        """
        x-ray-proxyのLogbookKaiConnectHandlerから接続設定を上書きする
        """
        self._logbook_host = host
        self._logbook_port = port
        self._update_logbook_hostspec()

    def _update_logbook_hostspec(self) -> None:
        self._logbook_hostspec = f"{self._logbook_host}:{self._logbook_port}"

    def running(self) -> None:
        """
        プロキシが起動完了したらクライアントのプールとレスポンスのコンシューマーを初期化する
        """
        for _ in range(CONNECTION_COUNT):
            self._clients.put_nowait(None)
        self._tasks = tuple(asyncio.create_task(self._worker(i + 1)) for i in range(CONNECTION_COUNT))

    @staticmethod
    def _write_pid(pid_file: str) -> None:
        if not pid_file:
            return
        try:
            with open(pid_file, "w") as f:
                f.write(str(os.getpid()))
        except OSError as e:
            logger.error(f"[logbook-kai-addon] Failed to write PID file: {e}")

    async def done(self) -> None:
        await self._queue.join()

        for task in self._tasks:
            task.cancel()

        await asyncio.gather(*self._tasks, return_exceptions=True)

        while not self._clients.empty():
            client = self._clients.get_nowait()
            if client is not None:
                await client.dispose()

    def request(self, flow: HTTPFlow) -> None:
        """
        特殊なリクエストをlogbook-kaiに転送する
        """
        if is_proxy_pac_request(flow.request) or is_passive_mode_request(flow.request):
            # host:portを書き換えてmitmproxyがlogbook-kaiにリクエストするよう仕向ける
            flow.request.host = self._logbook_host
            flow.request.port = self._logbook_port

    def response(self, flow: HTTPFlow) -> None:
        """
        艦これサーバーへのリクエストに対するレスポンスをlogbook-kaiに送信する
        """
        request = flow.request
        if not request.host.endswith(KANCOLLE_SERVER_SUFFIX):
            return
        if not check_path(request.path):
            return

        response = flow.response
        if response is not None and response.status_code == HTTP_OK:
            self.enqueue(create_params(request, response))

    def enqueue(self, params: PassiveServerParams) -> None:
        try:
            self._queue.put_nowait(params)
        except (asyncio.QueueFull, asyncio.QueueShutDown) as e:
            # キューが満杯もしくはシャットダウンされていたら何もしない
            logger.warning(
                f"[logbook-kai-addon] Failed to queue {params.path}: "
                f"Queue {'full' if isinstance(e, asyncio.QueueFull) else 'shutdown'}"
            )

    async def _get_client(self) -> Optional["AsyncKeepAliveClient"]:
        client = await self._clients.get()
        if client is None:
            return await self._create_client()

        if client.is_hostspec_changed(self._logbook_hostspec) or client.is_timed_out():
            # host:portが変わっているか、タイムアウトしていたら新しいクライアントを作る
            await client.dispose()
            return await self._create_client()

        return client

    async def _create_client(self) -> Optional["AsyncKeepAliveClient"]:
        try:
            reader, writer = await asyncio.open_connection(self._logbook_host, self._logbook_port)
            logger.debug(f"[logbook-kai-addon] Connected to {self._logbook_hostspec}")
            return AsyncKeepAliveClient(reader, writer, self._logbook_hostspec)
        except ConnectionRefusedError:
            logger.error(f"[logbook-kai-addon] Connection refused ({self._logbook_hostspec})")
            return None

    async def _worker(self, task_no: int) -> None:
        while True:
            try:
                await self._keepalive_send_to_logbook()
            except asyncio.CancelledError:
                # タスクがキャンセルされたら正常終了
                logger.info(f"[logbook-kai-addon worker#{task_no}] Task cancelled")
                raise  # CancelledErrorを再送出して確実に終了
            except asyncio.QueueShutDown:
                # キューがシャットダウンされたらループを抜ける
                break
            except Exception:
                # 想定外の例外はログを出力して継続
                logger.exception(f"[logbook-kai-addon worker#{task_no}] Unexpected error")
                continue

    async def _keepalive_send_to_logbook(self) -> None:
        while True:
            params = await self._queue.get()
            client = await self._get_client()
            try:
                if client is None:
                    # 接続エラーなどの理由でクライアントが作られていない場合は何もしない
                    # この場合、リトライせずに意図的にデータを破棄する
                    continue

                result = await client.send(params)
                match result:
                    case SendResult.SUCCESS:
                        pass
                    case SendResult.MUST_DISCONNECT:
                        await client.dispose()
                        client = None  # 次に _get_client が呼ばれる際にクライアントが生成される
                        continue
                    case SendResult.MUST_RETRY:
                        if params.attempts < MAX_ATTEMPTS:
                            logger.info(
                                f"[logbook-kai-addon] Retrying {params.path} "
                                f"(attempt {params.attempts + 1}/{MAX_ATTEMPTS})"
                            )
                            await self._queue.put(params.clone_for_retry())
                        else:
                            logger.warning(
                                f"[logbook-kai-addon] Max attempts ({MAX_ATTEMPTS}) exceeded for {params.path}"
                            )
                        await client.dispose()
                        client = None  # リトライ時に _get_client が呼ばれる際にクライアントが生成される
                        continue
            finally:
                # クライアントを再利用（またはリセット）する
                # コネクションプーリングとしてキューを使用しているので、task_done() は不要
                self._clients.put_nowait(client)
                self._queue.task_done()


class SendResult(enum.Enum):
    SUCCESS = enum.auto()
    MUST_DISCONNECT = enum.auto()
    MUST_RETRY = enum.auto()


class AsyncKeepAliveClient:
    _conn: h11.Connection
    _reader: asyncio.StreamReader
    _writer: asyncio.StreamWriter
    _hostspec: str
    _timeout: int
    _last_used_time: float

    def __init__(
        self,
        reader: asyncio.StreamReader,
        writer: asyncio.StreamWriter,
        hostspec: str,
        timeout: int = KEEP_ALIVE_TIMEOUT,
    ) -> None:
        self._conn = h11.Connection(our_role=h11.CLIENT)
        self._reader = reader
        self._writer = writer
        self._hostspec = hostspec
        self._timeout = timeout
        self._last_used_time = time.time()

    def is_timed_out(self) -> bool:
        """タイムアウトしているかチェック"""
        return time.time() - self._last_used_time > self._timeout

    def is_hostspec_changed(self, current_hostspec: str) -> bool:
        """logbook-kaiのhost:portが__init__時から変化しているかチェック"""
        return self._hostspec != current_hostspec

    async def dispose(self) -> None:
        self._writer.close()
        await self._writer.wait_closed()
        logger.debug(f"[logbook-kai-addon] Disconnected from {self._hostspec}")

    async def send(self, params: PassiveServerParams) -> SendResult:
        request_sent = False
        try:
            await self.send_request(params)
            request_sent = True

            events = await self.get_events()
            if not self.verify_events(events):
                return SendResult.MUST_DISCONNECT

            self._conn.start_next_cycle()
            self._last_used_time = time.time()  # 成功時に更新

            return SendResult.SUCCESS

        except (h11.RemoteProtocolError, ConnectionError) as e:
            logger.error(f"[logbook-kai-addon] Connection error: {e}")
            if not request_sent:
                return SendResult.MUST_RETRY
            else:
                return SendResult.MUST_DISCONNECT

    async def send_request(self, params: PassiveServerParams) -> None:
        req = h11.Request(
            method="POST",
            headers=params.headers,
            target=f"/pasv{params.path}",
        )
        await self.send_event(req)
        if params.content is not None:
            await self.send_event(h11.Data(params.content))
        await self.send_event(h11.EndOfMessage())

    async def send_event(self, event: h11.Event) -> None:
        data = self._conn.send(event)
        if data is not None:
            self._writer.write(data)
            await self._writer.drain()

    async def get_events(self) -> list[h11.Event]:
        events: list[h11.Event] = []

        while True:
            event = self._conn.next_event()

            if event is h11.NEED_DATA:
                # 追加データが必要な場合は読み込む
                data = await self._reader.read(BUFF_SIZE)
                if not data:
                    # 接続が閉じられた
                    break
                self._conn.receive_data(data)
                continue

            if isinstance(event, (h11.Response, h11.Data, h11.EndOfMessage, h11.ConnectionClosed)):
                events.append(event)

                # EndOfMessageまたはConnectionClosedで終了
                if isinstance(event, (h11.EndOfMessage, h11.ConnectionClosed)):
                    break
            else:
                # PAUSED or unexpected event
                break

        return events

    @staticmethod
    def verify_events(events: list[h11.Event]) -> bool:
        if not events:
            logger.warning("[logbook-kai-addon] No response received")
            return False

        response = events[0]
        if not isinstance(response, h11.Response):
            logger.error("[logbook-kai-addon] First event is not Response")
            return False

        if response.status_code != 200:
            logger.warning(f"[logbook-kai-addon] Response code: {response.status_code}")

        last_event = events[-1]
        if isinstance(last_event, h11.ConnectionClosed):
            logger.info("[logbook-kai-addon] Connection closed")
            return False

        return True


addons = [LogbookKaiAddon()]
