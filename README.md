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
    *   Initiates the login flow and handles the final callback rendering.

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
