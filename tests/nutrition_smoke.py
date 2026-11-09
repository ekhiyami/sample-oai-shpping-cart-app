"""Numeric correctness, record freshness and persistence across restart."""
import json, os, pathlib, subprocess, tempfile, time, zipfile
import test_support
from test_support import session, request

from test_support import java_executable


def main():
    test_support.base = "http://127.0.0.1:18896"
    root = pathlib.Path(__file__).resolve().parents[1]
    jar = root/"src/ai-shopping-cart-service/target/ai-shopping-cart-service-1.0.0-SNAPSHOT.jar"
    java = java_executable()

    with tempfile.TemporaryDirectory() as directory:
        db = "jdbc:h2:file:" + directory + "/nutrition;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_ON_EXIT=FALSE"
        env = dict(os.environ, RAG_ENABLED="false", CHAT_AI_ENABLED="false",SPRING_PROFILES_ACTIVE="local",SHOPPING_CART_AI_MOCK="true",SERVER_PORT="18896",
                   SERVER_ADDRESS="127.0.0.1",CHAT_ENABLED="true",CHAT_OWNER_ID="test-owner",
                   CHAT_ORIGIN=test_support.base,NUTRITION_LOOKUP_ENABLED="true",SPRING_DATASOURCE_URL=db,
                   SPRING_JPA_HIBERNATE_DDL_AUTO="update")
        processes = []
        logs = []
        def start(artifact):
            log = tempfile.TemporaryFile();logs.append(log)
            p = subprocess.Popen([java,"-jar",str(artifact)],env=env,stdout=log,stderr=log);processes.append(p)
            client = session()
            for _ in range(100):
                try:
                    if request(client,"/health")[0] == 200:
                        if len(request(client,"/api/chat/nutrition")[1].get("records",[])) == 4: break
                except (OSError,AttributeError): pass
                if p.poll() is not None:
                    log.seek(0);raise RuntimeError(log.read().decode()[-3000:])
                time.sleep(.4)
            else: raise RuntimeError("Nutrition component not ready")
            return p,client,request(client,"/api/chat/config")[1]["csrfToken"]
        def stop(p):
            p.terminate()
            try:p.wait(timeout=15)
            except subprocess.TimeoutExpired:p.kill();p.wait()
        def portion(client,token,item,sku,grams):
            return request(client,"/api/chat/portions","POST",{"itemId":item,"productId":sku,"totalGrams":grams},token)
        try:
            p,a,token = start(jar)
            initial = request(a,"/api/chat/nutrition")[1]
            assert len(initial["records"]) == 4 and initial["cartTotals"] is None
            salmon = request(a,"/api/cart-items","POST",{"name":"salmon","quantity":2,"category":"food",
                "productId":"DEMO-SALMON-001","totalGrams":999})[1]["id"]
            snapshot = request(a,"/api/chat/nutrition")[1]
            assert snapshot["lines"][0]["productId"] is None and snapshot["cartTotals"] is None
            assert portion(a,None,salmon,"DEMO-SALMON-001",150)[0] == 403
            assert portion(a,token,salmon,"unknown",150)[0] == 400
            for grams in (0,-1,100001,1.0001):
                assert portion(a,token,salmon,"DEMO-SALMON-001",grams)[0] == 400
            status,value = portion(a,token,salmon,"DEMO-SALMON-001",150)
            assert status == 200 and value["cartTotals"]["energy_kcal"] == 300
            assert value["cartTotals"]["protein_g"] == 30 and value["cartTotals"]["sodium_mg"] == 90
            # Total grams apply to the whole line, independent of the cart's item count.
            spinach = request(a,"/api/cart-items","POST",{"name":"spinach","quantity":1,"category":"food"})[1]["id"]
            assert request(a,"/api/chat/nutrition")[1]["cartTotals"] is None
            value = portion(a,token,spinach,"DEMO-SPINACH-001",80)[1]
            assert value["cartTotals"]["energy_kcal"] == 324 and value["cartTotals"]["protein_g"] == 32.4
            assert value["cartTotals"]["sodium_mg"] == 154
            assert request(a,f"/api/cart-items/{salmon}","PUT",{"name":"salmon","quantity":3,"category":"food"})[0] == 200
            value = request(a,"/api/chat/nutrition")[1]
            line = next(x for x in value["lines"] if x["itemId"] == salmon)
            assert line["productId"] is None and line["totalGrams"] is None and value["cartTotals"] is None
            assert portion(a,token,salmon,"DEMO-SALMON-001",150)[0] == 200
            assert request(a,"/api/ai-activity")[1]["requests"] == 0
            stop(p)
            with zipfile.ZipFile(jar) as z:
                h2 = next(n for n in z.namelist() if n.startswith("BOOT-INF/lib/h2-") and n.endswith(".jar"))
                local_h2 = pathlib.Path(directory)/"h2.jar";local_h2.write_bytes(z.read(h2))
            subprocess.run([java,"-cp",str(local_h2),"org.h2.tools.Shell","-url",db,"-user","sa","-password","",
                "-sql","update nutrition_fact set record_version='1.1', energy_kcal=210 where product_id='DEMO-SALMON-001'"],
                check=True,capture_output=True)
            p,a,token = start(jar)
            value = request(a,"/api/chat/nutrition")[1]
            fact = next(x for x in value["records"] if x["productId"] == "DEMO-SALMON-001")
            assert fact["version"] == "1.1" and fact["nutrients"]["energy_kcal"] == 210 and len(value["records"]) == 4
            assert next(x for x in value["lines"] if x["itemId"] == salmon)["totals"]["energy_kcal"] == 315
            stop(p)
            print("PASS: four seeded records, exact gram calculations, no count/serving assumptions, invalid inputs, CSRF, incomplete carts, changed-cart invalidation, fresh record versions, seed idempotence, and current-version persistence across restart.")
        finally:
            for p in processes:
                if p.poll() is None:stop(p)
            for log in logs:log.close()


if __name__ == "__main__":
    main()
