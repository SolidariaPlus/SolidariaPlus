package com.solidariaplus.identity.identityaccess.infrastructure.adapter.output.cache;

import com.solidariaplus.identity.identityaccess.application.port.output.RequestRateCounter;
import java.time.Duration;
import org.springframework.stereotype.Component;

/** Key ia:rl:{ip}:{epochMinute}; the TTL outlives its minute so a late request still finds the counter. */
@Component
class ValkeyRequestRateCounter implements RequestRateCounter {
    private static final Duration KEY_TTL = Duration.ofSeconds(120);

    private final ValkeyKeys keys;
    private final ExpiringCounter counter;

    ValkeyRequestRateCounter(ValkeyKeys keys, ExpiringCounter counter) {
        this.keys = keys;
        this.counter = counter;
    }

    @Override
    public long count(String address, long epochMinute) {
        return counter.increment(keys.of("rl", address + ":" + epochMinute), KEY_TTL);
    }
}
