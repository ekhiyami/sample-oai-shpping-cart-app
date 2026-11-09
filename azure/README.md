# Azure source-environment evidence

- `assessment-context.json`: machine-readable reference to the observed source architecture and feature state for local assessment.
- `search-observed-state.json`: selected deployed Search service/index/field/vector metadata with private names replaced; not a complete index or ingestion export.
- `source-provenance.json`: byte-level comparisons with the original supplied source snapshot. This is not a deployed-artifact hash comparison.
- `appsettings.example.json`: App Service name/value examples. Verified hosted flags are `SPRING_PROFILES_ACTIVE=azure`, `SHOPPING_CART_AI_MOCK=false`, and the four enabled feature flags. Resource names, credentials and identity values are placeholders. Token budget 3072, reasoning effort minimal, active index selection (name redacted), and corpus version 1.1 were also inspected. Model label and origin examples remain code-derived where not independently verified.
- `authsettingsV2.example.json`: relevant observed App Service Authentication structure, preserving required sign-in, HTTPS, and restricted principals. Tenant/client/principal values are placeholders.

These files are assessment inputs, not deployment scripts or exhaustive cloud exports. No action here grants access, changes Azure, or instructs learners to sign in. The current application uses its existing chat-owner identity. The first allowed-principal placeholder is also used for `CHAT_OWNER_ID`, preserving the observed relationship.

The single-tenant web registration uses a hosted callback at `https://YOUR-APP.azurewebsites.net/.auth/login/aad/callback`. Secrets remain in server/platform settings. No local SPA/native authentication client or delegated API scope was observed or added.

Do not upload examples with placeholder values to a running service. External Search ingestion/index/vectorizer assets and database contents were not exported. See the documentation for evidence boundaries.
