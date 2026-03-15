package com.example.oauthclient;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;

@Component
public class CookieBasedOAuth2AuthorizationRequestRepository implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    private static final Logger log = LoggerFactory.getLogger(CookieBasedOAuth2AuthorizationRequestRepository.class);

    public static final String OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME = "oauth2_auth_request";
    public static final String REDIRECT_URI_PARAM_COOKIE_NAME = "redirect_uri";
    private static final int cookieExpireSeconds = 180;

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        return CookieUtils.getCookie(request, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME)
                .map(cookie -> {
                    log.info("🍪 Successfully loaded Authorization Request from cookie!");
                    log.info("🍪 Cookie Content (Serialized): {}", cookie.getValue());
                    OAuth2AuthorizationRequest authRequest = CookieUtils.deserialize(cookie, OAuth2AuthorizationRequest.class);
                    if (authRequest != null) {
                        log.info("🍪 Deserialized Request - State: {}, Redirect URI: {}", authRequest.getState(), authRequest.getRedirectUri());
                    }
                    return authRequest;
                })
                .orElse(null);
    }

    @Override
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest, HttpServletRequest request, HttpServletResponse response) {
        if (authorizationRequest == null) {
            log.info("🍪 AuthorizationRequest is null. Deleting cookies.");
            CookieUtils.deleteCookie(request, response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME);
            CookieUtils.deleteCookie(request, response, REDIRECT_URI_PARAM_COOKIE_NAME);
            return;
        }

        String serializedReq = CookieUtils.serialize(authorizationRequest);
        log.info("🍪 Saving OAuth2 Authorization Request in cookie: {}", OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME);
        log.info("🍪 Request Details - State: {}, Redirect URI: {}", authorizationRequest.getState(), authorizationRequest.getRedirectUri());
        log.info("🍪 Cookie Content (Serialized): {}", serializedReq);
        CookieUtils.addCookie(request, response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME, serializedReq, cookieExpireSeconds);
        
        String redirectUriAfterLogin = request.getParameter(REDIRECT_URI_PARAM_COOKIE_NAME);
        if (redirectUriAfterLogin != null && !redirectUriAfterLogin.isBlank()) {
            log.info("🍪 Saving frontend redirect URI in cookie: {}", redirectUriAfterLogin);
            CookieUtils.addCookie(request, response, REDIRECT_URI_PARAM_COOKIE_NAME, redirectUriAfterLogin, cookieExpireSeconds);
        }
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request, HttpServletResponse response) {
        OAuth2AuthorizationRequest authRequest = loadAuthorizationRequest(request);
        log.info("🍪 Removing Authorization Request cookie.");
        CookieUtils.deleteCookie(request, response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME);
        return authRequest;
    }
}