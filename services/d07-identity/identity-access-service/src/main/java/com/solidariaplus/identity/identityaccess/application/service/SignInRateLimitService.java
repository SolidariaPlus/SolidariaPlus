package com.solidariaplus.identity.identityaccess.application.service;

import com.solidariaplus.identity.identityaccess.application.port.in.SignInRateLimitUseCase;
import com.solidariaplus.identity.identityaccess.application.port.output.RequestRateCounter;
import com.solidariaplus.identity.identityaccess.domain.service.SignInRateLimitPolicy;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class SignInRateLimitService implements SignInRateLimitUseCase {
    private static final long SECONDS_PER_MINUTE = 60;

    private final RequestRateCounter counter;
    private final Clock clock;

    public SignInRateLimitService(RequestRateCounter counter, Clock clock) {
        this.counter = counter;
        this.clock = clock;
    }

    @Override
    public Optional<Duration> register(String address) {
        Instant now = clock.instant();
        long epochMinute = now.getEpochSecond() / SECONDS_PER_MINUTE;
        if (!SignInRateLimitPolicy.isExceeded(counter.count(address, epochMinute))) {
            return Optional.empty();
        }
        Instant nextMinute = now.truncatedTo(ChronoUnit.MINUTES).plus(Duration.ofMinutes(1));
        return Optional.of(Duration.between(now, nextMinute));
    }
}
