package com.solidariaplus.identity.identityaccess.domain.model.entity;

import com.solidariaplus.identity.identityaccess.domain.model.vo.TokenHash;
import java.util.Objects;
import java.util.UUID;

public final class RefreshTokenFamily {
    private final UUID familyId;
    private TokenHash currentTokenHash;
    private boolean compromised;

    public static RefreshTokenFamily create(TokenHash initialTokenHash) {
        return new RefreshTokenFamily(UUID.randomUUID(), initialTokenHash, false);
    }

    public static RefreshTokenFamily reconstitute(UUID familyId, TokenHash currentTokenHash, boolean compromised) {
        return new RefreshTokenFamily(familyId, currentTokenHash, compromised);
    }

    private RefreshTokenFamily(UUID familyId, TokenHash currentTokenHash, boolean compromised) {
        this.familyId = Objects.requireNonNull(familyId, "familyId cannot be null");
        this.currentTokenHash = Objects.requireNonNull(currentTokenHash, "currentTokenHash cannot be null");
        this.compromised = compromised;
    }

    public boolean rotate(TokenHash presentedHash, TokenHash nextTokenHash) {
        Objects.requireNonNull(presentedHash, "presentedHash cannot be null");
        Objects.requireNonNull(nextTokenHash, "nextTokenHash cannot be null");

        if (compromised || !currentTokenHash.equals(presentedHash)) {
            this.compromised = true;
            return false;
        }
        this.currentTokenHash = nextTokenHash;
        return true;
    }

    public UUID getFamilyId() {
        return familyId;
    }

    public TokenHash getCurrentTokenHash() {
        return currentTokenHash;
    }

    public boolean isCompromised() {
        return compromised;
    }
}
