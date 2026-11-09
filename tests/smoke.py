"""Exercise the packaged app and the Azure v1 contract with a local HTTP stub."""
import json
import os
import subprocess
import tempfile
import threading
import time
import http.cookiejar
import urllib.error
import urllib.request
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path

from test_support import java_executable


def main():
    root = Path(__file__).resolve().parents[1]
    jar = root / "src/ai-shopping-cart-service/target/ai-shopping-cart-service-1.0.0-SNAPSHOT.jar"
    captured = []
    mode = "valid"


    class AzureStub(BaseHTTPRequestHandler):
        def log_message(self, *args):
            pass

        def do_POST(self):
            body = json.loads(self.rfile.read(int(self.headers["Content-Length"])))
            captured.append((self.path, self.headers.get("Authorization"), body))
            recipe = {"name": "Spinach and salmon", "ingredients": ["spinach", "salmon"],
                      "instructions": ["Cook and serve."], "nutriscore": "A"}
            answer = {"recipes": [recipe, recipe, recipe]} if "chef" in body["messages"][0]["content"] else {
                "nutriscore": "A", "explanation": "Example", "recommendation": "Example"}
            content = json.dumps(answer) if mode == "valid" else "invalid JSON"
            payload = json.dumps({"model":"gpt-5-mini-2025-08-07", "usage":{"prompt_tokens":234,"completion_tokens":56}, "choices": [{"finish_reason": "stop", "message": {"content": content}}]}).encode()
            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.end_headers()
            self.wfile.write(payload)


    server = ThreadingHTTPServer(("127.0.0.1", 18891), AzureStub)
    threading.Thread(target=server.serve_forever, daemon=True).start()
    base = "http://127.0.0.1:18890"


    opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))

    def request(path, body=None, method=None):
        data = json.dumps(body).encode() if body is not None else None
        req = urllib.request.Request(base + path, data=data, method=method,
                                     headers={"Content-Type": "application/json"})
        try:
            with opener.open(req, timeout=20) as response:
                return response.status, response.read().decode()
        except urllib.error.HTTPError as error:
            return error.code, error.read().decode()


    env = dict(os.environ, RAG_ENABLED="false", CHAT_AI_ENABLED="false", SPRING_PROFILES_ACTIVE="local", SHOPPING_CART_AI_MOCK="false",
               AZURE_OPENAI_ENDPOINT="http://127.0.0.1:18891", AZURE_OPENAI_API_KEY="local-test-only",
               AZURE_OPENAI_DEPLOYMENT_ID="test-deployment", SERVER_PORT="18890", SERVER_ADDRESS="127.0.0.1")
    java = java_executable()
    with tempfile.TemporaryFile() as log:
        process = subprocess.Popen([java, "-jar", str(jar)], env=env, stdout=log, stderr=log)
        try:
            for _ in range(60):
                try:
                    if request("/health")[0] == 200:
                        break
                except OSError:
                    pass
                if process.poll() is not None:
                    log.seek(0)
                    raise RuntimeError(log.read().decode()[-4000:])
                time.sleep(0.5)
            else:
                raise RuntimeError("Application did not become ready")
            assert request("/")[0] == 200
            assert "window.location.origin" in request("/env-config.js")[1]
            info=json.loads(request("/api/ai-activity")[1]); assert info["provider"]=="azure" and info["requests"]==0 and info["lastRequest"] is None
            assert request("/api/cart-items/top-3-recipes")[0] == 400
            for name in ["spinach", "salmon"]:
                assert request("/api/cart-items", {"name": name, "category": "food", "quantity": 1}, "POST")[0] == 201
            assert len(json.loads(request("/api/cart-items")[1])) == 2
            recipes = request("/api/cart-items/top-3-recipes")
            assert recipes[0] == 200 and len(json.loads(recipes[1])["recipes"]) == 3
            nutrition = request("/api/cart-items/ai-nutrition-analysis")
            assert nutrition[0] == 200 and json.loads(nutrition[1])["nutriscore"] == "A"
            info=json.loads(request("/api/ai-activity")[1]); last=info["lastRequest"]
            assert info["requests"]==2 and info["model"]=="gpt-5-mini-2025-08-07" and last["status"]=="SUCCESS" and last["inputTokens"]==234 and last["outputTokens"]==56 and last["durationMs"]>=0
            for path, auth, body in captured:
                assert path == "/openai/v1/chat/completions"
                assert auth == "Bearer local-test-only"
                assert body["model"] == "test-deployment"
                assert body["max_completion_tokens"] == 3072
                assert body["reasoning_effort"] == "minimal"
                assert "temperature" not in body and "top_p" not in body
                assert "spinach, salmon" in body["messages"][1]["content"]
            mode = "malformed"
            assert request("/api/cart-items/ai-nutrition-analysis")[0] == 502
            info=json.loads(request("/api/ai-activity")[1]); assert info["requests"]==3 and info["lastRequest"]["status"]=="FAILED"
            assert request("/api/cart-items", method="DELETE")[0] == 200
            assert json.loads(request("/api/cart-items")[1]) == []
            print("PASS: packaged frontend, same-origin routing, cart CRUD, recipe/nutrition JSON, Azure v1 request, malformed-response handling, and session-scoped actual AI activity/token usage.")
        finally:
            process.terminate()
            try:
                process.wait(timeout=15)
            except subprocess.TimeoutExpired:
                process.kill()
                process.wait()
            server.shutdown()


if __name__ == "__main__":
    main()
