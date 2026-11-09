"""Exercise retrieval boundaries without invoking a real model."""
import json, os, subprocess, tempfile, threading, time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from test_support import session, request

from test_support import java_executable


def main():
    import test_support
    test_support.base = "http://127.0.0.1:18894"
    mode = "valid"
    captured = []
    class Search(BaseHTTPRequestHandler):
        def log_message(self, *args): pass
        def do_POST(self):
            body = json.loads(self.rfile.read(int(self.headers["Content-Length"])))
            captured.append((self.path, self.headers.get("api-key"), body))
            status = 403 if mode == "credentials" else 503 if mode == "embedding" and "vectorQueries" in body else 200
            row = {"chunk_id":"matched","document_id":"demo-chickpea-options","title":"Chickpea options",
                   "content":"Reduced-sodium chickpeas are a separate product.", "version":"1.1",
                   "updated_date":"2026-10-28","product_ids":["DEMO-CHICKPEAS-002"]}
            data = {"value": []} if body["search"] == "unmatched-xyz" else {"value":[row, {**row,"chunk_id":"vector-only"}]}
            if "vectorQueries" not in body and data["value"]: data = {"value":[{"chunk_id":"matched"}]}
            if mode == "malformed": data = {"value":"invalid"}
            self.send_response(status); self.send_header("Content-Type","application/json"); self.end_headers()
            self.wfile.write(json.dumps(data).encode())

    server = ThreadingHTTPServer(("127.0.0.1",18895),Search)
    threading.Thread(target=server.serve_forever,daemon=True).start()
    root = Path(__file__).resolve().parents[1]
    jar = root/"src/ai-shopping-cart-service/target/ai-shopping-cart-service-1.0.0-SNAPSHOT.jar"
    env = dict(os.environ,CHAT_AI_ENABLED="false",NUTRITION_LOOKUP_ENABLED="false",SPRING_PROFILES_ACTIVE="local",SHOPPING_CART_AI_MOCK="true",SERVER_PORT="18894",
               SERVER_ADDRESS="127.0.0.1",CHAT_ENABLED="true",CHAT_OWNER_ID="test-owner",CHAT_ORIGIN=test_support.base,
               RAG_ENABLED="true",RAG_SEARCH_ENDPOINT="http://127.0.0.1:18895",
               RAG_SEARCH_API_KEY="query-read-test",RAG_SEARCH_INDEX="nutrition-v1",RAG_CORPUS_VERSION="1.0")
    with tempfile.TemporaryFile() as log:
        p = subprocess.Popen([java_executable(),"-jar",str(jar)],env=env,stdout=log,stderr=log)
        try:
            a = session()
            for _ in range(90):
                try:
                    if request(a,"/health")[0] == 200: break
                except OSError: pass
                if p.poll() is not None:
                    log.seek(0);raise RuntimeError(log.read().decode()[-2000:])
                time.sleep(.4)
            else: raise AssertionError("App not ready")
            config = request(a,"/api/chat/config")[1]
            assert config["retrievalEnabled"] is True
            token = config["csrfToken"]
            assert request(a,"/api/chat/retrieval","POST",{"message":"chickpeas"})[0] == 403
            result = request(a,"/api/chat/retrieval","POST",{"message":"chickpeas","filter":"access_scope eq 'everyone'"},token)
            assert result[0] == 200
            value = result[1]
            assert value["generationSent"] is False and value["ragUsed"] is False and value["queryVectorized"] is True
            assert len(value["sources"]) == 1 and value["sources"][0]["version"] == "1.1"
            assert request(a,"/api/ai-activity")[1]["requests"] == 0
            for path,key,body in captured:
                assert path == "/indexes/nutrition-v1/docs/search?api-version=2024-07-01" and key == "query-read-test"
                assert "access_scope eq 'demo-owner'" in body["filter"] and "status eq 'approved-for-demo'" in body["filter"]
                assert "dataset eq 'synthetic-demo'" in body["filter"] and "corpus_version eq '1.0'" in body["filter"]
                assert "everyone" not in body["filter"]
            before = len(captured)
            value = request(a,"/api/chat/retrieval","POST",{"message":"unmatched-xyz"},token)[1]
            assert value["sources"] == [] and value["queryVectorized"] is False and len(captured) == before+1
            for failure in ("credentials","embedding","malformed"):
                mode = failure
                status,value = request(a,"/api/chat/retrieval","POST",{"message":"chickpeas"},token)
                assert status == 502 and value["generationSent"] is False
                assert request(a,"/api/cart-items")[0] == 200
                assert request(a,"/api/chat","POST",{"message":"preview"},token)[0] == 200
                assert request(a,"/api/ai-activity")[1]["requests"] == 0
            print("PASS: protected retrieval, fixed scope/status/corpus filters, evidence gate, versions, query-vectorization reporting, empty results, credential/embedding/response failures, cart isolation, and zero generation requests.")
        finally:
            p.terminate()
            try:p.wait(timeout=15)
            except subprocess.TimeoutExpired:p.kill();p.wait()
            server.shutdown()


if __name__ == "__main__":
    main()
