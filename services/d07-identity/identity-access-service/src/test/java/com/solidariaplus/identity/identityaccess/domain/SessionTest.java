package com.solidariaplus.identity.identityaccess.domain;

import static com.solidariaplus.identity.identityaccess.domain.DomainFixtures.NOW;
import static org.assertj.core.api.Assertions.assertThat;

import com.solidariaplus.identity.identityaccess.domain.model.aggregate.Session;
import com.solidariaplus.identity.identityaccess.domain.model.entity.RefreshTokenFamily;
import com.solidariaplus.identity.identityaccess.domain.model.vo.SessionId;
import com.solidariaplus.identity.identityaccess.domain.model.vo.SubjectId;
import com.solidariaplus.identity.identityaccess.domain.model.vo.TokenHash;
import com.solidariaplus.identity.identityaccess.domain.service.SessionPolicy;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SessionTest {
    private final TokenHash first = TokenHash.of("first");
    private final TokenHash second = TokenHash.of("second");
    private final Session session = Session.start(new SessionId(UUID.randomUUID()), SubjectId.generate(), first, NOW);

    @Test
    void onlyTheCurrentRefreshTokenIsAccepted() {
        assertThat(session.isReuse(first)).isFalse();

        session.rotateRefreshToken(second);

        assertThat(session.isReuse(first)).isTrue();
        assertThat(session.isReuse(second)).isFalse();
        assertThat(session.getTokenFamily().getCurrentTokenHash()).isEqualTo(second);
    }

    @Test
    void staffSessionsEndAfterTenHours() {
        Duration staff = SessionPolicy.maxLifetime(false);

        assertThat(session.isActive(NOW.plus(Duration.ofHours(9)), staff)).isTrue();
        assertThat(session.isActive(NOW.plus(Duration.ofHours(11)), staff)).isFalse();
        assertThat(session.isActive(NOW.plus(Duration.ofDays(89)), SessionPolicy.maxLifetime(true)))
                .isTrue();
    }

    @Test
    void revocationIsKeptOnce() {
        session.revoke(NOW);
        session.revoke(NOW.plusSeconds(60));

        assertThat(session.isRevoked()).isTrue();
        assertThat(session.getRevokedAt()).contains(NOW);
        assertThat(session.isActive(NOW, Duration.ofDays(1))).isFalse();
    }

    @Test
    void reconstitutedSessionKeepsItsFamily() {
        RefreshTokenFamily family = RefreshTokenFamily.reconstitute(UUID.randomUUID(), first);
        Session stored = Session.reconstitute(session.getSessionId(), session.getSub(), family, NOW, null);

        assertThat(stored.getTokenFamily().getFamilyId()).isEqualTo(family.getFamilyId());
        assertThat(stored.getCreatedAt()).isEqualTo(NOW);
        assertThat(stored.isRevoked()).isFalse();
    }
}
