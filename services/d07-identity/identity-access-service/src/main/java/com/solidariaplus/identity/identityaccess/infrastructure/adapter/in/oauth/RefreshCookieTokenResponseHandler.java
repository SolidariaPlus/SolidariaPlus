package com.solidariaplus.identity.identityaccess.infrastructure.adapter.in.oauth;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.oauth2.core.http.converter.OAuth2AccessTokenResponseHttpMessageConverter;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AccessTokenAuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.web.util.WebUtils;

/**
 * Token endpoint success handler that keeps the refresh token out of the JSON body for the web
 * client and delivers it exclusively via an HttpOnly Secure SameSite=Strict cookie (__Host-refresh),
 * preventing XSS-based token theft.
 *
 * <p>All other clients receive the standard JSON response unchanged.
 */
final class RefreshCookieTokenResponseHandler implements AuthenticationSuccessHandler {

    static final String COOKIE_NAME = "__Host-refresh";
    private static final String WEB_CLIENT_ID = "solidaria-web";
    private static final String SECURE_PATH = "/";

    private final HttpMessageConverter<OAuth2AccessTokenResponse> converter =
            new OAuth2AccessTokenResponseHttpMessageConverter();

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException {
        OAuth2AccessTokenAuthenticationToken auth = (OAuth2AccessTokenAuthenticationToken) authentication;
        boolean isWebClient = WEB_CLIENT_ID.equals(auth.getRegisteredClient().getClientId());

        if (isWebClient && auth.getRefreshToken() != null) {
            setRefreshCookie(response, auth.getRefreshToken().getTokenValue());
        }

        OAuth2AccessTokenResponse tokenResponse = buildResponse(auth, isWebClient);
        converter.write(tokenResponse, null, new ServletServerHttpResponse(response));
    }

    private static OAuth2AccessTokenResponse buildResponse(
            OAuth2AccessTokenAuthenticationToken auth, boolean omitRefreshToken) {
        OAuth2AccessToken accessToken = auth.getAccessToken();
        Map<String, Object> additionalParams = new LinkedHashMap<>(auth.getAdditionalParameters());

        OAuth2AccessTokenResponse.Builder builder = OAuth2AccessTokenResponse.withToken(accessToken.getTokenValue())
                .tokenType(accessToken.getTokenType())
                .scopes(accessToken.getScopes());

        if (accessToken.getIssuedAt() != null && accessToken.getExpiresAt() != null) {
            builder.expiresIn(Duration.between(accessToken.getIssuedAt(), accessToken.getExpiresAt())
                    .getSeconds());
        }

        if (!omitRefreshToken && auth.getRefreshToken() != null) {
            builder.refreshToken(auth.getRefreshToken().getTokenValue());
        }

        builder.additionalParameters(additionalParams);
        return builder.build();
    }

    private static void setRefreshCookie(HttpServletResponse response, String tokenValue) {
        // __Host- prefix: Secure + no Domain + Path=/ required by browsers
        String cookie = COOKIE_NAME + "=" + tokenValue + "; HttpOnly; Secure; SameSite=Strict; Path=" + SECURE_PATH;
        response.addHeader(HttpHeaders.SET_COOKIE, cookie);
    }

    static String extractRefreshCookie(HttpServletRequest request) {
        Cookie raw = WebUtils.getCookie(request, COOKIE_NAME);
        return raw != null ? raw.getValue() : null;
    }
}
