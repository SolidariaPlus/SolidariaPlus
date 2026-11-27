package com.solidariaplus.identity.identityaccess.domain.model.vo;

import com.solidariaplus.identity.identityaccess.domain.service.Uuids;
import java.util.Objects;
import java.util.UUID;

public record SubjectId(UUID value) {
    public SubjectId {
        Objects.requireNonNull(value, "SubjectId cannot be null");
    }

    public static SubjectId generate() {
        return new SubjectId(Uuids.v7());
    }
}
