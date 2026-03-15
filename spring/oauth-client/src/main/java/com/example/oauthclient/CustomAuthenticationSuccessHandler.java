package com.example.oauthclient;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;

@Component
public class CustomAuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final CookieBasedOAuth2AuthorizationRequestRepository authorizationRequestRepository;

    public CustomAuthenticationSuccessHandler(CookieBasedOAuth2AuthorizationRequestRepository authorizationRequestRepository) {
        this.authorizationRequestRepository = authorizationRequestRepository;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        if (response.isCommitted()) {
            return;
        }

        // 1. Simulate Custom JWT Creation from Okta Principal
        if (authentication.getPrincipal() instanceof OidcUser oidcUser) {
            String simulatedJwt = "custom-app-jwt-for-" + oidcUser.getPreferredUsername();
            CookieUtils.addCookie(request, response, "APP_AUTH_TOKEN", simulatedJwt, 3600); // 1 hour expiration
        }

        // 2. Clean up the temporary authorization cookies
        super.clearAuthenticationAttributes(request);
        authorizationRequestRepository.removeAuthorizationRequest(request, response);
        CookieUtils.deleteCookie(request, response, CookieBasedOAuth2AuthorizationRequestRepository.REDIRECT_URI_PARAM_COOKIE_NAME);

        // 3. Dynamic Redirect
        Optional<String> redirectUri = CookieUtils.getCookie(request, CookieBasedOAuth2AuthorizationRequestRepository.REDIRECT_URI_PARAM_COOKIE_NAME)
                .map(Cookie::getValue);
        
        // Fallback to our standard callback if the cookie isn't present
        String targetUrl = redirectUri.orElse("http://sso-peanut.localhost:4200/login/callback");
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}