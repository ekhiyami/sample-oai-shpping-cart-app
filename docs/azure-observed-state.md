# Sanitized Azure observations — November 9, 2026

Read-only portal inspection found a running Azure App Service and an authenticated hosted interface. This document records selected observed configuration, with all account/resource names and identifiers replaced by placeholders. It is not a full export or a guarantee that the supplied source exactly matches the deployed binary.

| Area | Observed state | Evidence boundary |
|---|---|---|
| Hosting | Linux App Service, Java 17, Basic B1, one instance, Central US | Hosting configuration/status checked; not a capacity or availability test |
| Frontend/backend | Hosted shopping-cart interface loads after sign-in; source packages React with Spring Boot | Exact deployed artifact hash/revision not established |
| Microsoft authentication | Enabled; required except `/health`; unauthenticated users redirected; HTTPS required | Current signed-in browser access observed; a new learner account was not tested |
| Entra registration | Single tenant; web callback at `https://YOUR-APP.azurewebsites.net/.auth/login/aad/callback` | No SPA/native redirect URIs or exposed delegated API scopes observed |
| Platform authorization | Two explicitly allowed principal object IDs | Identities omitted; membership must be managed by instructor |
| Chat authorization | One separate `CHAT_OWNER_ID` | Source enforces a second owner check; platform access alone is insufficient |
| Hosted profile | `SPRING_PROFILES_ACTIVE=azure`, `SHOPPING_CART_AI_MOCK=false` | App Service setting values inspected |
| Features | `CHAT_ENABLED`, `CHAT_AI_ENABLED`, `RAG_ENABLED`, `NUTRITION_LOOKUP_ENABLED` all `true` | Hosted panel displayed grounded mode; no model/search execution was triggered for this inspection |
| Azure OpenAI | Resource endpoint and deployment setting present; API-key setting exists | Values redacted; key not revealed; UI label was GPT-5 mini, actual model response/version not verified |
| Azure AI Search | Free tier, Central US, one replica/partition; two indexes with ten indexed records each; active index selected by inspected app setting | Names redacted; no query executed |
| Active Search index | 14 fields inspected; `content_vector` has 1536 dimensions; HNSW/cosine, m=4, efConstruction=400, efSearch=500; a query vectorizer is configured | Partial metadata in `azure/search-observed-state.json`; full vectorizer/provider and ingestion details not inspected |
| Active RAG corpus | App setting filters corpus version `1.1` | Indexed record contents not exported |
| Recommendation parameters | Maximum completion tokens `3072`, reasoning effort `minimal` | Settings inspected; actual model response unverified |
| Database | Datasource/credential settings exist; source Azure profile uses PostgreSQL | Database contents/version/backup configuration not inspected |
| State | Shared cart visible; application uses session-scoped chat/activity | No workload or concurrent-user test performed |

`azure/authsettingsV2.example.json` records the relevant observed authentication structure. `azure/appsettings.example.json` mixes verified non-secret flags with placeholder values and code-derived settings needed by the source. Its companion README identifies that boundary. `azure/search-observed-state.json` adds selected deployed Search field/vector observations without private names or credentials. Neither file is a deployable copy of a private environment or an instruction to change one.

The package preserves the Azure model/search adapters, application routes, prompts, data model, single-owner chat behavior, and Azure profile. Preparation additions are local development/test helpers, sanitized examples, provenance, and documentation. The learner sign-in component was removed, and no new RAG simulation is included. Local mock mode deliberately differs from hosted live mode.

A follow-up read-only check found Deployment Center reporting CI/CD is not configured; no source revision or artifact hash was established. No live model request, Search query, database login, resource mutation, new-user authorization change, or deployment was performed as part of this inspection. Assessors must distinguish these observations from local stub tests and from proposed migration designs.
