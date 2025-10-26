"""
logbook-kaiから起動するmitmdump用アドオン
"""

import asyncio
import base64
import enum
import os
import socket
import time
from dataclasses import dataclass
from logging import getLogger

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

LOGBOOK_HOST: str = "127.0.0.1"
LOGBOOK_DEFAULT_PORT: int = 8888

HTTP_OK: int = 200
BUFF_SIZE: int = 1024
KEEP_ALIVE_TIMEOUT: int = 15
MAX_ATTEMPTS: int = 3

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
    return PassiveServerParams(path=req.path, headers=create_headers(req, res), content=res.content)


def create_headers(req: Request, res: Response) -> list[tuple[str, str]]:
    # h11ではこれらの低水準なHTTPヘッダも自前で指定する必要がある
    headers = [
        ("Host", req.host),
        # ("Connection", "keep-alive"),  # HTTP/1.1 ではデフォルトでKeep-Alive
        ("Keep-Alive", f"timeout={KEEP_ALIVE_TIMEOUT}, max=1000"),
    ]

    req_content_type = req.headers.get("content-type")
    res_content_type = res.headers.get("content-type")

    # Content-Type & Content-Length
    if res.content is not None:
        if res_content_type is not None:
            headers.append(("Content-Type", res_content_type))
        headers.append(("Content-Length", str(len(res.content))))

    # Passive Modeカスタムヘッダ
    headers.append(("X-Pasv-Request-Method", req.method))

    if (
        req.content is not None
        and req_content_type is not None
        and (
            req_content_type.startswith("text/")
            or req_content_type in {"application/json", "application/x-www-form-urlencoded"}
        )
    ):
        headers.append(("X-Pasv-Request-Content-Type", req_content_type))
        header_safe_body = base64.b64encode(req.content).decode("utf-8")
        headers.append(("X-Pasv-Request-Body", header_safe_body))

    return headers


class LogbookKaiAddon:
    """
    mitmproxyが取得したレスポンスデータをlogbook-kai passive serverに送信するaddon。

    x-ray-proxyのXRayAddonではrequest()にて自前で実装したリトライ機能付きリクエストを
    使っているが、こちらでは上流へのリクエストはmitmproxyに任せている。
    現在の艦これサーバーはHTTP/2に対応しているため、通信エラーが減少すればリトライは
    不要となる想定。必要に応じて将来的にリトライ機能の追加を検討する。
    """

    _queue: asyncio.Queue[PassiveServerParams]
    _tasks: tuple[asyncio.Task[None], asyncio.Task[None]]
    _logbook_port: int = LOGBOOK_DEFAULT_PORT

    def load(self, loader: Loader) -> None:
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

        self._queue = asyncio.Queue()
        self._tasks = (
            asyncio.create_task(self._worker(1)),
            asyncio.create_task(self._worker(2)),
        )

    def configure(self, updated: set[str]) -> None:
        if "logbook_port" in updated:
            self._logbook_port = ctx.options.logbook_port

        if "pid_file" in updated:
            self._write_pid(ctx.options.pid_file)

    @staticmethod
    def _write_pid(pid_file: str) -> None:
        if not pid_file:
            return
        try:
            with open(pid_file, "w") as f:
                f.write(str(os.getpid()))
        except OSError as e:
            logger.error(f"Failed to write PID file: {e}")

    async def done(self) -> None:
        await self._queue.join()
        for task in self._tasks:
            task.cancel()
        await asyncio.gather(*self._tasks, return_exceptions=True)

    def response(self, flow: HTTPFlow) -> None:
        request = flow.request
        if not request.host.endswith(KANCOLLE_SERVER_SUFFIX):
            return
        if not any(request.path.startswith(prefix) for prefix in PATH_PREFIXES_TO_HANDLE):
            return

        response = flow.response
        if response is not None and response.status_code == HTTP_OK:
            self._queue.put_nowait(create_params(request, response))

    async def _worker(self, task_no: int) -> None:
        while True:
            try:
                await self._keepalive_send_to_logbook()
            except asyncio.CancelledError:
                # タスクがキャンセルされたら正常終了
                logger.info(f"[mitmproxy-logbook-kai addon] Worker#{task_no} cancelled")
                raise  # CancelledErrorを再送出して確実に終了
            except Exception:
                logger.exception("[mitmproxy-logbook-kai addon] Unexpected error")
                continue

    async def _keepalive_send_to_logbook(self) -> None:
        """
        このメソッドのwhileループをbreakすることで処理が呼び出し元のworker()に戻り、
        worker()のwhileループで再度このメソッドが呼び出されることでコネクションが作り直される。
        """
        loop = asyncio.get_running_loop()
        with socket.create_connection((LOGBOOK_HOST, self._logbook_port)) as sock:
            sock.setblocking(False)
            client = AsyncKeepAliveClient(loop, sock)

            while True:
                params = await self._queue.get()
                try:
                    result = await client.send(params)
                    match result:
                        case SendResult.SUCCESS:
                            pass
                        case SendResult.MUST_DISCONNECT:
                            break
                        case SendResult.TIMEOUT | SendResult.MUST_RETRY:
                            reason = "timeout" if result is SendResult.TIMEOUT else "error"
                            if params.attempts < MAX_ATTEMPTS:
                                logger.info(
                                    f"[mitmproxy-logbook-kai addon] Retrying {params.path} "
                                    f"(attempt {params.attempts + 1}/{MAX_ATTEMPTS}) due to connection {reason}"
                                )
                                await self._queue.put(params.clone_for_retry())
                            else:
                                logger.warning(
                                    "[mitmproxy-logbook-kai addon] Max attempts "
                                    f"({MAX_ATTEMPTS}) exceeded for {params.path}"
                                )
                            break
                finally:
                    self._queue.task_done()


class SendResult(enum.Enum):
    SUCCESS = enum.auto()
    TIMEOUT = enum.auto()
    MUST_DISCONNECT = enum.auto()
    MUST_RETRY = enum.auto()


class AsyncKeepAliveClient:
    def __init__(
        self,
        loop: asyncio.AbstractEventLoop,
        sock: socket.socket,
        timeout: int = KEEP_ALIVE_TIMEOUT,
    ) -> None:
        self._conn = h11.Connection(our_role=h11.CLIENT)
        self._loop = loop
        self._sock = sock
        self._timeout = timeout
        self._last_used_time = time.time()

    def is_timed_out(self) -> bool:
        """タイムアウトしているかチェック"""
        return time.time() - self._last_used_time > self._timeout

    async def send(self, params: PassiveServerParams) -> SendResult:
        if self.is_timed_out():
            return SendResult.TIMEOUT

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
            logger.error(f"[mitmproxy-logbook-kai addon] Connection error: {e}")
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
            await self._loop.sock_sendall(self._sock, data)

    async def get_events(self) -> list[h11.Event]:
        events: list[h11.Event] = []

        while True:
            event = self._conn.next_event()

            if event is h11.NEED_DATA:
                # 追加データが必要な場合は読み込む
                data = await self._loop.sock_recv(self._sock, BUFF_SIZE)
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
            logger.warning("[mitmproxy-logbook-kai addon] No response received")
            return False

        response = events[0]
        if not isinstance(response, h11.Response):
            logger.error("[mitmproxy-logbook-kai addon] First event is not Response")
            return False

        if response.status_code != 200:
            logger.warning(f"[mitmproxy-logbook-kai addon] Response code: {response.status_code}")

        last_event = events[-1]
        if isinstance(last_event, h11.ConnectionClosed):
            logger.info("[mitmproxy-logbook-kai addon] Connection closed")
            return False

        return True


addons = [LogbookKaiAddon()]
