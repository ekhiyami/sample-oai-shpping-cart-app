# Source application walkthrough

The application implements a shared shopping cart, recipe suggestions, an original AI nutrition summary, deterministic nutrition facts, and optional grounded chat.

## Browser and backend

`src/frontend/src/App.tsx` assembles the supplied application page. There is no learner sign-in component; hosted authentication is performed by App Service Easy Auth. `ShoppingCart.tsx` manages cart actions and recommendation views; `NutritionChat.tsx` sends chat messages and displays sources. `AiActivityPanel.tsx` displays model-response metadata, counts, and latency.

`CartItemController.java` receives `/api/cart-items` requests. Spring Data repositories persist cart items and nutrition records. `AiShoppingCartServiceConfiguration.java` selects mock recommendations or the Azure OpenAI implementation.

## Generation and retrieval

`AzureOpenAiShoppingCartAiRecommendations.java` sends synchronous Java HTTP requests to Azure OpenAI `/openai/v1/chat/completions` for recipe and nutrition JSON.

`ChatController.java` manages optional chat, owner-header checks, CSRF tokens, and session history. `GroundedChatService.java` obtains current facts from `NutritionService.java`, asks `RagSearchService.java` for passages, assembles evidence and recent history, and requests a structured Azure OpenAI answer. It validates response fields, allowed citation IDs, and numeric-value presence. Some missing-information paths do not invoke generation.

`RagSearchService.java` performs lexical search followed by filtered hybrid retrieval. Azure Search must provide integrated query vectorization; the application does not call an embeddings API directly. The external index/ingestion resources are absent.

`NutritionSeed.java` loads four fictional records from `nutrition-facts.csv` when enabled. `NutritionService.java` uses decimal arithmetic and confirmed total grams. Cart counts are not weights.

## State and operations

Cart data is shared in the database. Chat/activity are separate for each server session. The app retains up to 12 messages and supplies up to four recent messages to grounded generation. `/health` is a process-health response, not a dependency readiness check.

The build embeds React assets in one Spring Boot JAR. Azure-hosted authentication is an external App Service responsibility. Inspect code/configuration to determine what additional evidence your assessment needs.
