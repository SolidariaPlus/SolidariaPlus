package com.solidariaplus.identity.identityaccess.infrastructure.adapter.output.crypto;

import com.solidariaplus.identity.identityaccess.application.port.output.PasswordHasher;
import com.solidariaplus.identity.identityaccess.domain.model.vo.Argon2Hash;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
class Argon2PasswordHasher implements PasswordHasher {
    private final Argon2PasswordEncoder encoder = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();

    @Override
    public Argon2Hash hash(String raw) {
        return new Argon2Hash(encoder.encode(raw));
    }

    @Override
    public boolean matches(String raw, Argon2Hash hash) {
        return encoder.matches(raw, hash.hash());
    }
}
