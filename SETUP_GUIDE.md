# Setup Guide: Mocking Okta OIDC Flow with Spring Boot & Angular

This guide documents the steps taken to scaffold a local environment for simulating and reproducing an Okta OIDC authentication flow where a Spring backend intercepts the token exchange and issues a `302 Redirect` back to an Angular frontend.

## 🎯 Architecture Overview

1. **Mock IDP (Spring Authorization Server)**: Simulates Okta. Runs on `http://localhost:9000`.
2. **Spring Client Backend (OAuth2 Client)**: Handles the OAuth2 flow and proxy requests. Runs on `http://localhost:8080`.
3. **Angular Frontend**: The single-page application hosted at `http://sso-peanut.localhost:4200` locally. Uses a proxy to forward authentication requests.

---

## ✅ To-Do List (Executed)

- [x] Create Mock IDP (Spring Auth Server)
- [x] Create Spring Client Backend
- [x] Create Angular Frontend
- [x] Connect Auth Flow and Proxy

---

## 🛠️ Prerequisites Installed

If you are starting fresh on a WSL environment, the following commands were used to install the required tooling:

**1. Install Java 17 and Maven:**
```bash
sudo apt update
sudo apt install openjdk-17-jdk maven -y
```

**2. Install Node.js (via NVM):**
```bash
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.7/install.sh | bash
export NVM_DIR="$HOME/.nvm"
[ -s "$NVM_DIR/nvm.sh" ] && \. "$NVM_DIR/nvm.sh"
nvm install 20
nvm use 20
```

**3. Install Angular CLI:**
```bash
npm install -g @angular/cli@17
```

---

## 🚀 Step-by-Step Implementation

### Step 1: Create Mock IDP (Spring Auth Server)
We set up a lightweight OIDC provider using `spring-security-oauth2-authorization-server`. 
- **Goal:** Create a static OIDC server with an in-memory user (`user`/`password`) and an in-memory client (`spring-client`) pointing to our application's redirect URIs.
- **Port:** `9000`
- **Actions:** Scaffolding the `pom.xml`, setting `application.yml` port, configuring the `SecurityConfig.java` to expose the JWK Source, register the client, and enable OpenID.

### Step 2: Create Spring Client Backend
We created the OAuth2 client using `spring-boot-starter-oauth2-client`.
- **Goal:** Exchange the callback code from the IDP for a token, then successfully redirect the user.
- **Port:** `8080`
- **Actions:** 
  - Configured `application.yml` provider `issuer-uri` to point to `http://localhost:9000`.
  - Created a `CustomAuthenticationSuccessHandler` extending `SavedRequestAwareAuthenticationSuccessHandler`. 
  - Overrode `onAuthenticationSuccess` to issue a redirect directly to `http://sso-peanut.localhost:4200/login/callback`.

### Step 3: Create Angular Frontend
We initialized an Angular frontend that utilizes the Angular dev-server proxy to route traffic.
- **Goal:** Host the UI on `sso-peanut.localhost:4200` and seamlessly connect to the backend.
- **Commands run:**
  ```bash
  mkdir -p angular && cd angular
  npx -y @angular/cli@17 new frontend --directory=. --routing=true --style=css --ssr=false --skip-git --interactive=false
  ```
- **Actions:**
  - Created `proxy.conf.json` to route `/oauth2` and `/login` paths to `http://127.0.0.1:8080`.
  - Updated `angular.json` to serve on host `sso-peanut.localhost` and include the proxy configuration.
  - Added a "Login" button and a custom callback component at `/login/callback`.

### Step 4: Connect Auth Flow and Proxy
To ensure the `sso-peanut.localhost` domain resolves locally, you must map it in your Windows host file.

**Windows `C:\Windows\System32\drivers\etc\hosts` Configuration:**
```text
127.0.0.1  sso-peanut.localhost
```
*(Note: If you are making curl requests strictly inside WSL, also add this to `/etc/hosts` in WSL).*

### Step 5: Gateway / Proxy Header Simulation
In an enterprise environment (like behind a Spring Cloud Gateway), the backend must construct redirect URIs based on the initial host requested by the client, not standard `localhost`.
- **Angular Updates:** Added `"startOAuth2": "ng serve --host sso-peanut.localhost --proxy-config proxy.conf.json --disable-host-check"` to `package.json`. This tells the Angular dev server to run on the specific custom host and disable host-checking to prevent "Invalid Host Header" issues.
- **Spring Boot Updates:** Added `forward-headers-strategy: framework` to `application.yml`. This explicitly tells the Spring backend to respect `X-Forwarded-*` headers injected by proxies (or in this case, Angular's Dev Server proxy) when creating dynamic OIDC redirect URLs.

---

## ▶️ Running the Application

To test this locally, open three separate WSL terminals and run:

**1. Mock IDP:**
```bash
cd /home/georg/projects/spring/mock-idp
mvn spring-boot:run
```

**2. Spring Client:**
```bash
cd /home/georg/projects/spring/oauth-client
mvn spring-boot:run
```

**3. Angular App:**
```bash
cd /home/georg/projects/angular
npm run startOAuth2
```

**Testing the Flow:**
1. Navigate to: `http://sso-peanut.localhost:4200`
2. Click the "Login with Okta Mock" button.
3. You will be redirected to the Mock IDP login page (port 9000).
4. Enter credentials:
   - **Username:** `user`
   - **Password:** `password`
5. The IDP will redirect to the Spring Client backend, which exchanges the tokens securely.
6. The custom Spring filter `CustomAuthenticationSuccessHandler` triggers a `302 Redirect`.
7. You land safely back at `http://sso-peanut.localhost:4200/login/callback` on the frontend!
