# Azure shopping cart frontend

React/TypeScript with Vite, packaged with the Spring Boot service on the same origin. `App.tsx`, `NutritionChat.tsx`, and `RagTrace.tsx` match the supplied application snapshot. The local learner sign-in link was removed; Microsoft authentication belongs to the hosted App Service platform.

See the root README and source-fidelity document for assessment scope. A development preview may hide RAG controls when its flags are disabled; the observed hosted flags enable grounded chat and retrieval.

Runtime `REACT_APP_API_URL` and build-time `VITE_API_URL` configure a public backend URL for development. Credentials remain in the backend. Use the root build for the supported same-origin frontend/backend combination.
