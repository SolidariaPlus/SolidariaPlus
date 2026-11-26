package com.solidariaplus.identity.identityaccess.domain.event;

import com.solidariaplus.identity.identityaccess.domain.model.vo.SubjectId;
import java.time.Instant;
import java.util.UUID;

public record AuditRecorded(
        UUID recordId,
        Action action,
        SubjectId actorSub,
        String actorRole,
        String entityType,
        String entityId,
        Outcome outcome,
        Instant occurredAt)
        implements DomainEvent {

    public enum Action {
        SIGN_IN,
        SIGN_IN_FAILED,
        ACCOUNT_LOCKED,
        ACCOUNT_ACTIVATED,
        ACTIVATION_CODE_ISSUED,
        SESSION_REVOKED,
        STAFF_AUTHENTICATOR_RESET
    }

    public enum Outcome {
        SUCCESS,
        FAILURE,
        DENIED
    }

    public static AuditRecorded of(
            Action action,
            SubjectId actorSub,
            String actorRole,
            String entityType,
            String entityId,
            Outcome outcome,
            Instant now) {
        return new AuditRecorded(UUID.randomUUID(), action, actorSub, actorRole, entityType, entityId, outcome, now);
    }
}
