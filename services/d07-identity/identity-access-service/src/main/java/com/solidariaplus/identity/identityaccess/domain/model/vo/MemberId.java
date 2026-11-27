package com.solidariaplus.identity.identityaccess.domain.model.vo;

import java.util.Objects;
import java.util.UUID;

public record MemberId(UUID value) {
    public MemberId {
        Objects.requireNonNull(value, "MemberId cannot be null");
    }
}
