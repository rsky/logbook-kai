#!/usr/bin/env python3
"""
Interception proxy using mitmproxy (https://mitmproxy.org).
Communicates to mitmproxy-java via websockets.

This file is based on mitmproxy-java's `src/main/resources/scripts/proxy.py`.
Changes:
- Response modification has been disabled.
- Uses wsproto instead of websockets, since mitmproxy already uses wsproto, avoiding extra dependencies.
- Added support for the new mitmproxy header format, `iterable[tuple[bytes, bytes]]`.
- Code formatting applied.
- Removed unnecessary features for us.

See also: https://github.com/appium/mitmproxy-java
"""

import asyncio
import json
import os
import platform
import socket
import struct
import sys
import traceback

from wsproto import ConnectionType, WSConnection
from wsproto.events import AcceptConnection, BytesMessage, CloseConnection, Ping, Pong, RejectConnection, Request


def convert_body_to_bytes(body):
    """
    Converts an HTTP request/response body into a list of numbers.
    """
    if body is None:
        return bytes()
    else:
        return body


class WebSocketAdapter:
    """
    Relays HTTP/HTTPS requests to a websocket server.
    Enables using MITMProxy from the outside of Python.
    """

    def __init__(self):
        self.queue = asyncio.Queue()
        self.task = None

    def load(self, loader):
        self.task = asyncio.create_task(self.websocket_loop())
        if platform.system() == "Windows":
            self.send_message(
                {
                    "request": {
                        "method": "PID"
                        "url": "/"
                        "headers": [],
                    },
                    "response": {
                        "status_code": os.getpid(),
                        "headers": [],
                    },
                },
                b"",
                b"",
            )

    def send_message(self, metadata, data1, data2):
        """
        Sends the given message on the WebSocket connection,
        and awaits a response. Metadata is a JSONable object,
        and data is bytes.
        """
        metadata_bytes = bytes(json.dumps(metadata), "utf8")
        data1_size = len(data1)
        data2_size = len(data2)
        metadata_size = len(metadata_bytes)

        msg = struct.pack(
            "<III" + str(metadata_size) + "s" + str(data1_size) + "s" + str(data2_size) + "s",
            metadata_size,
            data1_size,
            data2_size,
            metadata_bytes,
            data1,
            data2,
        )
        self.queue.put_nowait(msg)

    def response(self, flow):
        """
        Intercepts an HTTP response. Mutates its headers / body / status code / etc.
        """
        request = flow.request
        # Only handles .kancolle-server.com
        if not request.host.endswith(".kancolle-server.com"):
            return

        response = flow.response
        self.send_message(
            {
                "request": {
                    "method": request.method,
                    "url": request.url,
                    "headers": list(request.headers.items(True)),
                },
                "response": {
                    "status_code": response.status_code,
                    "headers": list(response.headers.items(True)),
                },
            },
            convert_body_to_bytes(request.content),
            convert_body_to_bytes(response.content),
        )

    async def done(self):
        """
        Called when MITMProxy is shutting down.
        """
        await self.queue.join()
        self.task.cancel()
        try:
            await self.task
        except asyncio.CancelledError:
            pass  # Expected when cancelling
        except Exception:
            print("[mitmproxy-logbook-kai addon] Unexpected error:", sys.exc_info())

    async def websocket_loop(self):
        """
        Processes messages from self.queue until mitmproxy shuts us down.
        """
        while True:
            retry_delay = 0
            max_retries = 5
            try:
                loop = asyncio.get_running_loop()
                with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as conn:
                    conn.setblocking(False)
                    await loop.sock_connect(conn, ("127.0.0.1", 8765))
                    retry_delay = 0

                    # Negotiate WebSocket opening handshake
                    ws = WSConnection(ConnectionType.CLIENT)
                    await send_data(ws.send(Request(host="127.0.0.1", target="/")), conn, loop)
                    await receive_data(ws, conn, loop)
                    await handle_events(ws, conn, loop)

                    while True:
                        # Make sure the connection is still live.
                        await send_data(ws.send(Ping()), conn, loop)
                        await receive_data(ws, conn, loop)
                        await handle_events(ws, conn, loop)

                        msg = await self.queue.get()
                        try:
                            await send_data(ws.send(BytesMessage(data=msg)), conn, loop)
                            await receive_data(ws, conn, loop)
                            await handle_events(ws, conn, loop)
                        finally:
                            self.queue.task_done()
            except ConnectionAbortedError:
                print(f"[mitmproxy-logbook-kai addon] Connection aborted, reconnecting...")
            except ConnectionRefusedError:
                if retry_delay <= max_retries:
                    print(f"[mitmproxy-logbook-kai addon] Connection refused, retrying in {retry_delay} seconds...")
                    await asyncio.sleep(retry_delay)
                    retry_delay += 1
                else:
                    print("[mitmproxy-logbook-kai addon] Max retries exceeded, breaking websocket loop")
                    break
            except Exception:
                print("[mitmproxy-logbook-kai addon] Unexpected error:", sys.exc_info())
                traceback.print_exc(file=sys.stdout)


async def send_data(data, conn, loop):
    await loop.sock_sendall(conn, data)


BUFF_SIZE = 4096


async def receive_data(ws, conn, loop):
    data = bytearray()
    while True:
        part = await loop.sock_recv(conn, BUFF_SIZE)
        if not part:
            break
        data.extend(part)
        if len(part) < BUFF_SIZE:
            break
    if len(data) == 0:
        ws.receive_data(None)
    else:
        ws.receive_data(data)


async def handle_events(ws, conn, loop) -> None:
    for event in ws.events():
        if isinstance(event, AcceptConnection):
            print("WebSocket negotiation complete")
        elif isinstance(event, CloseConnection):
            print("WebSocket connection closed")
        elif isinstance(event, RejectConnection):
            print("WebSocket connection rejected")
        elif isinstance(event, BytesMessage):
            # print(f"Received bytes message: length={len(event.data)}")
            pass
        elif isinstance(event, Ping):
            # print(f"Received ping: {event.payload!r}")
            await send_data(ws.send(Pong(payload=event.payload)), conn, loop)
        elif isinstance(event, Pong):
            # print(f"Received pong: {event.payload!r}")
            pass
        else:
            print(f"Unexpected event: {str(event)}")


addons = [WebSocketAdapter()]
