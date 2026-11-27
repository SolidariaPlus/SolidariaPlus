package com.solidariaplus.identity.identityaccess.infrastructure.adapter.in.oauth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * For the web client using the HttpOnly refresh cookie: injects the cookie value as the
 * {@code refresh_token} request parameter so the standard OAuth2 refresh token flow works
 * without the browser-side JavaScript ever reading the token.
 *
 * <p>Only active on {@code POST /oauth2/token} with {@code grant_type=refresh_token} from a
 * client that did not supply the token in the form body.
 */
final class RefreshCookieRequestFilter extends OncePerRequestFilter {

    private static final String TOKEN_ENDPOINT = "/oauth2/token";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (shouldInjectCookie(request)) {
            String cookieToken = RefreshCookieTokenResponseHandler.extractRefreshCookie(request);
            if (cookieToken != null) {
                chain.doFilter(new CookieInjectedRequest(request, cookieToken), response);
                return;
            }
        }
        chain.doFilter(request, response);
    }

    private static boolean shouldInjectCookie(HttpServletRequest request) {
        return "POST".equalsIgnoreCase(request.getMethod())
                && TOKEN_ENDPOINT.equals(request.getServletPath())
                && AuthorizationGrantType.REFRESH_TOKEN
                        .getValue()
                        .equals(request.getParameter(OAuth2ParameterNames.GRANT_TYPE))
                && request.getParameter(OAuth2ParameterNames.REFRESH_TOKEN) == null;
    }

    private static final class CookieInjectedRequest extends HttpServletRequestWrapper {

        private final Map<String, String[]> parameters;

        CookieInjectedRequest(HttpServletRequest request, String refreshToken) {
            super(request);
            Map<String, String[]> params = new HashMap<>(request.getParameterMap());
            params.put(OAuth2ParameterNames.REFRESH_TOKEN, new String[] {refreshToken});
            this.parameters = Collections.unmodifiableMap(params);
        }

        @Override
        public String getParameter(String name) {
            String[] values = parameters.get(name);
            return values != null && values.length > 0 ? values[0] : null;
        }

        @Override
        public Map<String, String[]> getParameterMap() {
            return parameters;
        }

        @Override
        public Enumeration<String> getParameterNames() {
            return Collections.enumeration(parameters.keySet());
        }

        @Override
        public String[] getParameterValues(String name) {
            return parameters.get(name);
        }
    }
}
