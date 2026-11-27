package com.solidariaplus.identity.identityaccess.infrastructure.adapter.output.crypto;

import com.amazonaws.encryptionsdk.AwsCrypto;
import com.amazonaws.encryptionsdk.CommitmentPolicy;
import com.amazonaws.encryptionsdk.CryptoAlgorithm;
import com.solidariaplus.identity.identityaccess.application.port.output.SecretCipher;
import com.solidariaplus.identity.identityaccess.domain.model.vo.EncryptedTotpSecret;
import com.solidariaplus.identity.identityaccess.domain.model.vo.SubjectId;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.stereotype.Component;
import software.amazon.cryptography.materialproviders.IKeyring;

@Component
class EncryptionSdkSecretCipher implements SecretCipher {
    private final AwsCrypto crypto = AwsCrypto.builder()
            .withCommitmentPolicy(CommitmentPolicy.RequireEncryptRequireDecrypt)
            .withEncryptionAlgorithm(CryptoAlgorithm.ALG_AES_256_GCM_HKDF_SHA512_COMMIT_KEY)
            .build();
    private final IKeyring keyring;

    EncryptionSdkSecretCipher(IKeyring keyring) {
        this.keyring = keyring;
    }

    @Override
    public EncryptedTotpSecret encrypt(String secret, SubjectId owner) {
        byte[] plaintext = secret.getBytes(StandardCharsets.UTF_8);
        return new EncryptedTotpSecret(
                crypto.encryptData(keyring, plaintext, context(owner)).getResult());
    }

    @Override
    public String decrypt(EncryptedTotpSecret ciphertext, SubjectId owner) {
        byte[] plaintext = crypto.decryptData(keyring, ciphertext.ciphertext(), context(owner))
                .getResult();
        return new String(plaintext, StandardCharsets.UTF_8);
    }

    private static Map<String, String> context(SubjectId owner) {
        return Map.of(
                "table",
                "staff_profiles",
                "column",
                "totp_secret_enc",
                "id",
                owner.value().toString());
    }
}
