package com.solidariaplus.identity.identityaccess.infrastructure.adapter.in.oauth;

import java.net.URI;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.core.endpoint.PkceParameterNames;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationContext;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationException;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.util.StringUtils;

final class LoopbackAwareRequestValidator implements Consumer<OAuth2AuthorizationCodeRequestAuthenticationContext> {

    private static final String RFC_6749_ERROR_URI = "https://datatracker.ietf.org/doc/html/rfc6749#section-4.1.2.1";
    private static final String S256 = "S256";
    private static final Set<String> LOOPBACK_HOSTS = Set.of("127.0.0.1", "::1");

    @Override
    public void accept(OAuth2AuthorizationCodeRequestAuthenticationContext context) {
        OAuth2AuthorizationCodeRequestAuthenticationToken token = context.getAuthentication();
        RegisteredClient client = context.getRegisteredClient();

        validateRedirectUri(token, client);
        validateScopes(token, client);
        validatePkce(token, client);
    }

    private void validateRedirectUri(OAuth2AuthorizationCodeRequestAuthenticationToken token, RegisteredClient client) {
        String requestedUri = token.getRedirectUri();
        if (!StringUtils.hasText(requestedUri)) {
            if (client.getRedirectUris().size() != 1) {
                throwError(OAuth2ErrorCodes.INVALID_REQUEST, OAuth2ParameterNames.REDIRECT_URI, token, client);
            }
            return;
        }
        boolean matched = client.getRedirectUris().stream().anyMatch(r -> urisMatch(r, requestedUri));
        if (!matched) {
            throwError(OAuth2ErrorCodes.INVALID_REQUEST, OAuth2ParameterNames.REDIRECT_URI, token, client);
        }
    }

    private boolean urisMatch(String registered, String requested) {
        return isLoopback(requested) ? loopbackUrisMatch(registered, requested) : registered.equals(requested);
    }

    private boolean isLoopback(String uri) {
        try {
            return LOOPBACK_HOSTS.contains(URI.create(uri).getHost());
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /** Compares scheme, host and path; ignores the port per RFC 8252 §7.3. */
    private boolean loopbackUrisMatch(String registered, String requested) {
        try {
            URI reg = URI.create(registered);
            URI req = URI.create(requested);
            return Objects.equals(reg.getScheme(), req.getScheme())
                    && Objects.equals(reg.getHost(), req.getHost())
                    && Objects.equals(normalizePath(reg.getPath()), normalizePath(req.getPath()));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private String normalizePath(String path) {
        return path == null ? "" : path;
    }

    private void validateScopes(OAuth2AuthorizationCodeRequestAuthenticationToken token, RegisteredClient client) {
        Set<String> requested = token.getScopes();
        if (requested != null && !client.getScopes().containsAll(requested)) {
            throwError(OAuth2ErrorCodes.INVALID_SCOPE, OAuth2ParameterNames.SCOPE, token, client);
        }
    }

    private void validatePkce(OAuth2AuthorizationCodeRequestAuthenticationToken token, RegisteredClient client) {
        if (!client.getClientSettings().isRequireProofKey()) {
            return;
        }
        Map<String, Object> params = token.getAdditionalParameters();
        String codeChallenge = (String) params.get(PkceParameterNames.CODE_CHALLENGE);
        if (!StringUtils.hasText(codeChallenge)) {
            throwError(OAuth2ErrorCodes.INVALID_REQUEST, PkceParameterNames.CODE_CHALLENGE, token, client);
        }
        String method = (String) params.get(PkceParameterNames.CODE_CHALLENGE_METHOD);
        if (StringUtils.hasText(method) && !S256.equals(method)) {
            throwError(OAuth2ErrorCodes.INVALID_REQUEST, PkceParameterNames.CODE_CHALLENGE_METHOD, token, client);
        }
    }

    private void throwError(
            String errorCode,
            String parameterName,
            OAuth2AuthorizationCodeRequestAuthenticationToken token,
            RegisteredClient client) {
        OAuth2Error error = new OAuth2Error(errorCode, "OAuth 2.0 Parameter: " + parameterName, RFC_6749_ERROR_URI);
        throw new OAuth2AuthorizationCodeRequestAuthenticationException(
                error,
                new OAuth2AuthorizationCodeRequestAuthenticationToken(
                        token.getAuthorizationUri(),
                        client.getClientId(),
                        (Authentication) token.getPrincipal(),
                        token.getRedirectUri(),
                        token.getState(),
                        token.getScopes(),
                        token.getAdditionalParameters()));
    }
}
