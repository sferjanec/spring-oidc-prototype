  # Detailed OIDC Authentication Flow (BFF Architecture)
 
This document outlines the exact step-by-step sequence of events that occur during the OAuth2 Authorization Code flow in this Backend-For-Frontend (BFF) prototype.

## Phase 1: Initiation and Proxying

1. **The Click:** The user clicks the "Login" link in the Angular frontend. The browser makes a `GET` request to `http://sso-peanut.localhost:4200/oauth2/authorization/okta`.
2. **The Proxy Intercept:** The Angular development server intercepts this via `proxy.conf.json` (`/oauth2` rule). It forwards the request to the Spring Client at `127.0.0.1:8080`, preserving the original `Host` header because `"changeOrigin": false` is set.
3. **Spring Prepares the Redirect:** The Spring Client's OAuth2 filter intercepts the `/oauth2/authorization/okta` path. It generates a random `state` and `nonce`, and dynamically constructs the `redirect_uri` (`http://sso-peanut.localhost:4200/login/oauth2/code/okta`) using the proxy's `Host` header.
4. **Off to the IDP:** Spring returns an HTTP `302 Found` to the browser, directing it to the Mock IDP's authorization endpoint with the client ID, state, and redirect URI.

## Phase 2: Authentication at the IDP

5. **The IDP Checks Parameters:** The Mock IDP (`localhost:9000`) receives the authorization request and verifies that the requested `redirect_uri` exactly matches the one registered for `spring-client` in its `SecurityConfig.java`.
6. **The Login Page:** The unauthenticated user is presented with the Mock IDP's login form. The user enters their credentials (`user`/`password`).
7. **The Authorization Code:** Upon successful authentication, the IDP generates a short-lived, single-use "Authorization Code".
8. **Back to the App:** The IDP returns a `302 Found` response, redirecting the browser back to the exact `redirect_uri` provided in Step 4, appending the `code` and `state` parameters.

## Phase 3: Token Exchange and Completion

9. **The Proxy Intercepts (Again):** The browser navigates to `http://sso-peanut.localhost:4200/login/oauth2/code/okta?code=...`. The Angular proxy matches this against the `/login/oauth2/**` rule and forwards the request (with the code) to the Spring Client.
10. **Backend-to-Backend Token Exchange:** The Spring Client receives the Authorization Code. It makes a direct, backend-to-backend `POST` request to the Mock IDP's token endpoint (`http://localhost:9000/oauth2/token`), authenticating itself with its Client ID and Secret, and exchanging the code for tokens.
11. **Tokens Issued:** The Mock IDP validates the code and client credentials, and responds with an **ID Token** (user profile) and an **Access Token** (for APIs).
12. **Session Creation:** Spring validates the ID Token, creates a `SecurityContext` for the user, establishes a session, and sets a `JSESSIONID` cookie in the browser. The Spring backend now considers the user authenticated.
13. **The Final Redirect:** Spring Security issues a final `302 Found` redirect to the browser, pointing to the frontend success page: `http://sso-peanut.localhost:4200/login/callback`.
14. **Angular Takes Over:** The browser requests the callback URL. The Angular proxy ignores it (as it doesn't match the specific OAuth2 proxy rules), allowing the Angular Router to load the `CallbackComponent` and display the success message.

## Using this for Debugging

When debugging similar flows in a work environment, check the following:

*   **Phase 1:** Does the initial 302 from the BFF have the correct public proxy address in the `redirect_uri` parameter? If not, check proxy headers or `changeOrigin` settings.
*   **Phase 2:** Does the IDP reject the request? Check the IDP's allowed redirect URIs list to ensure it matches the proxy's public URI.
*   **Phase 3:** After the IDP redirects back with the `?code=...`, does the UI show a 404? Ensure the frontend proxy is specifically routing the OIDC callback URL to the backend, without swallowing the final application callback.

## 💡 Deep Dive & Common Questions

**Q: I thought the IDP redirects directly back to the Spring app. Why does it go to Angular first?**
Conceptually, the authorization code *must* get to the Spring app. However, the IDP (like Okta) has no knowledge of your internal network; it only knows the public-facing URL (`http://sso-peanut.localhost:4200`). Therefore, the IDP redirects the user's *browser* there. 
This is where the BFF proxy shines: The Angular dev server intercepts this specific callback request (e.g., `/login/oauth2/code/okta`) via the proxy rules and forwards it seamlessly to the internal Spring backend (`http://127.0.0.1:8080`).

**Q: Does the IDP issue the ID and Access tokens immediately when the user logs in?**
No! This is the most critical security feature of the **Authorization Code Flow**. When the user logs in, the IDP only issues a short-lived, single-use **Authorization Code** (the `?code=...` parameter in the URL). Tokens are never sent to the browser where they could be intercepted by malicious scripts (XSS) or browser extensions. 
Instead, the Spring backend takes that code and makes a secure, backend-to-backend call to the IDP's `/token` endpoint (authenticating itself with its Client Secret) to exchange it for the actual tokens.

**Q: When exactly do we redirect back to the normal Angular app?**
This is the very last step. After Spring receives the code (Step 10), exchanges it for tokens (Step 11), and creates a secure session (Step 12), its authentication job is done. It then issues one final `302 Redirect` to a "clean" URL meant for the frontend: `http://sso-peanut.localhost:4200/login/callback`. Because this path does *not* match our specific OAuth2 proxy rules, the proxy ignores it, and the Angular router finally takes over to display the UI.

## 🏢 Enterprise Architecture Variant (Work Simulation)

In enterprise environments, this flow is often modified to be completely **stateless**, removing the need for server-side sessions (`JSESSIONID`). Here is how those custom components map to our established phases:

### Modified Phase 1: Gateway & Cookie Storage
*   **Step 2 (The Gateway):** Instead of the Angular dev server acting as the proxy, an **API Gateway** routes the traffic to Spring. It intercepts the request and explicitly injects the `X-Forwarded-Host` header.
*   **Step 3 (`saveAuthorizationRequest`):** Right before Spring issues the redirect to the IDP, it delegates to a `CookieBasedOAuth2AuthorizationRequestRepository`. Instead of saving the request context (state, nonce, frontend redirect URI) in a server-side Session, it serializes this data and stores it in the user's browser as the `oauth2_auth_request` cookie.

### Modified Phase 3: Restoration & Custom JWT
*   **Step 9 (`loadAuthorizationRequest`):** When the browser returns to the application from the IDP, it automatically includes the `oauth2_auth_request` cookie. Spring intercepts this, reads the cookie, and restores the original authorization context to validate the IDP's response.
*   **Steps 12 & 13 (`OidcAuthenticationSuccessHandler`):** Once the backend token exchange is complete, your custom success handler completely replaces the default session creation.
    1. **Custom Token Generation:** It extracts the principal (user data) from the Okta tokens and generates a new, proprietary application JWT.
    2. **Cookie Attachment:** It attaches this custom JWT to the HTTP response as an HTTP-only secure cookie.
    3. **Dynamic Redirect:** It retrieves the original frontend `redirect_uri` (saved in the `oauth2_auth_request` cookie in Phase 1) and uses Spring's `RedirectStrategy` to issue the final `302 Found` redirect back to the Angular app. 
    
The frontend is successfully loaded, and the browser now holds the custom JWT cookie for all future authenticated API calls!

### The Exact Journey of the `oauth2_auth_request` Cookie

1. **Creation (`sso-peanut.localhost`):** The user clicks "Login". The Spring backend generates the `state` (e.g., `WmJZ_Dpeu...`), puts it in an `oauth2_auth_request` cookie, and attaches it to the `302 Redirect` response.
2. **The Browser Vault:** The browser receives the redirect. Due to the **Same-Origin Policy**, it places that cookie into a secure vault labeled specifically and exclusively for `sso-peanut.localhost`.
3. **The Trip to the IDP (`localhost:9000`):** The browser navigates to the Mock IDP. It checks its vault for any cookies belonging to `localhost:9000`. It doesn't find the `oauth2_auth_request` cookie (because it's locked to `sso-peanut.localhost`), so it sends the request to the IDP *without* that cookie. (This is good—the IDP never sees your internal app state).
4. **The Return Journey:** After a successful login, the IDP returns a `302 Redirect` telling the browser to go back to `http://sso-peanut.localhost:4200/login/oauth2/code/okta?code=XYZ&state=WmJZ_Dpeu...`.
5. **Restoring the State:** The browser prepares to make the request to `sso-peanut.localhost`. It checks its vault, finds the `oauth2_auth_request` cookie sitting exactly where it left it, and automatically attaches it to the request header.
6. **Validation & Deletion:** The request hits the Spring backend. Spring deserializes the cookie and compares its `state` to the `state` URL parameter the IDP just sent. If they match perfectly, the login response is validated. Spring then deletes the cookie, as its job as "short-term memory" is officially done.
