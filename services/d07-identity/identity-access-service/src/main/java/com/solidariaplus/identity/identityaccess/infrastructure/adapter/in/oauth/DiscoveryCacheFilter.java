package com.solidariaplus.identity.identityaccess.infrastructure.adapter.in.oauth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
class DiscoveryCacheFilter extends OncePerRequestFilter {

    private static final String CACHE_HEADER_VALUE = "max-age=86400, must-revalidate, public";

    private static final Set<String> CACHED_PATHS =
            Set.of("/oauth2/jwks", "/.well-known/oauth-authorization-server", "/.well-known/openid-configuration");

    static final RequestMatcher CACHED_REQUESTS = request -> CACHED_PATHS.contains(
            request.getRequestURI().substring(request.getContextPath().length()));

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (CACHED_REQUESTS.matches(request)) {
            response.setHeader(HttpHeaders.CACHE_CONTROL, CACHE_HEADER_VALUE);
        }
        chain.doFilter(request, response);
    }
}
