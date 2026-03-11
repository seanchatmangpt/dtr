#!/usr/bin/env python3
"""Local proxy that adds auth when forwarding to upstream proxy.
Handles both:
  - HTTPS CONNECT tunneling (for https://repo.maven.apache.org etc.)
  - Plain HTTP GET/HEAD/POST (for http:// artifact repos)

Usage:
  python3 maven-proxy-auth.py &
  # Maven is auto-configured via .mvn/jvm.config proxy settings

Configure Maven to use this proxy:
  -Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=3128
  -Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=3128
  -Dhttp.nonProxyHosts=localhost|127.0.0.1
"""
import socket
import threading
import os
import base64
import select
import sys
from urllib.parse import urlparse

LOCAL_PORT = 3128
UPSTREAM = os.environ.get('https_proxy') or os.environ.get('HTTPS_PROXY') \
        or os.environ.get('http_proxy') or os.environ.get('HTTP_PROXY')

def log(msg):
    print(f"[proxy] {msg}", file=sys.stderr, flush=True)

def get_upstream():
    if not UPSTREAM:
        log("ERROR: No upstream proxy in environment (https_proxy / HTTPS_PROXY)")
        sys.exit(1)
    p = urlparse(UPSTREAM)
    return p.hostname, p.port or 3128, p.username or '', p.password or ''

def read_request_head(sock):
    """Read bytes until end of HTTP headers (\r\n\r\n)."""
    buf = b''
    sock.settimeout(10)
    while b'\r\n\r\n' not in buf and len(buf) < 65536:
        chunk = sock.recv(4096)
        if not chunk:
            break
        buf += chunk
    return buf

def pipe(a, b, timeout=300):
    """Bidirectional relay between two sockets until one closes or timeout."""
    a.setblocking(False)
    b.setblocking(False)
    while True:
        r, _, _ = select.select([a, b], [], [], timeout)
        if not r:
            break
        for s in r:
            try:
                data = s.recv(65536)
            except Exception:
                return
            if not data:
                return
            dest = b if s is a else a
            try:
                dest.sendall(data)
            except Exception:
                return

def handle_connect(client, host, port, req_head):
    """HTTPS CONNECT tunnel: forward to upstream proxy with auth."""
    proxy_host, proxy_port, user, pwd = get_upstream()
    auth = base64.b64encode(f"{user}:{pwd}".encode()).decode()
    log(f"CONNECT {host}:{port} -> upstream {proxy_host}:{proxy_port}")
    upstream = socket.socket()
    upstream.settimeout(15)
    try:
        upstream.connect((proxy_host, proxy_port))
        connect_req = (
            f"CONNECT {host}:{port} HTTP/1.1\r\n"
            f"Host: {host}:{port}\r\n"
            f"Proxy-Authorization: Basic {auth}\r\n"
            f"\r\n"
        )
        upstream.sendall(connect_req.encode())
        resp = b''
        while b'\r\n\r\n' not in resp and len(resp) < 4096:
            resp += upstream.recv(4096)
        first_line = resp.split(b'\r\n')[0]
        if b'200' in first_line:
            client.sendall(b'HTTP/1.1 200 Connection Established\r\n\r\n')
            pipe(client, upstream)
        else:
            log(f"CONNECT failed: {first_line}")
            client.sendall(b'HTTP/1.1 502 Bad Gateway\r\n\r\n')
    except Exception as e:
        log(f"CONNECT error: {e}")
        try:
            client.sendall(b'HTTP/1.1 502 Bad Gateway\r\n\r\n')
        except Exception:
            pass
    finally:
        upstream.close()

def handle_http(client, req_head):
    """Plain HTTP request: inject Proxy-Authorization and forward to upstream."""
    proxy_host, proxy_port, user, pwd = get_upstream()
    auth = base64.b64encode(f"{user}:{pwd}".encode()).decode()
    # Split headers and body separator
    head, sep, tail = req_head.partition(b'\r\n\r\n')
    lines = head.split(b'\r\n')
    request_line = lines[0].decode('utf-8', errors='ignore')
    log(f"HTTP {request_line}")
    # Remove any existing Proxy-Authorization, then inject ours
    filtered = [l for l in lines if not l.lower().startswith(b'proxy-authorization:')]
    filtered.insert(1, f'Proxy-Authorization: Basic {auth}'.encode())
    # Keep Connection: keep-alive if Maven sends it; don't break pipelining
    modified = b'\r\n'.join(filtered) + b'\r\n\r\n' + tail
    upstream = socket.socket()
    upstream.settimeout(60)
    try:
        upstream.connect((proxy_host, proxy_port))
        upstream.sendall(modified)
        # Continue relaying (handles chunked responses, large JARs, etc.)
        pipe(client, upstream, timeout=300)
    except Exception as e:
        log(f"HTTP forward error: {e}")
        try:
            client.sendall(b'HTTP/1.1 502 Bad Gateway\r\n\r\n')
        except Exception:
            pass
    finally:
        upstream.close()

def handle(client):
    try:
        req = read_request_head(client)
        if not req:
            return
        request_line = req.split(b'\r\n')[0].decode('utf-8', errors='ignore')
        parts = request_line.split()
        if len(parts) < 2:
            return
        method = parts[0].upper()
        if method == 'CONNECT':
            target = parts[1]
            if ':' in target:
                host, port_str = target.rsplit(':', 1)
                try:
                    port = int(port_str)
                except ValueError:
                    port = 443
            else:
                host = target
                port = 443
            handle_connect(client, host, port, req)
        else:
            # Plain HTTP (GET, HEAD, POST, etc.)
            handle_http(client, req)
    except Exception as e:
        log(f"Handle error: {e}")
    finally:
        try:
            client.close()
        except Exception:
            pass

if __name__ == '__main__':
    if not UPSTREAM:
        log("ERROR: https_proxy / HTTPS_PROXY / http_proxy not set")
        sys.exit(1)
    log(f"Starting on 127.0.0.1:{LOCAL_PORT} -> upstream: {UPSTREAM}")
    log("Handles: HTTPS CONNECT tunneling + plain HTTP GET/HEAD/POST")
    srv = socket.socket()
    srv.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    srv.bind(('127.0.0.1', LOCAL_PORT))
    srv.listen(128)
    log(f"Listening — configure Maven with:")
    log(f"  -Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort={LOCAL_PORT}")
    log(f"  -Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort={LOCAL_PORT}")
    log(f"  -Dhttp.nonProxyHosts=localhost|127.0.0.1")
    while True:
        try:
            c, addr = srv.accept()
            threading.Thread(target=handle, args=(c,), daemon=True).start()
        except Exception as e:
            log(f"Accept error: {e}")
