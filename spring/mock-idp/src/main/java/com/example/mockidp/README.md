# Spring OIDC Proxy Prototype

This repository contains a minimal prototype designed to simulate and debug an OAuth2/OIDC authentication flow where a Spring Boot backend sits behind a proxy (like an Angular dev server or Spring Cloud Gateway) and interacts with an Identity Provider (IdP) like Okta.

The specific scenario simulated here is investigating redirect issues, specifically ensuring the Spring Client backend successfully issues a `302 Redirect` back to the frontend application after exchanging tokens with the IdP, while properly respecting proxy headers (`X-Forwarded-*`).

## 🏗️ Architecture

The project consists of three main components:

1.  **Mock IDP (`/spring/mock-idp`)**: A lightweight Spring Authorization Server running on `http://localhost:9000`. This acts as a mock Okta instance, providing a sterile environment to test the OIDC protocol.
    *   **User:** `user` / `password`
    *   **Client:** `spring-client` / `secret`

2.  **Spring Client Backend (`/spring/oauth-client`)**: A Spring Boot OAuth2 Login client running on `http://localhost:8080`.
    *   Points to the Mock IDP for authentication.
    *   Configured with `server.forward-headers-strategy: framework` to properly construct dynamic redirect URIs when behind a proxy.
    *   Includes a custom `AuthenticationSuccessHandler` that explicitly issues a 302 redirect back to the frontend's callback URL.

3.  **Angular Frontend (`/angular`)**: A standalone Angular application running on a custom localized host alias `http://sso-peanut.localhost:4200`.
    *   Uses the Angular development server (`proxy.conf.json`) as a reverse proxy, forwarding `/oauth2` and `/login` requests back to the Spring Client.
    *   Uses the Angular development server (`proxy.conf.json`) as a reverse proxy, forwarding specific authentication paths (e.g., `/oauth2/**`, `/login/oauth2/**`) to the Spring Client.
    *   Initiates the login flow and handles the final application callback.

## 🚀 Getting Started

Please see the full [SETUP_GUIDE.md](SETUP_GUIDE.md) for detailed prerequisites, step-by-step installation instructions, and configuration details (including how to set up your `.hosts` file to resolve `sso-peanut.localhost`).

### Quick Start

1.  **Map your local host**
    Add the following to your OS's hosts file (e.g., `C:\Windows\System32\drivers\etc\hosts` or `/etc/hosts`):
    ```
    127.0.0.1  sso-peanut.localhost
    ```

2.  **Start the Mock IdP**
    ```bash
    cd spring/mock-idp
    mvn spring-boot:run
    ```

3.  **Start the Spring Client**
    ```bash
    cd spring/oauth-client
    mvn spring-boot:run
    ```

4.  **Start the Angular Frontend**
    ```bash
    cd angular
    npm install
    npm run startOAuth2
    ```

5.  **Test the Flow**
    Navigate to `http://sso-peanut.localhost:4200` in your browser and click "Login with Okta Mock". Enter the mock credentials (`user`/`password`), and you should be successfully redirected back to the Angular callback page.

## 🤔 Troubleshooting

### Redirect to `127.0.0.1:8080` and "This site can't be reached"

**Symptom:** After clicking the login button from `http://sso-peanut.localhost:4200`, your browser is redirected to a URL like `http://127.0.0.1:8080/oauth2/authorization/okta` and you see a "This site can't be reached" error.

**Cause:** This is the exact problem this prototype is designed to demonstrate. It occurs because the Spring Boot backend is generating a redirect URL using its own internal address (`127.0.0.1:8080`) instead of the public-facing proxy address (`sso-peanut.localhost:4200`).

While the Spring application is correctly configured with `server.forward-headers-strategy: framework` to respect `X-Forwarded-*` headers, the standard Angular development server proxy (`proxy.conf.json`) does **not** add these headers to proxied requests by default. Without these headers, Spring has no way of knowing it's behind a proxy.

**Solution:** The Angular proxy configuration must be updated to add the `X-Forwarded-Host` and `X-Forwarded-Proto` headers. This requires switching from a static `proxy.conf.json` file to a dynamic `proxy.conf.js` file.

1.  In your `/angular` project, create or rename your proxy config to `proxy.conf.js`.
2.  Add an `onProxyReq` event handler to inject the headers. Your configuration should look similar to this:

    ```javascript
    const PROXY_CONFIG = {
      "/oauth2/**": {
        "target": "http://localhost:8080",
        "secure": false,
        "logLevel": "debug",
        "onProxyReq": (proxyReq, req, res) => {
          proxyReq.setHeader('X-Forwarded-Host', req.headers.host);
          proxyReq.setHeader('X-Forwarded-Proto', req.protocol);
        }
      },
      // Add other proxied paths like /login here if needed
    };

    module.exports = PROXY_CONFIG;
    ```

3.  Ensure your `angular.json` file's `serve` target points to this new proxy file (e.g., `"proxyConfig": "proxy.conf.js"`).
4.  Restart the Angular dev server. The redirects should now be constructed correctly.
