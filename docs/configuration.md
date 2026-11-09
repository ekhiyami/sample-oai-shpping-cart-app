# Configuration for source assessment

Use the observed Azure profile/flags below to interpret the workload. Local settings are optional development conveniences and are not the deployed configuration.

| Setting | Observed hosted value / purpose | Optional preview |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `azure`; PostgreSQL/HTTPS hosting | `local`; H2 |
| `SHOPPING_CART_AI_MOCK` | `false` | `true` |
| `CHAT_ENABLED` | `true` | `true` |
| `CHAT_AI_ENABLED` | `true` | `false` |
| `RAG_ENABLED` | `true` | `false` |
| `NUTRITION_LOOKUP_ENABLED` | `true` | `true` |
| `CHAT_OWNER_ID` | Existing single chat-owner object ID, redacted | Blank on loopback only |
| `CHAT_ORIGIN` | Intended hosted origin; source-derived placeholder | Printed 127.0.0.1 origin |

`azure/appsettings.example.json` labels all secret/resource/identity fields with placeholders. Consult its README for values that were observed versus supplied as code-derived examples.

| Setting | Source role |
|---|---|
| `AZURE_OPENAI_ENDPOINT` | Resource URL without `/openai/v1` |
| `AZURE_OPENAI_DEPLOYMENT_ID` | Deployment name sent as `model` |
| `AZURE_OPENAI_API_KEY` | Backend credential |
| `AZURE_OPENAI_MODEL_NAME` | Display label; not proof of actual deployed model version |
| `AZURE_OPENAI_MAX_COMPLETION_TOKENS` | Recommendation budget; default 3072 |
| `AZURE_OPENAI_REASONING_EFFORT` | Recommendation reasoning parameter; default `minimal` |
| `RAG_SEARCH_ENDPOINT`, `RAG_SEARCH_API_KEY` | Search service endpoint/backend query credential |
| `RAG_SEARCH_INDEX`, `RAG_CORPUS_VERSION` | Hosted active index name is redacted; observed corpus version is `1.1`. Code defaults (`nutrition-v1` / `1.0`) are overridden by hosted settings. |
| `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` | PostgreSQL connection settings |
| `JAVA_HOME`, `SERVER_PORT` | Optional local tool settings |

Grounded generation independently sets `minimal` reasoning and 3072 completion tokens. It requests strict JSON-schema output. Recommendations request JSON-object output. Actual model compatibility remains a runtime verification requirement.

The built frontend uses the same origin as the backend. Standalone development also supports `REACT_APP_API_URL` or `VITE_API_URL`, which are public backend address settings only. No Azure/database credentials belong in frontend configuration. These settings are not a learner authentication integration.

`run-local.sh` ignores root `.env`, forces H2/loopback, and disables live AI/Search. `run-azure-openai.sh` loads `.env` as trusted Bash assignments and enables developer-configured service calls. Neither launcher is the architecture reference or a cloud-provisioning command.
