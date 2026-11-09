# Source-package verification — November 9, 2026

| Check | Result | Scope |
|---|---|---|
| Frontend/backend build after alignment | PASS | TypeScript/Vite and Java 17/Maven combined JAR |
| Packaged source smoke check after alignment | PASS | UI assets, same-origin config, cart, Azure request contract with local HTTP fixture, activity and invalid response behavior |
| Core source comparison | PASS | RAG/search/model adapters, chat/security logic, nutrition/data models, Azure profile, and restored application/chat UI match the supplied snapshot |
| Source provenance | RECORDED | 81 source files compared; 71 unchanged and 10 documented packaging/display/metadata adaptations; see `azure/source-provenance.json` |
| Remaining original smoke suites | Prior PASS retained | Chat, retrieval, nutrition and grounded generation implementations are unchanged by this alignment; tests were previously executed with local fixtures |
| Learner-only additions | REMOVED / EXCLUDED | No hosted sign-in component, no new local RAG simulation, no simulation session-startup changes |
| Azure inspection | READ-ONLY | Hosting/authentication/feature evidence, selected model settings, active Search index and field/vector metadata, Deployment Center provenance check |
| Live model/Search queries | NOT RUN | Metadata inspection and local contract fixtures only |
| Exact deployed artifact parity | UNVERIFIED | No deployed source revision or artifact hash established |
| Source/ZIP hygiene | CHECKED | Placeholders, local paths, generated assets, source hashes and internal documentation links checked during packaging |

Build environment: Java 17.0.19, Node.js 25.5.0, npm 11.8.0, Maven 3.8.8. No Azure resources or access policies were changed. These checks are source verification, not a completed migration assessment.

The downloadable source and ZIP exclude generated application JARs, dependency caches, private `.env` files, the abandoned simulation, and prior migration outputs. Root `checksums.sha256` covers the delivered source except itself.
