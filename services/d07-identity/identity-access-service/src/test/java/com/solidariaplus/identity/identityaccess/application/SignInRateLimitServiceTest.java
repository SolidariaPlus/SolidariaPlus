package com.solidariaplus.identity.identityaccess.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.solidariaplus.identity.identityaccess.application.service.SignInRateLimitService;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SignInRateLimitServiceTest {
    private static final Instant NOW = Instant.parse("2026-11-25T15:00:42Z");

    private final Map<String, Long> counts = new HashMap<>();
    private final SignInRateLimitService service = new SignInRateLimitService(
            (address, minute) -> counts.merge(address + ":" + minute, 1L, Long::sum), Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void twentyRequestsAMinutePassAndTheNextWaitsForTheNextMinute() {
        for (int request = 0; request < 20; request++) {
            assertThat(service.register("203.0.113.7")).isEmpty();
        }

        assertThat(service.register("203.0.113.7")).contains(Duration.ofSeconds(18));
        assertThat(service.register("198.51.100.20")).isEmpty();
        assertThat(counts).containsKey("203.0.113.7:" + NOW.getEpochSecond() / 60);
    }
}
