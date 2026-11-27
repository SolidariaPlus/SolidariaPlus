package com.solidariaplus.identity.identityaccess.domain.service;

import java.time.Duration;

public final class SessionPolicy {
    /** Longest access-token lifetime of every client (application.yml); bounds how long a revocation is cached. */
    public static final Duration ACCESS_TOKEN_MAX_LIFETIME = Duration.ofMinutes(10);

    private static final Duration STAFF_MAX_LIFETIME = Duration.ofHours(10);
    private static final Duration MEMBER_MAX_LIFETIME = Duration.ofDays(90);

    private SessionPolicy() {}

    public static Duration maxLifetime(boolean member) {
        return member ? MEMBER_MAX_LIFETIME : STAFF_MAX_LIFETIME;
    }
}
