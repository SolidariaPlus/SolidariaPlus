package com.solidariaplus.identity.identityaccess.domain;

import com.solidariaplus.identity.identityaccess.domain.model.vo.Argon2Hash;
import com.solidariaplus.identity.identityaccess.domain.model.vo.EncryptedTotpSecret;
import java.time.Instant;

public final class DomainFixtures {
    public static final Instant NOW = Instant.parse("2026-11-25T15:00:00Z");
    public static final Argon2Hash HASH = new Argon2Hash("$argon2id$v=19$m=16384,t=2,p=1$c2FsdA$aGFzaA");
    public static final EncryptedTotpSecret TOTP = new EncryptedTotpSecret(new byte[] {1, 2, 3});

    private DomainFixtures() {}
}
