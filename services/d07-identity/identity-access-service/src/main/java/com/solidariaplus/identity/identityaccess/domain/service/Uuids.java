package com.solidariaplus.identity.identityaccess.domain.service;

import java.security.SecureRandom;
import java.util.UUID;

public final class Uuids {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final long VERSION_7 = 0x7000L;
    private static final long VARIANT_IETF = 0x8000000000000000L;
    private static final long VARIANT_MASK = 0x3FFFFFFFFFFFFFFFL;

    private Uuids() {}

    public static UUID v7() {
        long mostSignificant = (System.currentTimeMillis() << 16) | VERSION_7 | (RANDOM.nextInt() & 0x0FFF);
        long leastSignificant = (RANDOM.nextLong() & VARIANT_MASK) | VARIANT_IETF;
        return new UUID(mostSignificant, leastSignificant);
    }
}
