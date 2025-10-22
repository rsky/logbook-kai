"""
logbook-kaiから起動するmitmdump用アドオン
"""

import asyncio
import base64
import os
import urllib.request
from dataclasses import dataclass
from logging import getLogger

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

LOGBOOK_SCHEME: str = "http"
LOGBOOK_HOST: str = "127.0.0.1"
LOGBOOK_DEFAULT_PORT: int = 8888

HTTP_OK: int = 200

logger = getLogger(__name__)


@dataclass(frozen=True, kw_only=True, slots=True, eq=False)
class PassiveServerParams:
    path: str
    headers: dict[str, str]
    content: bytes | None


def create_params(req: Request, res: Response) -> PassiveServerParams:
    return PassiveServerParams(path=req.path, headers=create_headers(req, res), content=res.content)


def create_headers(req: Request, res: Response) -> dict[str, str]:
    headers = {
        "X-Pasv-Request-Method": req.method,
    }

    req_content_type = req.headers.get("content-type")
    res_content_type = res.headers.get("content-type")

    if (
        req.content is not None
        and req_content_type is not None
        and (
            req_content_type.startswith("text/")
            or req_content_type in {"application/json", "application/x-www-form-urlencoded"}
        )
    ):
        headers["X-Pasv-Request-Content-Type"] = req_content_type
        header_safe_body = base64.b64encode(req.content).decode("utf-8")
        headers["X-Pasv-Request-Body"] = header_safe_body

    if res.content is not None and res_content_type is not None:
        headers["Content-Type"] = res_content_type

    return headers


class LogbookKaiAddon:
    queue: asyncio.Queue[PassiveServerParams]
    task: asyncio.Task[None]
    logbook_port: int = LOGBOOK_DEFAULT_PORT

    def __init__(self) -> None:
        self.queue = asyncio.Queue()

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

        self.task = asyncio.create_task(self.worker())

    def configure(self, updated: set[str]) -> None:
        if "logbook_port" in updated:
            self.logbook_port = ctx.options.logbook_port

        if "pid_file" in updated:
            pid_file = ctx.options.pid_file
            if pid_file and os.path.exists(pid_file):
                try:
                    with open(pid_file, "w") as f:
                        f.write(str(os.getpid()))
                except OSError as e:
                    logger.error(f"Failed to write PID file: {e}")

    async def done(self) -> None:
        await self.queue.join()
        self.task.cancel()
        await asyncio.gather(self.task, return_exceptions=True)

    def response(self, flow: HTTPFlow) -> None:
        request = flow.request
        if not request.host.endswith(KANCOLLE_SERVER_SUFFIX):
            return
        if not any(request.path.startswith(prefix) for prefix in PATH_PREFIXES_TO_HANDLE):
            return

        response = flow.response
        if response is not None and response.status_code == HTTP_OK:
            self.queue.put_nowait(create_params(request, response))

    async def worker(self) -> None:
        while True:
            params = await self.queue.get()
            try:
                # 依存関係を増やさないためあえて標準ライブラリを使用
                # 通信頻度を考慮しても、単一タスクかつブロッキングI/Oで十分なはず
                # もし非同期I/Oにしたいのならmitmproxyが依存するh11とasyncioの組み合わせを検討する
                # httpxやaiohttpは別途インストールが必要になるので使わない
                req = urllib.request.Request(
                    url=f"{LOGBOOK_SCHEME}://{LOGBOOK_HOST}:{self.logbook_port}/pasv{params.path}",
                    data=params.content,
                    headers=params.headers,
                    method="POST",
                )
                with urllib.request.urlopen(req) as res:
                    if res.status != HTTP_OK:
                        logger.error(f"[mitmproxy-logbook-kai addon] Unexpected response: {res.status}")
            except Exception:
                logger.exception("[mitmproxy-logbook-kai addon] Unexpected error")
            finally:
                self.queue.task_done()


addons = [LogbookKaiAddon()]
