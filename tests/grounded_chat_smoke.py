"""Verify retrieval-before-generation, request contract, citations and failure boundaries."""
import json, os, pathlib, subprocess, tempfile, threading, time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
import test_support
from test_support import session, request

from test_support import java_executable


def main():
    test_support.base = "http://127.0.0.1:18900"
    search_mode = "valid"
    model_mode = "valid"
    search_calls = []
    model_calls = []
    class Provider(BaseHTTPRequestHandler):
        def log_message(self,*args): pass
        def send_json(self,status,value):
            self.send_response(status);self.send_header("Content-Type","application/json");self.end_headers()
            self.wfile.write(json.dumps(value).encode())
        def do_POST(self):
            body = json.loads(self.rfile.read(int(self.headers["Content-Length"])))
            if "/docs/search" in self.path:
                search_calls.append((self.path,self.headers.get("api-key"),body))
                if search_mode == "credentials":return self.send_json(403,{"error":"denied"})
                if search_mode == "embedding" and "vectorQueries" in body:return self.send_json(503,{"error":"embedding unavailable"})
                row = {"chunk_id":"chickpea-chunk","document_id":"demo-chickpea-options","title":"Chickpea options",
                       "content":"Regular and reduced-sodium chickpeas must be compared at the same drained weight. Synthetic demo guidance.",
                       "version":"1.1","updated_date":"2026-10-28","product_ids":["DEMO-CHICKPEAS-001","DEMO-CHICKPEAS-002"]}
                return self.send_json(200,{"value":[] if "unsupported-xyz" in body["search"] or "mars" in body["search"] else [row]})
            model_calls.append((self.path,self.headers.get("Authorization"),body))
            if model_mode == "quota":return self.send_json(429,{"error":{"message":"quota"}})
            last = body["messages"][-1]["content"]
            evidence = json.loads(last.split("CURRENT_EVIDENCE:\n",1)[1])
            question = last.split("\nCURRENT_EVIDENCE:",1)[0].lower()
            answer = "The reduced-sodium demo chickpeas have 80 mg sodium per 100 g, compared with 300 mg for the regular option."
            ids = ["S1","F1","F2"]
            if "less" in question:answer = "The reduction is 220 mg per 100 g (73.33%)."
            if "calories" in question:
                answer = "The confirmed demo cart total is 24 kcal."
                ids = [x["id"] for x in evidence["nutritionRecords"] if x["record"]["productId"] == "DEMO-SPINACH-001"]
            value = {"answer":answer,"citation_ids":ids,"needs_clarification":False}
            if model_mode == "citation":value["citation_ids"]=["invented-source"]
            if model_mode == "numeric":value["answer"]="The product contains 99999 mg sodium."
            content = "not JSON" if model_mode == "json" else json.dumps(value)
            return self.send_json(200,{"model":"gpt-5-mini-2025-08-07",
                "usage":{"prompt_tokens":1200,"completion_tokens":80},
                "choices":[{"finish_reason":"length" if model_mode == "truncated" else "stop",
                            "message":{"content":content,"refusal":"declined" if model_mode == "refusal" else None}}]})

    server = ThreadingHTTPServer(("127.0.0.1",18901),Provider)
    threading.Thread(target=server.serve_forever,daemon=True).start()
    root = pathlib.Path(__file__).resolve().parents[1]
    jar = root/"src/ai-shopping-cart-service/target/ai-shopping-cart-service-1.0.0-SNAPSHOT.jar"
    env = dict(os.environ,SPRING_PROFILES_ACTIVE="local",SHOPPING_CART_AI_MOCK="true",SERVER_PORT="18900",
               SERVER_ADDRESS="127.0.0.1",CHAT_ENABLED="true",CHAT_AI_ENABLED="true",CHAT_OWNER_ID="test-owner",
               CHAT_ORIGIN=test_support.base,NUTRITION_LOOKUP_ENABLED="true",RAG_ENABLED="true",
               RAG_SEARCH_ENDPOINT="http://127.0.0.1:18901",RAG_SEARCH_API_KEY="query-read-test",
               RAG_SEARCH_INDEX="nutrition-v2",RAG_CORPUS_VERSION="1.1",
               AZURE_OPENAI_ENDPOINT="http://127.0.0.1:18901",AZURE_OPENAI_API_KEY="local-test-only",
               AZURE_OPENAI_DEPLOYMENT_ID="test-deployment")
    java = java_executable()
    processes=[];logs=[]
    def start(rag):
        log=tempfile.TemporaryFile();logs.append(log)
        p=subprocess.Popen([java,"-jar",str(jar)],env={**env,"RAG_ENABLED":rag},stdout=log,stderr=log);processes.append(p)
        a=session()
        for _ in range(100):
            try:
                if request(a,"/health")[0] == 200 and len(request(a,"/api/chat/nutrition")[1].get("records",[])) == 4:break
            except (OSError,AttributeError):pass
            if p.poll() is not None:
                log.seek(0);raise RuntimeError(log.read().decode()[-3000:])
            time.sleep(.4)
        else:raise RuntimeError("App not ready")
        return p,a,request(a,"/api/chat/config")[1]["csrfToken"]
    def stop(p):
        p.terminate()
        try:p.wait(timeout=15)
        except subprocess.TimeoutExpired:p.kill();p.wait()
    def chat(a,token,message):
        return request(a,"/api/chat","POST",{"message":message},token)
    try:
        p,a,token=start("true")
        assert request(a,"/api/chat/config")[1]["mode"] == "grounded"
        assert chat(a,None,"chickpeas")[0] == 403 and len(model_calls)==0 and len(search_calls)==0
        status,result=chat(a,token,"Compare the sodium in the chickpea options.")
        assert status==200 and result["ragUsed"] is True and result["generationSent"] is True
        assert len(result["citations"])==3 and result["trace"]["passages"]==1 and result["trace"]["nutritionRecords"]==4
        assert result["trace"]["queryVectorized"] is True and result["trace"]["queryEmbeddingTokens"] is None
        assert result["trace"]["generationStatus"]=="SUCCESS" and result["trace"]["retrievalStatus"]=="SUCCESS"
        path,auth,body=model_calls[-1]
        assert path=="/openai/v1/chat/completions" and auth=="Bearer local-test-only"
        assert body["model"]=="test-deployment" and body["reasoning_effort"]=="minimal"
        assert body["max_completion_tokens"]==3072 and "temperature" not in body and "tools" not in body
        assert body["response_format"]["type"]=="json_schema" and body["response_format"]["json_schema"]["strict"] is True
        evidence=json.loads(body["messages"][-1]["content"].split("CURRENT_EVIDENCE:\n",1)[1])
        assert evidence["documents"][0]["version"]=="1.1" and evidence["documents"][0]["content"].startswith("Regular")
        assert evidence["comparisons"]["sodiumReductionMg"]==220 and evidence["comparisons"]["sodiumReductionPercent"]==73.33
        for path,key,query in search_calls:
            assert key=="query-read-test" and "access_scope eq 'demo-owner'" in query["filter"] and "corpus_version eq '1.1'" in query["filter"]
        history=request(a,"/api/chat")[1]
        assert history["lastTrace"]["requestId"]==result["trace"]["requestId"] and len(history["messages"][1]["citations"])==3
        result=chat(a,token,"How much less sodium?")[1]
        assert result["generationSent"] is True and len(model_calls[-1][2]["messages"])==4
        assert "Compare the sodium" in search_calls[-1][2]["search"]
        spinach=request(a,"/api/cart-items","POST",{"name":"spinach","quantity":1,"category":"food"})[1]["id"]
        assert request(a,"/api/chat/portions","POST",{"itemId":spinach,"productId":"DEMO-SPINACH-001","totalGrams":80},token)[0]==200
        result=chat(a,token,"How many calories are in my cart?")[1]
        assert "24 kcal" in result["answer"] and result["generationSent"] is True
        evidence=json.loads(model_calls[-1][2]["messages"][-1]["content"].split("CURRENT_EVIDENCE:\n",1)[1])
        assert evidence["cart"][0]["totalGrams"]==80 and evidence["calculatedCartTotals"]["energy_kcal"]==24
        request(a,f"/api/cart-items/{spinach}","PUT",{"name":"spinach","quantity":2,"category":"food"})
        before=len(model_calls); before_search=len(search_calls)
        result=chat(a,token,"Total calories in my cart?")[1]
        assert result["needsClarification"] is True and result["generationSent"] is False
        assert len(model_calls)==before and len(search_calls)==before_search
        result=chat(a,token,"unsupported-xyz")[1]
        assert result["generationSent"] is False and result["trace"]["passages"]==0
        result=chat(a,token,"What is the atmosphere on Mars?")[1]
        assert result["generationSent"] is False and result["trace"]["passages"]==0
        for failure in ("credentials","embedding"):
            search_mode=failure;before=len(model_calls)
            status,result=chat(a,token,"chickpea sodium")
            assert status==502 and result["generationSent"] is False and result["trace"]["retrievalStatus"]=="FAILED"
            assert len(model_calls)==before and request(a,"/api/cart-items")[0]==200
        search_mode="valid"
        for failure in ("citation","numeric","json","refusal","truncated","quota"):
            model_mode=failure
            before_messages=len(request(a,"/api/chat")[1]["messages"])
            status,result=chat(a,token,"chickpea sodium")
            assert status==502 and result["generationSent"] is True and result["ragUsed"] is True
            assert len(request(a,"/api/chat")[1]["messages"])==before_messages
            activity=request(a,"/api/ai-activity")[1]
            assert activity["lastRequest"]["status"]=="FAILED" and request(a,"/api/cart-items")[0]==200
        model_mode="valid"
        b=session();token_b=request(b,"/api/chat/config")[1]["csrfToken"]
        assert request(b,"/api/chat")[1]["messages"]==[] and request(b,"/api/ai-activity")[1]["requests"]==0
        assert chat(b,token,"chickpeas")[0]==403
        for _ in range(7):assert chat(a,token,"chickpea options")[0]==200
        assert len(request(a,"/api/chat")[1]["messages"])==12
        assert all(len(call[2]["messages"])<=6 for call in model_calls)
        assert request(a,"/api/ai-activity")[1]["requests"]==len(model_calls)
        stop(p)
        before=len(model_calls);before_search=len(search_calls)
        p,a,token=start("false")
        assert request(a,"/api/chat/config")[1]["mode"]=="unavailable"
        assert chat(a,token,"chickpea options")[0]==503
        assert len(model_calls)==before and len(search_calls)==before_search
        assert request(a,"/api/cart-items")[0]==200
        print("PASS: explicit retrieval and current database context before Chat Completions, strict JSON contract, verified citations/numeric values, follow-ups, updated cart, missing evidence, fail-closed retrieval switch, provider failures, session isolation, bounded history and truthful token/context telemetry.")
    finally:
        for p in processes:
            if p.poll() is None:stop(p)
        for log in logs:log.close()
        server.shutdown()


if __name__ == "__main__":
    main()
