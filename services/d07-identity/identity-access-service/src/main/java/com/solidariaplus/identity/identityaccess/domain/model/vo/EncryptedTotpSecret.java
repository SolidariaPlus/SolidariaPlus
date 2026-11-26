package com.solidariaplus.identity.identityaccess.domain.model.vo;

public record EncryptedTotpSecret(byte[] ciphertext) {
    public EncryptedTotpSecret {
        if (ciphertext == null || ciphertext.length == 0) {
            throw new IllegalArgumentException("EncryptedTotpSecret cannot be empty");
        }
        ciphertext = ciphertext.clone();
    }

    @Override
    public byte[] ciphertext() {
        return ciphertext.clone();
    }

    @Override
    public String toString() {
        return "EncryptedTotpSecret[KMS_PROTECTED]";
    }
}
