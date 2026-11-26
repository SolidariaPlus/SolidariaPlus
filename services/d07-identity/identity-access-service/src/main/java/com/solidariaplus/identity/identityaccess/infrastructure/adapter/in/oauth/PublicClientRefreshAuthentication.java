package com.solidariaplus.identity.identityaccess.infrastructure.adapter.in.oauth;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.util.StringUtils;

final class PublicClientRefreshAuthentication implements AuthenticationConverter, AuthenticationProvider {
    private static final String REFRESH_TOKEN = AuthorizationGrantType.REFRESH_TOKEN.getValue();

    private final RegisteredClientRepository clients;

    PublicClientRefreshAuthentication(RegisteredClientRepository clients) {
        this.clients = clients;
    }

    @Override
    public Authentication convert(HttpServletRequest request) {
        String clientId = request.getParameter(OAuth2ParameterNames.CLIENT_ID);
        boolean publicRefresh = REFRESH_TOKEN.equals(request.getParameter(OAuth2ParameterNames.GRANT_TYPE))
                && StringUtils.hasText(clientId)
                && request.getHeader(HttpHeaders.AUTHORIZATION) == null
                && request.getParameter(OAuth2ParameterNames.CLIENT_SECRET) == null;
        return publicRefresh
                ? new OAuth2ClientAuthenticationToken(
                        clientId,
                        ClientAuthenticationMethod.NONE,
                        null,
                        Map.of(OAuth2ParameterNames.GRANT_TYPE, REFRESH_TOKEN))
                : null;
    }

    @Override
    public Authentication authenticate(Authentication authentication) {
        OAuth2ClientAuthenticationToken client = (OAuth2ClientAuthenticationToken) authentication;
        if (!ClientAuthenticationMethod.NONE.equals(client.getClientAuthenticationMethod())
                || !REFRESH_TOKEN.equals(client.getAdditionalParameters().get(OAuth2ParameterNames.GRANT_TYPE))) {
            return null;
        }
        RegisteredClient registered =
                clients.findByClientId(client.getPrincipal().toString());
        if (registered == null
                || !registered.getClientAuthenticationMethods().contains(ClientAuthenticationMethod.NONE)
                || !registered.getAuthorizationGrantTypes().contains(AuthorizationGrantType.REFRESH_TOKEN)) {
            throw new OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_CLIENT);
        }
        return new OAuth2ClientAuthenticationToken(registered, ClientAuthenticationMethod.NONE, null);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return OAuth2ClientAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
