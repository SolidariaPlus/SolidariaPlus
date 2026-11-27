package com.solidariaplus.identity.identityaccess.domain.service;

import java.time.Duration;

public final class LockoutPolicy {
    public static final Duration WINDOW = Duration.ofMinutes(15);
    private static final int MAX_FAILED_ATTEMPTS = 5;

    private LockoutPolicy() {}

    public static boolean shouldLock(long failedAttempts) {
        return failedAttempts >= MAX_FAILED_ATTEMPTS;
    }
}
