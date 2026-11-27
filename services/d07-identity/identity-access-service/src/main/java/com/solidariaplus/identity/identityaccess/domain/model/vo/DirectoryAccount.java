package com.solidariaplus.identity.identityaccess.domain.model.vo;

public record DirectoryAccount(String username) {
    public DirectoryAccount {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("DirectoryAccount cannot be blank");
        }
    }

    @Override
    public String toString() {
        return "DirectoryAccount[REDACTED]";
    }
}
