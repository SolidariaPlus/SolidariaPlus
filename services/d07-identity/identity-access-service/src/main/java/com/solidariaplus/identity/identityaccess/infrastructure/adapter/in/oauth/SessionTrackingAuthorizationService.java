package com.solidariaplus.identity.identityaccess.infrastructure.adapter.in.oauth;

import com.solidariaplus.identity.identityaccess.application.port.in.RevokeSessionsUseCase;
import com.solidariaplus.identity.identityaccess.application.port.in.SessionTokenUseCase;
import com.solidariaplus.identity.identityaccess.domain.event.SessionRevoked.Reason;
import com.solidariaplus.identity.identityaccess.domain.model.vo.SessionId;
import com.solidariaplus.identity.identityaccess.domain.model.vo.SubjectId;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;

final class SessionTrackingAuthorizationService implements OAuth2AuthorizationService {
    private final OAuth2AuthorizationService delegate;
    private final SessionTokenUseCase sessionTokens;
    private final RevokeSessionsUseCase revocations;

    SessionTrackingAuthorizationService(
            OAuth2AuthorizationService delegate, SessionTokenUseCase sessionTokens, RevokeSessionsUseCase revocations) {
        this.delegate = delegate;
        this.sessionTokens = sessionTokens;
        this.revocations = revocations;
    }

    @Override
    public void save(OAuth2Authorization authorization) {
        OAuth2Authorization.Token<OAuth2RefreshToken> refreshToken = authorization.getRefreshToken();
        if (refreshToken != null) {
            track(authorization, refreshToken);
        }
        delegate.save(authorization);
    }

    @Override
    public void remove(OAuth2Authorization authorization) {
        delegate.remove(authorization);
    }

    @Override
    public OAuth2Authorization findById(String id) {
        return delegate.findById(id);
    }

    @Override
    public OAuth2Authorization findByToken(String token, OAuth2TokenType tokenType) {
        OAuth2Authorization authorization = delegate.findByToken(token, tokenType);
        if (tokenType != null && !OAuth2TokenType.REFRESH_TOKEN.equals(tokenType)) {
            return authorization;
        }
        Optional<SessionId> session = SessionRefreshTokens.sessionOf(token);
        boolean accepted = session.map(id -> sessionTokens.accept(id, token)).orElse(true);
        return accepted ? authorization : null;
    }

    private void track(OAuth2Authorization authorization, OAuth2Authorization.Token<OAuth2RefreshToken> token) {
        SessionId sessionId = new SessionId(UUID.fromString(authorization.getId()));
        if (token.isInvalidated()) {
            revocations.revokeSession(sessionId, Reason.SIGN_OUT);
            return;
        }
        String value = token.getToken().getTokenValue();
        OAuth2Authorization stored = delegate.findById(authorization.getId());
        boolean issued = stored == null
                || stored.getRefreshToken() == null
                || !stored.getRefreshToken().getToken().getTokenValue().equals(value);
        if (issued) {
            sessionTokens.recordIssued(
                    sessionId, new SubjectId(UUID.fromString(authorization.getPrincipalName())), value);
        }
    }
}
