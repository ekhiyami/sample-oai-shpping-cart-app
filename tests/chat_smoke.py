"""Black-box tests against the packaged app: feature switch, CSRF, isolation and preview."""
import http.cookiejar
import json
import os
from pathlib import Path
import subprocess
import tempfile
import time
import urllib.error
import urllib.request

import test_support
from test_support import session, request, java_executable


def main():
    root = Path(__file__).resolve().parents[1]
    jar = root / "src/ai-shopping-cart-service/target/ai-shopping-cart-service-1.0.0-SNAPSHOT.jar"
    base = "http://127.0.0.1:18892"
    test_support.base = base
    java = java_executable()

    for enabled in ("false", "true"):
        env = dict(os.environ, RAG_ENABLED="false", CHAT_AI_ENABLED="false", SPRING_PROFILES_ACTIVE="local", SHOPPING_CART_AI_MOCK="true",
                   SERVER_PORT="18892", SERVER_ADDRESS="127.0.0.1", CHAT_ENABLED=enabled,
                   CHAT_OWNER_ID="test-owner", CHAT_ORIGIN=base)
        with tempfile.TemporaryFile() as log:
            p = subprocess.Popen([java, "-jar", str(jar)], env=env, stdout=log, stderr=log)
            try:
                a, b = session(), session()
                for _ in range(90):
                    try:
                        if request(a, "/health")[0] == 200: break
                    except OSError: pass
                    if p.poll() is not None:
                        log.seek(0); raise RuntimeError(log.read().decode()[-2000:])
                    time.sleep(.4)
                else: raise AssertionError("App unavailable")
                assert request(a, "/api/chat/config", owner=None)[0] == 403
                assert request(a, "/api/chat/config", owner="another-owner")[0] == 403
                config = request(a, "/api/chat/config")[1]
                if enabled == "false":
                    assert config == {"enabled": False}
                    assert request(a, "/api/chat", "POST", {"message": "hello"})[0] == 404
                    assert request(a, "/api/cart-items")[0] == 200
                    continue
                token = config["csrfToken"]
                token_b = request(b, "/api/chat/config")[1]["csrfToken"]
                assert token != token_b
                for bad in (None, "invalid", token_b):
                    assert request(a, "/api/chat", "POST", {"message": "hello"}, bad)[0] == 403
                assert request(a, "/api/chat", "POST", {"message": "hello"}, token, origin="https://other.example")[0] == 403
                for message in ("", " ", "x" * 2001, None):
                    assert request(a, "/api/chat", "POST", {"message": message}, token)[0] == 400
                result = request(a, "/api/chat", "POST", {"message": "<script>test</script>"}, token, origin=base)
                assert result[0] == 200 and result[1]["generationSent"] is False and result[1]["ragUsed"] is False
                assert "Preview" in result[1]["answer"]
                assert len(request(a, "/api/chat")[1]["messages"]) == 2
                assert request(b, "/api/chat")[1]["messages"] == []
                for i in range(8):
                    assert request(a, "/api/chat", "POST", {"message": f"message {i}"}, token)[0] == 200
                assert len(request(a, "/api/chat")[1]["messages"]) == 12
                assert request(a, "/api/ai-activity")[1]["requests"] == 0
                assert request(a, "/api/chat", "DELETE")[0] == 403
                assert request(a, "/api/chat", "DELETE", token=token)[0] == 200
                assert request(a, "/api/chat")[1]["messages"] == []
            finally:
                p.terminate()
                try: p.wait(timeout=15)
                except subprocess.TimeoutExpired: p.kill(); p.wait()
    print("PASS: chat switch, owner restriction, CSRF and origin rejection, validation, session isolation, bounded history, reset, and zero preview AI requests.")


if __name__ == "__main__":
    main()
