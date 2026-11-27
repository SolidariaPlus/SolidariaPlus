package com.solidariaplus.identity.identityaccess.infrastructure.adapter.output.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.solidariaplus.identity.identityaccess.ServiceIntegrationTest;
import com.solidariaplus.identity.identityaccess.application.port.output.ProcessedEventStore;
import com.solidariaplus.identity.identityaccess.domain.model.aggregate.MemberAccount;
import com.solidariaplus.identity.identityaccess.domain.model.aggregate.Session;
import com.solidariaplus.identity.identityaccess.domain.model.aggregate.StaffProfile;
import com.solidariaplus.identity.identityaccess.domain.model.aggregate.Subject;
import com.solidariaplus.identity.identityaccess.domain.model.vo.Argon2Hash;
import com.solidariaplus.identity.identityaccess.domain.model.vo.DirectoryAccount;
import com.solidariaplus.identity.identityaccess.domain.model.vo.MemberAccountStatus;
import com.solidariaplus.identity.identityaccess.domain.model.vo.MemberId;
import com.solidariaplus.identity.identityaccess.domain.model.vo.MemberNumber;
import com.solidariaplus.identity.identityaccess.domain.model.vo.MfaResetWindow;
import com.solidariaplus.identity.identityaccess.domain.model.vo.SessionId;
import com.solidariaplus.identity.identityaccess.domain.model.vo.SubjectId;
import com.solidariaplus.identity.identityaccess.domain.model.vo.TokenHash;
import com.solidariaplus.identity.identityaccess.domain.repository.MemberAccountRepository;
import com.solidariaplus.identity.identityaccess.domain.repository.SessionRepository;
import com.solidariaplus.identity.identityaccess.domain.repository.StaffProfileRepository;
import com.solidariaplus.identity.identityaccess.domain.repository.SubjectRepository;
import com.solidariaplus.identity.identityaccess.domain.service.Uuids;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

class PersistenceIntegrationTest extends ServiceIntegrationTest {
    private static final Instant NOW = Instant.parse("2026-11-25T15:00:00Z");
    private static final Argon2Hash HASH = new Argon2Hash("$argon2id$v=19$m=16384,t=2,p=1$c2FsdA$aGFzaA");
    private static final AtomicLong MEMBER_NUMBERS = new AtomicLong(5_000_000_000L);

    @Autowired
    private SubjectRepository subjects;

    @Autowired
    private MemberAccountRepository accounts;

    @Autowired
    private StaffProfileRepository staffProfiles;

    @Autowired
    private SessionRepository sessions;

    @Autowired
    private ProcessedEventStore processedEvents;

    @Autowired
    private RegisteredClientRepository registeredClients;

    @Autowired
    private JdbcClient jdbc;

    @Test
    void memberAccountKeepsItsLatestActivationCode() {
        MemberAccount account = newMember();
        account.issueActivationCode(HASH, NOW);
        accounts.save(account);

        MemberAccount found =
                accounts.findByMemberNumber(account.getMemberNumber()).orElseThrow();

        assertThat(found.getMemberId()).isEqualTo(account.getMemberId());
        assertThat(found.getStatus()).isEqualTo(MemberAccountStatus.PENDING_ACTIVATION);
        assertThat(found.getActiveCode()).hasValueSatisfying(code -> {
            assertThat(code.getCodeHash()).isEqualTo(HASH);
            assertThat(code.getExpiresAt()).isAfter(NOW);
        });
        assertThat(accounts.findById(account.getMemberId())).isPresent();
    }

    @Test
    void memberNumberIsUnique() {
        MemberAccount first = newMember();
        accounts.save(first);
        Subject other = Subject.forMember(new MemberId(Uuids.v7()));
        subjects.save(other);

        MemberAccount duplicate =
                MemberAccount.onboard(other.getMemberId().orElseThrow(), other.getSub(), first.getMemberNumber());

        assertThatExceptionOfType(DuplicateKeyException.class).isThrownBy(() -> accounts.save(duplicate));
    }

    @Test
    void staffProfileKeepsTheAuthenticatorReset() {
        SubjectId boardMember = staffSubject();
        StaffProfile profile = newStaff("reset." + UUID.randomUUID());
        MfaResetWindow window = profile.resetMfaByBoard(boardMember, NOW);
        staffProfiles.save(profile);

        StaffProfile found = staffProfiles
                .findByDirectoryAccount(profile.getDirectoryAccount())
                .orElseThrow();

        assertThat(found.getResetWindow()).contains(window);
        assertThat(found.getRoles()).containsExactly("TELLER");
        assertThat(staffProfiles.findBySub(profile.getSub())).isPresent();
    }

    @Test
    void directoryAccountIsUnique() {
        String account = "unique." + UUID.randomUUID();
        staffProfiles.save(newStaff(account));

        StaffProfile duplicate = newStaff(account);

        assertThatExceptionOfType(DuplicateKeyException.class).isThrownBy(() -> staffProfiles.save(duplicate));
    }

    @Test
    void halfAnAuthenticatorResetIsRejected() {
        StaffProfile profile = newStaff("half." + UUID.randomUUID());
        staffProfiles.save(profile);

        assertThatExceptionOfType(DataIntegrityViolationException.class)
                .isThrownBy(() -> jdbc.sql("UPDATE staff_profiles SET mfa_reset_at = now() WHERE sub = :sub")
                        .param("sub", profile.getSub().value())
                        .update());
    }

    @Test
    void sessionKeepsOnlyTheHashOfTheCurrentRefreshToken() {
        Session session = Session.start(new SessionId(Uuids.v7()), staffSubject(), TokenHash.of("first"), NOW);
        sessions.save(session);
        session.rotateRefreshToken(TokenHash.of("second"));
        sessions.save(session);

        Session found = sessions.findById(session.getSessionId()).orElseThrow();
        String stored = jdbc.sql("SELECT current_token_hash FROM refresh_token_families WHERE session_id = :id")
                .param("id", session.getSessionId().value())
                .query(String.class)
                .single();

        assertThat(found.getTokenFamily().getCurrentTokenHash()).isEqualTo(TokenHash.of("second"));
        assertThat(stored).isEqualTo(TokenHash.of("second").sha256Hex()).doesNotContain("second");
        assertThat(sessions.findActiveBySubject(session.getSub())).hasSize(1);
    }

    @Test
    void sessionHasOneRefreshTokenFamily() {
        Session session = Session.start(new SessionId(Uuids.v7()), staffSubject(), TokenHash.of("token"), NOW);
        sessions.save(session);

        assertThatExceptionOfType(DuplicateKeyException.class)
                .isThrownBy(() -> jdbc.sql("""
                                INSERT INTO refresh_token_families (family_id, session_id, current_token_hash)
                                VALUES (:familyId, :sessionId, 'other')
                                """)
                        .param("familyId", Uuids.v7())
                        .param("sessionId", session.getSessionId().value())
                        .update());
    }

    @Test
    void revokedSessionIsNoLongerActive() {
        Session session = Session.start(new SessionId(Uuids.v7()), staffSubject(), TokenHash.of("token"), NOW);
        sessions.save(session);
        session.revoke(NOW);
        sessions.save(session);

        assertThat(sessions.findById(session.getSessionId()).orElseThrow().getRevokedAt())
                .contains(NOW);
        assertThat(sessions.findActiveBySubject(session.getSub())).isEmpty();
    }

    @Test
    void consumedEventIsProcessedOnce() {
        UUID eventId = Uuids.v7();

        assertThat(processedEvents.markProcessed(eventId, "MemberAdmitted")).isTrue();
        assertThat(processedEvents.markProcessed(eventId, "MemberAdmitted")).isFalse();
    }

    @Test
    void registeredClientsOfTheConfigurationAreStored() {
        assertThat(registeredClients).isInstanceOf(JdbcRegisteredClientRepository.class);
        assertThat(registeredClients.findByClientId("solidaria-web")).isNotNull();
        assertThat(jdbc.sql("SELECT client_id FROM oauth2_registered_client")
                        .query(String.class)
                        .list())
                .containsExactlyInAnyOrder("solidaria-web", "solidaria-mobile", "solidaria-desktop");
    }

    @Test
    void everyColumnHasAComment() {
        var uncommented = jdbc.sql("""
                        SELECT c.table_name || '.' || c.column_name
                        FROM information_schema.columns c
                        JOIN pg_catalog.pg_class t ON t.relname = c.table_name
                        JOIN pg_catalog.pg_namespace n ON n.oid = t.relnamespace AND n.nspname = c.table_schema
                        WHERE c.table_schema = current_schema() AND c.table_name <> 'flyway_schema_history'
                          AND col_description(t.oid, c.ordinal_position) IS NULL
                        """).query(String.class).list();

        assertThat(uncommented).isEmpty();
    }

    @Test
    void syntheticSeedIsLoaded() {
        assertThat(accounts.findByMemberNumber(new MemberNumber("1004")))
                .hasValueSatisfying(
                        account -> assertThat(account.getStatus()).isEqualTo(MemberAccountStatus.PENDING_ACTIVATION));
        assertThat(staffProfiles.findByDirectoryAccount(new DirectoryAccount("fernanda.prue19")))
                .hasValueSatisfying(
                        profile -> assertThat(profile.getFunctions()).containsExactly("PRESIDENT"));
    }

    private MemberAccount newMember() {
        Subject subject = Subject.forMember(new MemberId(Uuids.v7()));
        subjects.save(subject);
        MemberNumber number = new MemberNumber(String.valueOf(MEMBER_NUMBERS.incrementAndGet()));
        return MemberAccount.onboard(subject.getMemberId().orElseThrow(), subject.getSub(), number);
    }

    private StaffProfile newStaff(String account) {
        return new StaffProfile(staffSubject(), new DirectoryAccount(account), Set.of("TELLER"), Set.of());
    }

    private SubjectId staffSubject() {
        Subject subject = Subject.forStaff();
        subjects.save(subject);
        return subject.getSub();
    }
}
