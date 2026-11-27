package com.solidariaplus.identity.identityaccess.infrastructure.adapter.in.oauth;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

/**
 * Micrometer metrics for MS-17 (KAN-49). Counters and timers are pre-registered so Prometheus
 * scrapes them even before any event occurs; absence of a metric cannot be confused with 0.
 */
@Component
public class IdentityMetrics {

    private final Counter signInSuccess;
    private final Counter signInFailure;
    private final Counter signInLocked;
    private final Counter tokenRevocations;
    private final Counter sessionRevocations;
    private final Timer tokenIssuanceTimer;

    public IdentityMetrics(MeterRegistry registry) {
        signInSuccess = Counter.builder("identity.signin")
                .tag("outcome", "success")
                .description("Successful sign-in attempts")
                .register(registry);

        signInFailure = Counter.builder("identity.signin")
                .tag("outcome", "failure")
                .description("Failed sign-in attempts (bad credentials)")
                .register(registry);

        signInLocked = Counter.builder("identity.signin")
                .tag("outcome", "locked")
                .description("Sign-in attempts blocked by account lockout")
                .register(registry);

        tokenRevocations = Counter.builder("identity.token.revocations")
                .description("OAuth2 tokens explicitly revoked")
                .register(registry);

        sessionRevocations = Counter.builder("identity.session.revocations")
                .description("Sessions revoked (sign-out or reuse detection)")
                .register(registry);

        tokenIssuanceTimer = Timer.builder("identity.token.issuance")
                .description("Time to issue an access token (authorization_code or client_credentials)")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry);
    }

    public void recordSignInSuccess() {
        signInSuccess.increment();
    }

    public void recordSignInFailure() {
        signInFailure.increment();
    }

    public void recordSignInLocked() {
        signInLocked.increment();
    }

    public void recordTokenRevocation() {
        tokenRevocations.increment();
    }

    public void recordSessionRevocation() {
        sessionRevocations.increment();
    }

    public Timer tokenIssuanceTimer() {
        return tokenIssuanceTimer;
    }
}
