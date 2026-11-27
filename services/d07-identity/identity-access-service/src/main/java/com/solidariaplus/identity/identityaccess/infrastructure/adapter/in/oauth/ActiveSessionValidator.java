package com.solidariaplus.identity.identityaccess.infrastructure.adapter.in.oauth;

import com.solidariaplus.identity.identityaccess.application.port.in.SessionStatusUseCase;
import com.solidariaplus.identity.identityaccess.domain.model.vo.SessionId;
import java.util.UUID;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

/** Rejects access tokens of a revoked session before they expire; tokens without sid (services) pass. */
final class ActiveSessionValidator implements OAuth2TokenValidator<Jwt> {
    private static final OAuth2Error REVOKED =
            new OAuth2Error(OAuth2ErrorCodes.INVALID_TOKEN, "The session has been revoked.", null);

    private final SessionStatusUseCase sessions;

    ActiveSessionValidator(SessionStatusUseCase sessions) {
        this.sessions = sessions;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        String sid = token.getClaimAsString("sid");
        boolean revoked = sid != null && sessions.isRevoked(new SessionId(UUID.fromString(sid)));
        return revoked ? OAuth2TokenValidatorResult.failure(REVOKED) : OAuth2TokenValidatorResult.success();
    }
}
