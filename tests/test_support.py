"""Shared local HTTP helpers; importing this module never starts tests."""
import http.cookiejar
import json
import os
from pathlib import Path
import shutil
import subprocess
import urllib.error
import urllib.request

base = "http://127.0.0.1:18892"


def java_executable():
    java = str(Path(os.environ["JAVA_HOME"]) / "bin" / "java") if os.environ.get("JAVA_HOME") else shutil.which("java")
    if not java:
        raise RuntimeError("Java is unavailable. Install JDK 17 and set JAVA_HOME or PATH.")
    try:
        result = subprocess.run([java, "-version"], capture_output=True, text=True)
    except OSError as exc:
        raise RuntimeError("Cannot start Java. Set JAVA_HOME to a working JDK 17 installation.") from exc
    if result.returncode:
        raise RuntimeError("Cannot start Java. Set JAVA_HOME to a working JDK 17 installation.")
    return java


def session():
    return urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))

def request(client, path, method="GET", body=None, token=None, owner="test-owner", origin=None):
    headers = {"Content-Type": "application/json"}
    if owner is not None: headers["X-MS-CLIENT-PRINCIPAL-ID"] = owner
    if token is not None: headers["X-CSRF-TOKEN"] = token
    if origin: headers["Origin"] = origin
    req = urllib.request.Request(base + path, method=method, headers=headers,
                                 data=json.dumps(body).encode() if body is not None else None)
    try:
        with client.open(req, timeout=20) as r:
            text = r.read().decode()
            return r.status, json.loads(text) if text.startswith(("{", "[")) else text
    except urllib.error.HTTPError as e:
        text = e.read().decode()
        return e.code, json.loads(text) if text.startswith(("{", "[")) else text

