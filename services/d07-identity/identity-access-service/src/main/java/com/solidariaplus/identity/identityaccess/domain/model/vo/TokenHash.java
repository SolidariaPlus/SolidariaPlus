package com.solidariaplus.identity.identityaccess.domain.model.vo;

public record TokenHash(String sha256Hex) {
    public TokenHash {
        if (sha256Hex == null || sha256Hex.isBlank()) {
            throw new IllegalArgumentException("TokenHash cannot be blank");
        }
    }

    @Override
    public String toString() {
        return "TokenHash[REDACTED]";
    }
}
