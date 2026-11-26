package com.solidariaplus.identity.identityaccess.domain.model.aggregate;

import com.solidariaplus.identity.identityaccess.domain.model.entity.RefreshTokenFamily;
import com.solidariaplus.identity.identityaccess.domain.model.vo.SessionId;
import com.solidariaplus.identity.identityaccess.domain.model.vo.SubjectId;
import com.solidariaplus.identity.identityaccess.domain.model.vo.TokenHash;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public final class Session {
    private final SessionId sessionId;
    private final SubjectId sub;
    private final RefreshTokenFamily tokenFamily;
    private final Instant createdAt;
    private Instant revokedAt;

    public static Session start(SubjectId sub, TokenHash initialRefreshTokenHash, Instant now) {
        return new Session(SessionId.generate(), sub, RefreshTokenFamily.create(initialRefreshTokenHash), now, null);
    }

    public static Session reconstitute(
            SessionId sessionId, SubjectId sub, RefreshTokenFamily tokenFamily, Instant createdAt, Instant revokedAt) {
        return new Session(sessionId, sub, tokenFamily, createdAt, revokedAt);
    }

    private Session(
            SessionId sessionId, SubjectId sub, RefreshTokenFamily tokenFamily, Instant createdAt, Instant revokedAt) {
        this.sessionId = Objects.requireNonNull(sessionId, "sessionId cannot be null");
        this.sub = Objects.requireNonNull(sub, "sub cannot be null");
        this.tokenFamily = Objects.requireNonNull(tokenFamily, "tokenFamily cannot be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt cannot be null");
        this.revokedAt = revokedAt;
    }

    public boolean rotateRefreshToken(TokenHash presentedHash, TokenHash nextHash, Instant now) {
        if (isRevoked()) {
            return false;
        }
        boolean rotated = tokenFamily.rotate(presentedHash, nextHash);
        if (!rotated) {
            revoke(now);
            return false;
        }
        return true;
    }

    public void revoke(Instant now) {
        if (this.revokedAt == null) {
            this.revokedAt = Objects.requireNonNull(now, "now cannot be null");
        }
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public SessionId getSessionId() {
        return sessionId;
    }

    public SubjectId getSub() {
        return sub;
    }

    public RefreshTokenFamily getTokenFamily() {
        return tokenFamily;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Optional<Instant> getRevokedAt() {
        return Optional.ofNullable(revokedAt);
    }
}
