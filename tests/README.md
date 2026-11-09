# Local source checks

Build first, then run `python3 tests/run_all.py` from the package root. Python uses only its standard library. Java is found through `JAVA_HOME/bin/java` or PATH.

| Suite | Scope |
|---|---|
| `smoke.py` | Packaged UI, same-origin config, cart CRUD, recommendation requests, invalid JSON, activity |
| `chat_smoke.py` | Switches, owner checks, CSRF/origin, session isolation, bounded history, preview |
| `retrieval_smoke.py` | Search request/filter contracts, retrieval gates, simulated errors |
| `nutrition_smoke.py` | Synthetic facts, portions, arithmetic, mapping invalidation, restart persistence, seed idempotence |
| `grounded_chat_smoke.py` | Retrieval-before-generation, structured output, citation/numeric validation, failures, history |

Tests start loopback servers and temporary databases and clean them up. They run sequentially on ports 18890–18901. No Azure credentials or external model/search calls are needed. Shared helpers have no import-time test execution.

The nutrition suite no longer depends on an unavailable earlier JAR and does not claim cross-release rollback validation. Live model quality, PostgreSQL integration, platform authentication, and a real Search index require separate evidence.
