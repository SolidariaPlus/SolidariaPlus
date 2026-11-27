package com.solidariaplus.identity.identityaccess.application.port.output;

import com.solidariaplus.identity.identityaccess.domain.model.vo.EncryptedTotpSecret;
import com.solidariaplus.identity.identityaccess.domain.model.vo.SubjectId;

public interface SecretCipher {
    EncryptedTotpSecret encrypt(String secret, SubjectId owner);

    String decrypt(EncryptedTotpSecret ciphertext, SubjectId owner);
}
