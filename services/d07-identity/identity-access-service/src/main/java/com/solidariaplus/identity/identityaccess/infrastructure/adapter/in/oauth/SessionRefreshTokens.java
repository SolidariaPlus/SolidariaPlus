package com.solidariaplus.identity.identityaccess.infrastructure.adapter.in.oauth;

import com.solidariaplus.identity.identityaccess.domain.model.vo.SessionId;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.crypto.keygen.Base64StringKeyGenerator;
import org.springframework.security.crypto.keygen.StringKeyGenerator;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;

final class SessionRefreshTokens implements OAuth2TokenGenerator<OAuth2RefreshToken> {
    private static final char SEPARATOR = '.';
    private static final int RANDOM_BYTES = 64;

    private final StringKeyGenerator random =
            new Base64StringKeyGenerator(Base64.getUrlEncoder().withoutPadding(), RANDOM_BYTES);

    @Override
    public OAuth2RefreshToken generate(OAuth2TokenContext context) {
        if (!OAuth2TokenType.REFRESH_TOKEN.equals(context.getTokenType()) || context.getAuthorization() == null) {
            return null;
        }
        Instant issuedAt = Instant.now();
        Instant expiresAt =
                issuedAt.plus(context.getRegisteredClient().getTokenSettings().getRefreshTokenTimeToLive());
        String value = context.getAuthorization().getId() + SEPARATOR + random.generateKey();
        return new OAuth2RefreshToken(value, issuedAt, expiresAt);
    }

    static Optional<SessionId> sessionOf(String refreshToken) {
        int separator = refreshToken.indexOf(SEPARATOR);
        if (separator < 0) {
            return Optional.empty();
        }
        try {
            return Optional.of(new SessionId(UUID.fromString(refreshToken.substring(0, separator))));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
