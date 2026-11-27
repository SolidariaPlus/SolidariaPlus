package com.solidariaplus.identity.identityaccess.domain.model.vo;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public record TokenHash(String sha256Hex) {
    public TokenHash {
        if (sha256Hex == null || sha256Hex.isBlank()) {
            throw new IllegalArgumentException("TokenHash cannot be blank");
        }
    }

    public static TokenHash of(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return new TokenHash(HexFormat.of().formatHex(digest));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public String toString() {
        return "TokenHash[REDACTED]";
    }
}
