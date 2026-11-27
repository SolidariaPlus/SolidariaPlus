package com.solidariaplus.identity.identityaccess.application.port.output;

import com.solidariaplus.identity.identityaccess.domain.model.vo.SessionId;
import java.time.Duration;
import java.time.Instant;

public interface RevokedSessionStore {
    /** Remembers the revocation while access tokens of the session may still be valid. */
    void markRevoked(SessionId sessionId, Instant revokedAt, Duration accessTokenLifetime);

    boolean isRevoked(SessionId sessionId);
}
