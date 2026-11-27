package com.solidariaplus.identity.identityaccess.infrastructure.adapter.output.cache;

import static org.assertj.core.api.Assertions.assertThat;

import com.solidariaplus.identity.identityaccess.ServiceIntegrationTest;
import com.solidariaplus.identity.identityaccess.application.port.in.RevokeSessionsUseCase;
import com.solidariaplus.identity.identityaccess.application.port.in.SessionStatusUseCase;
import com.solidariaplus.identity.identityaccess.application.port.output.LoginAttemptStore;
import com.solidariaplus.identity.identityaccess.application.port.output.RequestRateCounter;
import com.solidariaplus.identity.identityaccess.domain.event.SessionRevoked.Reason;
import com.solidariaplus.identity.identityaccess.domain.model.aggregate.Session;
import com.solidariaplus.identity.identityaccess.domain.model.aggregate.Subject;
import com.solidariaplus.identity.identityaccess.domain.model.vo.SessionId;
import com.solidariaplus.identity.identityaccess.domain.model.vo.TokenHash;
import com.solidariaplus.identity.identityaccess.domain.repository.SessionRepository;
import com.solidariaplus.identity.identityaccess.domain.repository.SubjectRepository;
import com.solidariaplus.identity.identityaccess.domain.service.LockoutPolicy;
import com.solidariaplus.identity.identityaccess.domain.service.Uuids;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

class ValkeyIntegrationTest extends ServiceIntegrationTest {

    @Autowired
    private LoginAttemptStore attempts;

    @Autowired
    private RequestRateCounter requests;

    @Autowired
    private RevokeSessionsUseCase revocations;

    @Autowired
    private SessionStatusUseCase sessionStatus;

    @Autowired
    private SessionRepository sessions;

    @Autowired
    private SubjectRepository subjects;

    @Autowired
    private StringRedisTemplate valkey;

    @Test
    void failuresExpireFifteenMinutesAfterTheFirstOne() {
        String identifier = "failures-" + UUID.randomUUID();
        String key = "ia:fail:" + TokenHash.of(identifier).sha256Hex();

        assertThat(attempts.recordFailure(identifier, LockoutPolicy.WINDOW)).isEqualTo(1);
        assertThat(attempts.recordFailure(identifier, Duration.ofMinutes(1))).isEqualTo(2);

        assertThat(ttl(key)).isBetween(Duration.ofMinutes(14), LockoutPolicy.WINDOW);
        assertThat(valkey.keys("ia:fail:*")).noneMatch(stored -> stored.contains(identifier));

        attempts.clear(identifier);
        assertThat(valkey.hasKey(key)).isFalse();
    }

    @Test
    void lockLastsFifteenMinutes() {
        String identifier = "lock-" + UUID.randomUUID();

        attempts.lock(identifier, LockoutPolicy.WINDOW);

        assertThat(attempts.isLocked(identifier)).isTrue();
        assertThat(attempts.isLocked("other-" + identifier)).isFalse();
        assertThat(ttl("ia:lock:" + TokenHash.of(identifier).sha256Hex()))
                .isBetween(Duration.ofMinutes(14), LockoutPolicy.WINDOW);
    }

    @Test
    void requestsAreCountedPerAddressAndMinuteForTwoMinutes() {
        long minute = Instant.now().getEpochSecond() / 60;

        assertThat(requests.count("203.0.113.7", minute)).isEqualTo(1);
        assertThat(requests.count("203.0.113.7", minute)).isEqualTo(2);
        assertThat(requests.count("203.0.113.7", minute + 1)).isEqualTo(1);
        assertThat(requests.count("198.51.100.20", minute)).isEqualTo(1);

        assertThat(ttl("ia:rl:203.0.113.7:" + minute)).isBetween(Duration.ofSeconds(110), Duration.ofSeconds(120));
    }

    @Test
    void revokedSessionIsCachedForTheAccessTokenLifetime() {
        Subject subject = Subject.forStaff();
        subjects.save(subject);
        SessionId sessionId = new SessionId(Uuids.v7());
        sessions.save(Session.start(sessionId, subject.getSub(), TokenHash.of("token"), Instant.now()));

        assertThat(sessionStatus.isRevoked(sessionId)).isFalse();
        revocations.revokeSession(sessionId, Reason.SIGN_OUT);

        assertThat(sessionStatus.isRevoked(sessionId)).isTrue();
        assertThat(ttl("ia:revoked:" + sessionId.value())).isBetween(Duration.ofMinutes(9), Duration.ofMinutes(10));
    }

    private Duration ttl(String key) {
        Long millis = valkey.getExpire(key, TimeUnit.MILLISECONDS);
        return Duration.ofMillis(millis == null ? -1 : millis);
    }
}
