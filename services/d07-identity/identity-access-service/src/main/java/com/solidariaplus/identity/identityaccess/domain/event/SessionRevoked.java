package com.solidariaplus.identity.identityaccess.domain.event;

import com.solidariaplus.identity.identityaccess.domain.model.vo.SessionId;
import com.solidariaplus.identity.identityaccess.domain.model.vo.SubjectId;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

public record SessionRevoked(
        SubjectId subject, Optional<SessionId> sessionId, Scope scope, Reason reason, Instant occurredAt)
        implements DomainEvent {
    private static final Duration ACCESS_TOKEN_LIFETIME = Duration.ofMinutes(10);

    public enum Scope {
        SESSION,
        ALL_SESSIONS
    }

    public enum Reason {
        SIGN_OUT,
        REFRESH_TOKEN_REUSE,
        MEMBERSHIP_ENDED,
        DIRECTORY_ACCOUNT_DISABLED,
        ADMINISTRATIVE
    }

    public static SessionRevoked ofSession(SubjectId subject, SessionId sessionId, Reason reason, Instant now) {
        return new SessionRevoked(subject, Optional.of(sessionId), Scope.SESSION, reason, now);
    }

    public static SessionRevoked ofAllSessions(SubjectId subject, Reason reason, Instant now) {
        return new SessionRevoked(subject, Optional.empty(), Scope.ALL_SESSIONS, reason, now);
    }

    public Instant tokensExpireBy() {
        return occurredAt.plus(ACCESS_TOKEN_LIFETIME);
    }
}
