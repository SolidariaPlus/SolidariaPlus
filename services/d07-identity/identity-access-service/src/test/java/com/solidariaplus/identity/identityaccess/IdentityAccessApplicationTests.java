package com.solidariaplus.identity.identityaccess;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class IdentityAccessApplicationTests {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17.6-alpine");

    @DynamicPropertySource
    static void generatedKeys(DynamicPropertyRegistry registry) throws GeneralSecurityException {
        KeyPair signing = ecKeyPair();
        byte[] fieldKey = new byte[32];
        new SecureRandom().nextBytes(fieldKey);
        registry.add("signing.kid", () -> "test");
        registry.add("signing.privateKey", () -> encode(signing.getPrivate().getEncoded()));
        registry.add("signing.publicKey", () -> encode(signing.getPublic().getEncoded()));
        registry.add("solidaria.encryption.local-key", () -> encode(fieldKey));
    }

    @Test
    void contextLoads() {}

    private static KeyPair ecKeyPair() throws GeneralSecurityException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec("secp256r1"));
        return generator.generateKeyPair();
    }

    private static String encode(byte[] value) {
        return Base64.getEncoder().encodeToString(value);
    }
}
