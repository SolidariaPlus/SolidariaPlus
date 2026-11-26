package com.solidariaplus.identity.identityaccess.domain.model.vo;

import java.util.Objects;
import java.util.UUID;

public record SessionId(UUID value) {
    public SessionId {
        Objects.requireNonNull(value, "SessionId cannot be null");
    }
}
