package com.solidariaplus.identity.identityaccess.domain.model.entity;

import com.solidariaplus.identity.identityaccess.domain.exception.DomainException;
import com.solidariaplus.identity.identityaccess.domain.exception.ErrorCode;
import com.solidariaplus.identity.identityaccess.domain.model.vo.Argon2Hash;
import com.solidariaplus.identity.identityaccess.domain.service.Uuids;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class ActivationCode {
    private static final Duration VALIDITY_WINDOW = Duration.ofHours(72);

    private final UUID codeId;
    private final Argon2Hash codeHash;
    private final Instant createdAt;
    private final Instant expiresAt;
    private boolean used;

    public static ActivationCode issue(Argon2Hash codeHash, Instant createdAt) {
        Objects.requireNonNull(createdAt, "createdAt cannot be null");
        return new ActivationCode(Uuids.v7(), codeHash, createdAt, createdAt.plus(VALIDITY_WINDOW), false);
    }

    public static ActivationCode reconstitute(
            UUID codeId, Argon2Hash codeHash, Instant createdAt, Instant expiresAt, boolean used) {
        return new ActivationCode(codeId, codeHash, createdAt, expiresAt, used);
    }

    private ActivationCode(UUID codeId, Argon2Hash codeHash, Instant createdAt, Instant expiresAt, boolean used) {
        this.codeId = Objects.requireNonNull(codeId, "codeId cannot be null");
        this.codeHash = Objects.requireNonNull(codeHash, "codeHash cannot be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt cannot be null");
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt cannot be null");
        this.used = used;
    }

    public boolean isValidAt(Instant now) {
        return !used && now.isBefore(expiresAt);
    }

    public void consume(Instant now) {
        if (!isValidAt(now)) {
            throw new DomainException(ErrorCode.AUTH_ACTIVATION_CODE_INVALID);
        }
        this.used = true;
    }

    public UUID getCodeId() {
        return codeId;
    }

    public Argon2Hash getCodeHash() {
        return codeHash;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public boolean isUsed() {
        return used;
    }
}
