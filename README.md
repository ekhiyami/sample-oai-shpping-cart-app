# Sample Azure OpenAI Shopping Cart

A learning application for exploring an Azure AI shopping cart and preparing a migration assessment with [OpenAI Migration Pathways](https://github.com/openai-on-aws/openai-solutions/tree/main/migrations/openai-migration-pathways).

The repository contains React/TypeScript and Java/Spring Boot source, Azure OpenAI and Azure AI Search integrations, configuration examples with private values replaced by placeholders, and documentation of the observed Azure environment. Learners can inspect the code and prepare a migration plan without an Azure account.

## What the application contains

- Shopping cart management, recipe recommendations, and nutrition analysis.
- Azure OpenAI generation and grounded chat.
- Retrieval-augmented generation (RAG): Azure AI Search keyword and hybrid retrieval, query vectorization integration, retrieved passages, and citation checks.
- Nutrition calculations using synthetic sample facts.
- PostgreSQL configuration and App Service/Microsoft Entra authentication context.
- AI activity and retrieval traces.

The React frontend is embedded in the Spring Boot JAR and calls its backend on the same origin.

## Run the local preview

Install **JDK 17**, **Node.js 20 or later with npm**, and **Bash**. Set `JAVA_HOME` to your JDK installation if Java is not detected. On Windows, use WSL.

```bash
git clone https://github.com/ekhiyami/sample-oai-shpping-cart-app.git
cd sample-oai-shpping-cart-app
bash build-azure.sh
bash run-local.sh
```

The first build downloads dependencies. Open **http://127.0.0.1:8080** and stop the app with **Ctrl+C**. If port 8080 is occupied, use:

```bash
SERVER_PORT=8082 bash run-local.sh
```

Then open **http://127.0.0.1:8082**.

**The default preview uses mock responses. Live Azure AI calls and RAG are disabled.** It uses an in-memory H2 database, so cart changes reset when the app stops. It does not connect to the existing hosted application.

| Capability | Default local preview | Observed Azure environment |
|---|---|---|
| Cart storage | Temporary H2 database | PostgreSQL |
| Recommendations | Mock responses | Azure OpenAI enabled |
| Grounded chat and RAG | Disabled | Azure OpenAI and Azure AI Search enabled |
| Authentication | Loopback development access | Microsoft Entra through App Service Easy Auth |

The Azure integration code is included for inspection. Running live calls requires separately configured Azure resources and credentials; see [configuration](docs/configuration.md) and [Azure setup](docs/azure-setup.md). The optional `run-azure-openai.sh` launcher supports that development setup.

## Assess the migration

1. Install [OpenAI Migration Pathways](https://github.com/openai-on-aws/openai-solutions/tree/main/migrations/openai-migration-pathways) separately in your coding assistant.
2. Open this repository and read the [Azure architecture](docs/azure-architecture.md), [observed environment](docs/azure-observed-state.md), and [source fidelity](docs/source-fidelity.md).
3. Follow the [learner assignment](docs/learner-assignment.md), including its ready-to-use assessment prompt.
4. Assess the Azure-hosted configuration, including AI, RAG, PostgreSQL, authentication, and sessions. Save your report and migration plan outside this repository.

The exercise targets AWS and includes conditional choices where requirements are undecided. This repository provides the Azure source and assessment inputs; AWS implementation code and a completed assessment are excluded.

## Run the local checks

After building, install Python 3.9 or later and run:

```bash
python3 tests/run_all.py
```

Five suites cover application, chat, retrieval, nutrition, and grounded-chat behavior using local fixtures. They do not validate live Azure services.

## License

MIT. Based on a modified Azure Samples application. See [LICENSE.md](LICENSE.md) and [UPSTREAM.md](UPSTREAM.md) for attribution and documented changes.
