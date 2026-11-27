package com.solidariaplus.identity.identityaccess.domain.event;

import java.time.Instant;

public sealed interface DomainEvent permits SessionRevoked, AuditRecorded {
    Instant occurredAt();
}
