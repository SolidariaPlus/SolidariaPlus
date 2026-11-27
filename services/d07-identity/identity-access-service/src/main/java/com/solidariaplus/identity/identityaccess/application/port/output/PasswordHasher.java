package com.solidariaplus.identity.identityaccess.application.port.output;

import com.solidariaplus.identity.identityaccess.domain.model.vo.Argon2Hash;

public interface PasswordHasher {
    Argon2Hash hash(String raw);

    boolean matches(String raw, Argon2Hash hash);
}
