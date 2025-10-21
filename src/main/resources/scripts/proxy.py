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
import platform
import queue
import socket
import struct
import sys
import threading
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

    def websocket_thread(self):
        """
        Main function of the websocket thread. Runs the websocket event loop
        until MITMProxy shuts down.
        """
        self.worker_event_loop = asyncio.new_event_loop()
        self.worker_event_loop.run_until_complete(self.websocket_loop())

    def __init__(self):
        self.queue = queue.Queue()
        self.finished = False
        # Start websocket thread
        threading.Thread(target=self.websocket_thread).start()

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
        obj = {"lock": threading.Condition(), "msg": msg, "response": None}
        # We use the lock to marry multithreading with asyncio.
        # print("acquiring lock")
        obj["lock"].acquire()
        # print("inserting into list")
        self.queue.put(obj)
        # print("waiting")
        obj["lock"].wait()
        # print("wait finished!")

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
        return

    def done(self):
        """
        Called when MITMProxy is shutting down.
        """
        # Tell the WebSocket loop to stop processing events
        self.finished = True
        self.queue.put(None)
        return

    async def websocket_loop(self):
        """
        Processes messages from self.queue until mitmproxy shuts us down.
        """
        while not self.finished:
            try:
                with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as conn:
                    conn.setblocking(False)
                    await self.worker_event_loop.sock_connect(conn, ("127.0.0.1", 8765))

                    # Negotiate WebSocket opening handshake
                    ws = WSConnection(ConnectionType.CLIENT)
                    await send_data(ws.send(Request(host="127.0.0.1", target="/")), conn, self.worker_event_loop)
                    await receive_data(ws, conn, self.worker_event_loop)
                    await handle_events(ws, conn, self.worker_event_loop)

                    while True:
                        # Make sure the connection is still live.
                        await send_data(ws.send(Ping()), conn, self.worker_event_loop)
                        await receive_data(ws, conn, self.worker_event_loop)
                        await handle_events(ws, conn, self.worker_event_loop)

                        try:
                            obj = self.queue.get(timeout=1)
                            if obj is None:
                                break
                            try:
                                obj["lock"].acquire()
                                await send_data(ws.send(BytesMessage(data=obj["msg"])), conn, self.worker_event_loop)
                                await receive_data(ws, conn, self.worker_event_loop)
                                await handle_events(ws, conn, self.worker_event_loop)
                            finally:
                                # Always remember to wake up other threads + release lock to avoid deadlocks
                                obj["lock"].notify()
                                obj["lock"].release()
                        except queue.Empty:
                            pass
            except ConnectionAbortedError:
                print("[mitmproxy-java plugin] Connection aborted, trying to reconnect...")
            except ConnectionRefusedError:
                print("[mitmproxy-java plugin] Connection refused, breaking websocket loop")
                break
            except Exception:
                print("[mitmproxy-java plugin] Unexpected error:", sys.exc_info())
                traceback.print_exc(file=sys.stdout)

        # Workaround that the child process is not killed when shutdown on windows
        if not self.finished and platform.system() == "Windows":
            sys.exit(0)


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
