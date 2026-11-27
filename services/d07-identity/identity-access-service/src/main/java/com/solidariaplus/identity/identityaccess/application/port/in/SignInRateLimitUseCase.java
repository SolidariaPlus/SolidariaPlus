package com.solidariaplus.identity.identityaccess.application.port.in;

import java.time.Duration;
import java.util.Optional;

public interface SignInRateLimitUseCase {
    /** Counts a sign-in request from the address; returns how long it must wait when the limit is reached. */
    Optional<Duration> register(String address);
}
