# Authentication in the Azure source architecture

Authentication is supplied by Azure App Service Authentication (Easy Auth) and a Microsoft Entra web application. It is outside the Java application's own sign-in implementation.

Observed configuration requires authentication for application routes, excludes `/health`, redirects unauthenticated visitors to the Microsoft provider, requires HTTPS, and restricts access to an explicit principal allowlist. The application registration is single-tenant with a hosted web callback. No SPA/native redirect configuration or exposed delegated API scope was observed.

The chat controller adds a separate single-owner check: `CHAT_OWNER_ID` must match `X-MS-CLIENT-PRINCIPAL-ID` supplied through the trusted App Service authentication boundary. Chat writes additionally require the session CSRF token and check the configured origin. The existing chat owner remains the intended runtime identity; this package does not add identities, change permissions, or create a local login flow.

`azure/authsettingsV2.example.json` preserves the relevant observed structure with placeholder IDs. Two principals were listed at the platform layer, while one was configured as chat owner. These are different controls and should be assessed separately. `azure/appsettings.example.json` uses the first allowed-principal placeholder for the owner to preserve the observed relationship.

The Java profile uses secure, HttpOnly, SameSite=Lax session cookies and a 30-minute application-session timeout. The platform authentication cookie has its own configuration. Chat/activity state is process-local; cart and nutrition state are database-backed.

A directly exposed JAR does not reproduce Easy Auth. A principal header alone is not proof of an authenticated user outside that platform boundary. The loopback preview intentionally has no owner restriction and must not be used to infer the hosted access policy.

Learners assess this architecture from source and sanitized evidence. They do not need to connect to the hosted application from the local code.
