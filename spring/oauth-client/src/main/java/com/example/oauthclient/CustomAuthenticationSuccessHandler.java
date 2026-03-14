package com.example.oauthclient;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomAuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws ServletException, IOException {
        
        // Setup X-Forwarded-Host or other custom headers if you want to inspect them during debugging
        // Here we intercept the successful login and issue a 302 redirect back to the Angular app
        String targetUrl = "http://sso-peanut.localhost:4200/login/callback";
        
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
