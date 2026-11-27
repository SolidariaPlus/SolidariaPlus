package com.solidariaplus.identity.identityaccess.infrastructure.adapter.in.oauth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
class DiscoveryCacheFilter extends OncePerRequestFilter {

    private static final String CACHE_HEADER_VALUE = "max-age=86400, must-revalidate, public";

    private static final Set<String> CACHED_PATHS =
            Set.of("/oauth2/jwks", "/.well-known/oauth-authorization-server", "/.well-known/openid-configuration");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (CACHED_PATHS.contains(request.getRequestURI())) {
            response.setHeader(HttpHeaders.CACHE_CONTROL, CACHE_HEADER_VALUE);
        }
        chain.doFilter(request, response);
    }
}
