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
