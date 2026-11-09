# Source fidelity and assessment boundaries

## Reference for this package

The reference is the original Azure application source snapshot supplied with the package, plus selected read-only portal observations of the existing hosted environment. The app was observed running. The source is not claimed to be a verified export of its deployed binary.

The following application areas are byte-for-byte unchanged from that supplied snapshot:

- Azure OpenAI recommendation client and prompt constants.
- Azure Search retrieval service and its keyword/hybrid/filter/field contracts.
- Grounded chat orchestration, evidence assembly, JSON response checks, citation and numeric checks.
- Chat routes, single-owner authorization, CSRF/origin checks, session history and traces.
- Nutrition calculation/seeding, cart/database entities and repositories.
- Azure profile, Maven dependencies, and frontend dependency lockfile.
- React application composition, chat UI, and RAG trace UI.

`azure/source-provenance.json` lists file-level SHA-256 comparisons against the supplied snapshot. Hash agreement demonstrates local snapshot fidelity, not deployed-artifact equality.

## Remaining package adaptations

- Removed unused target-provider display branches so only Azure appears in source; mock activity labels identify optional development mode. This affects activity display metadata, not AI requests or RAG orchestration.
- Browser runtime configuration emits only the public API URL with safe serialization. The build enforces same-origin frontend requests and clears obsolete generated assets.
- Updated configuration metadata descriptions to reflect actual runtime properties; these metadata entries do not change application behavior.
- Added build/local-development helpers, normalized test entry points, documentation, placeholder Azure configuration, and provenance files.

The hosted-access UI and its component were removed. The unfinished local RAG simulation and session-startup changes were never included in this distribution. No local login integration, mock RAG engine, sample Search corpus replacement, or target migration implementation is included.

## What is established about Azure

Read-only portal checks established selected runtime hosting, authentication, feature enablement, setting presence, active Search index selection, corpus version, and index field/vector metadata. See `azure-observed-state.md`. Deployment Center reported CI/CD is not configured; no deployed repository revision or artifact hash was established.

## Evidence still needed for an exact deployment assessment

- The actual deployed source revision/build inputs and artifact hash, to prove exact source parity.
- Actual model deployment/version and validated request compatibility.
- Complete Search definition, vectorizer/embedding configuration, corpus, chunking/ingestion process, and access metadata beyond the partial field/vector observations in `azure/search-observed-state.json`.
- Database version/schema state, network topology, secrets/identity operations, backups/retention, traffic and operational targets.

Do not invent these details from mock tests or placeholder settings. The source contains the application side of RAG; missing external service assets remain explicit assessment gaps. Exact deployed-build parity cannot be certified from the evidence currently available.

## Assessment configuration

Use `azure/assessment-context.json` and the verified hosted flags in `azure/appsettings.example.json`. Do not select the `local` Spring profile or infer missing deployed features from `run-local.sh`, which intentionally disables live AI and retrieval.
