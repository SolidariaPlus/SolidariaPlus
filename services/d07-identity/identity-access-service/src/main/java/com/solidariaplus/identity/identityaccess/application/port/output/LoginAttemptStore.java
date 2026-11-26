package com.solidariaplus.identity.identityaccess.application.port.output;

import java.time.Duration;

public interface LoginAttemptStore {
    boolean isLocked(String identifier);

    long recordFailure(String identifier, Duration window);

    void lock(String identifier, Duration duration);

    void clear(String identifier);
}
