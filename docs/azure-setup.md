# Source environment and optional development setup

The assessment does not require Azure access or a running local application. Use the current architecture and sanitized hosted settings under `azure/` as the environment reference. This document describes configuration dependencies, not permission to change the hosted app.

## Hosted application configuration

The source Azure profile uses Java 17 App Service hosting, PostgreSQL, Azure OpenAI, Azure AI Search and App Service Easy Auth. The observed flags enable chat, grounded generation, retrieval and nutrition lookup; recommendation mocks are off. Identity is enforced by the platform plus a separate single chat-owner check.

`azure/appsettings.example.json` preserves verified flags with placeholders for private values. `azure/authsettingsV2.example.json` preserves the relevant observed authentication structure. These are partial reference examples, not complete provisioning exports. Learners do not need to populate them to assess the code.

## Optional local development

`bash build-azure.sh` builds the React/Java application. `bash run-local.sh` runs a limited H2/mock preview with no Azure calls, and `python3 tests/run_all.py` runs contract checks using local fixtures. The preview does not represent the deployed feature state.

For a developer-owned environment, `.env.example` and `run-azure-openai.sh` can configure a local backend to call compatible Azure services using backend-only credentials. This mode is optional and separate from the assessment. It does not authenticate learners to the hosted app. Root `.env` is loaded only by this launcher; it is not automatically loaded by Spring or the build.

## External Search assets needed to reproduce the full environment

The application adapter requires:

- A synthetic corpus with stable document/chunk IDs and versioning.
- Filterable `access_scope`, `status`, `dataset`, and `corpus_version` fields.
- Searchable `content` and `title`; retrievable `chunk_id`, `document_id`, `version`, `updated_date`, `product_ids`, and `source_file`.
- A `content_vector` field and integrated text-vectorizer configuration, including the embedding model/deployment/dimensions.
- The actual ingestion/chunking process, credentials and network access policy.

Source filters use `demo-owner`, `approved-for-demo`, `synthetic-demo`, and the configured corpus version with Search API `2024-07-01`. These are synthetic data-contract labels, not personal account identifiers.

The code has the application side of RAG. External index/vectorizer/corpus/ingestion assets are not replaced by invented local components; their absence from the package is an assessment evidence gap. No live model/search request or deployment is required by the learner workflow.
