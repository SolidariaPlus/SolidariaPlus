package com.solidariaplus.identity.identityaccess.domain.model.vo;

public record Argon2Hash(String hash) {
    public Argon2Hash {
        if (hash == null || !hash.startsWith("$argon2id$")) {
            throw new IllegalArgumentException("Invalid Argon2id hash format");
        }
    }

    @Override
    public String toString() {
        return "Argon2Hash[PROTECTED]";
    }
}
