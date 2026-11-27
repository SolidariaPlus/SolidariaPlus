package com.solidariaplus.identity.identityaccess.domain.service;

/** Sign-in requests allowed per client address and minute, in addition to API Gateway throttling. */
public final class SignInRateLimitPolicy {
    private static final int MAX_REQUESTS_PER_MINUTE = 20;

    private SignInRateLimitPolicy() {}

    public static boolean isExceeded(long requestsThisMinute) {
        return requestsThisMinute > MAX_REQUESTS_PER_MINUTE;
    }
}
