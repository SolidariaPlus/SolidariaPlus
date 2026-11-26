package com.solidariaplus.identity.identityaccess.domain.model.entity;

import com.solidariaplus.identity.identityaccess.domain.model.vo.TokenHash;
import com.solidariaplus.identity.identityaccess.domain.service.Uuids;
import java.util.Objects;
import java.util.UUID;

public final class RefreshTokenFamily {
    private final UUID familyId;
    private TokenHash currentTokenHash;

    public static RefreshTokenFamily create(TokenHash initialTokenHash) {
        return new RefreshTokenFamily(Uuids.v7(), initialTokenHash);
    }

    public static RefreshTokenFamily reconstitute(UUID familyId, TokenHash currentTokenHash) {
        return new RefreshTokenFamily(familyId, currentTokenHash);
    }

    private RefreshTokenFamily(UUID familyId, TokenHash currentTokenHash) {
        this.familyId = Objects.requireNonNull(familyId, "familyId cannot be null");
        this.currentTokenHash = Objects.requireNonNull(currentTokenHash, "currentTokenHash cannot be null");
    }

    public boolean isCurrent(TokenHash presentedHash) {
        return currentTokenHash.equals(presentedHash);
    }

    public void rotate(TokenHash nextTokenHash) {
        this.currentTokenHash = Objects.requireNonNull(nextTokenHash, "nextTokenHash cannot be null");
    }

    public UUID getFamilyId() {
        return familyId;
    }

    public TokenHash getCurrentTokenHash() {
        return currentTokenHash;
    }
}
