package com.solidariaplus.identity.identityaccess;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Base of the integration tests: one PostgreSQL and one Valkey container for every test class, so the
 * Spring context is built once and shared.
 */
@SpringBootTest
@ActiveProfiles("test")
public abstract class ServiceIntegrationTest {

    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17.6-alpine");

    @ServiceConnection(name = "redis")
    static final GenericContainer<?> VALKEY =
            new GenericContainer<>("valkey/valkey:8.1.3-alpine").withExposedPorts(6379);

    static {
        POSTGRES.start();
        VALKEY.start();
    }

    @DynamicPropertySource
    static void generatedKeys(DynamicPropertyRegistry registry) throws GeneralSecurityException {
        KeyPair signing = ecKeyPair();
        KeyPair previous = ecKeyPair();
        byte[] fieldKey = new byte[32];
        new SecureRandom().nextBytes(fieldKey);
        registry.add("signing.kid", () -> "test-current");
        registry.add("signing.privateKey", () -> encode(signing.getPrivate().getEncoded()));
        registry.add("signing.publicKey", () -> encode(signing.getPublic().getEncoded()));
        registry.add("signing.previous-kid", () -> "test-previous");
        registry.add(
                "signing.previous-publicKey", () -> encode(previous.getPublic().getEncoded()));
        registry.add("solidaria.encryption.local-key", () -> encode(fieldKey));
    }

    private static KeyPair ecKeyPair() throws GeneralSecurityException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec("secp256r1"));
        return generator.generateKeyPair();
    }

    private static String encode(byte[] value) {
        return Base64.getEncoder().encodeToString(value);
    }
}
