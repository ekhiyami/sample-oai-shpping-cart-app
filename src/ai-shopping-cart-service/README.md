# Azure shopping cart backend

See the [root README](../../README.md) for build and run instructions. The service uses Java 17, Spring Boot, JPA, PostgreSQL (Azure profile), H2 (local profile), and Java HTTP calls to Azure OpenAI v1 Chat Completions.

The Azure source design relies on App Service Easy Auth for Microsoft owner sign-in. The JAR alone does not supply this authentication. The local launchers bind to loopback and are practice tools.

Run the local tests with `python3 tests/run_all.py` from the package root. They use HTTP stubs rather than live Azure services.
