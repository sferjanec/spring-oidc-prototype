package com.example.oauthclient;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class GatewayHostValidationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(GatewayHostValidationFilter.class);

    @Value("${gw.security.allowed-domain:.localhost}")
    private String allowedDomainSuffix;

    @Value("${gw.security.allow-internal-hosts:true}")
    private boolean allowInternalHosts;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String hostHeader = request.getHeader("Host");
        String xForwardedHost = request.getHeader("X-Forwarded-Host");
        String xForwardedProto = request.getHeader("X-Forwarded-Proto");

        log.info("🌐 Incoming Request - URI: {}, Host: {}, X-Forwarded-Host: {}, X-Forwarded-Proto: {}", request.getRequestURI(), hostHeader, xForwardedHost, xForwardedProto);

        if (hostHeader != null) {
            String domainOnly = hostHeader.split(":")[0];

            if (!isAllowed(domainOnly)) {
                log.warn("🚫 Gateway Simulation: Rejected request for host '{}'", domainOnly);
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Host not allowed by gateway configuration");
                return;
            }
            log.debug("✅ Gateway Simulation: Allowed request for host '{}'", domainOnly);
        }

        filterChain.doFilter(request, response);
    }

    private boolean isAllowed(String domainOnly) {
        // 1. Check allowed domain suffix (e.g., .localhost or your company's domain)
        if (domainOnly.equals(allowedDomainSuffix.substring(1)) || domainOnly.endsWith(allowedDomainSuffix)) {
            return true;
        }

        // 2. Check internal hosts fallback for local dev
        if (allowInternalHosts && (domainOnly.equals("localhost") || domainOnly.startsWith("127.") || domainOnly.endsWith(".localhost"))) {
            return true;
        }

        return false;
    }
}