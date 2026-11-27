package com.solidariaplus.identity.identityaccess.domain;

import static com.solidariaplus.identity.identityaccess.domain.DomainFixtures.HASH;
import static com.solidariaplus.identity.identityaccess.domain.DomainFixtures.NOW;
import static com.solidariaplus.identity.identityaccess.domain.MemberAccountTest.assertCode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import com.solidariaplus.identity.identityaccess.domain.event.AuditRecorded;
import com.solidariaplus.identity.identityaccess.domain.event.SessionRevoked;
import com.solidariaplus.identity.identityaccess.domain.exception.ErrorCode;
import com.solidariaplus.identity.identityaccess.domain.model.vo.Argon2Hash;
import com.solidariaplus.identity.identityaccess.domain.model.vo.DirectoryAccount;
import com.solidariaplus.identity.identityaccess.domain.model.vo.EncryptedTotpSecret;
import com.solidariaplus.identity.identityaccess.domain.model.vo.MemberNumber;
import com.solidariaplus.identity.identityaccess.domain.model.vo.MfaResetWindow;
import com.solidariaplus.identity.identityaccess.domain.model.vo.SessionId;
import com.solidariaplus.identity.identityaccess.domain.model.vo.SubjectId;
import com.solidariaplus.identity.identityaccess.domain.model.vo.TokenHash;
import com.solidariaplus.identity.identityaccess.domain.service.ActivationCodeGenerator;
import com.solidariaplus.identity.identityaccess.domain.service.LockoutPolicy;
import com.solidariaplus.identity.identityaccess.domain.service.PasswordPolicy;
import com.solidariaplus.identity.identityaccess.domain.service.SignInRateLimitPolicy;
import com.solidariaplus.identity.identityaccess.domain.service.Uuids;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RulesAndValuesTest {

    @Test
    void passwordPolicyRequiresTwelveMixedCharacters() {
        PasswordPolicy.validate("Ahorro-Seguro-2026");
        assertCode(() -> PasswordPolicy.validate("Corta-1"), ErrorCode.VALIDATION_FAILED);
        assertCode(() -> PasswordPolicy.validate("sinmayusculas-2026"), ErrorCode.VALIDATION_FAILED);
        assertCode(() -> PasswordPolicy.validate(null), ErrorCode.VALIDATION_FAILED);
    }

    @Test
    void twentyFirstRequestInAMinuteExceedsTheRateLimit() {
        assertThat(SignInRateLimitPolicy.isExceeded(20)).isFalse();
        assertThat(SignInRateLimitPolicy.isExceeded(21)).isTrue();
    }

    @Test
    void fifthFailureLocksTheAccount() {
        assertThat(LockoutPolicy.shouldLock(4)).isFalse();
        assertThat(LockoutPolicy.shouldLock(5)).isTrue();
        assertThat(LockoutPolicy.WINDOW).isEqualTo(Duration.ofMinutes(15));
    }

    @Test
    void activationCodesHaveEightUnambiguousCharacters() {
        assertThat(ActivationCodeGenerator.generate()).matches("[A-HJ-NP-Z2-9]{8}");
    }

    @Test
    void identifiersAreVersionSeven() {
        UUID id = Uuids.v7();

        assertThat(id.version()).isEqualTo(7);
        assertThat(id.variant()).isEqualTo(2);
    }

    @Test
    void valuesRejectInvalidInputAndHideSecrets() {
        assertThatIllegalArgumentException().isThrownBy(() -> new Argon2Hash("plain"));
        assertThatIllegalArgumentException().isThrownBy(() -> new MemberNumber("ABC"));
        assertThatIllegalArgumentException().isThrownBy(() -> new DirectoryAccount(" "));
        assertThatIllegalArgumentException().isThrownBy(() -> new TokenHash(""));
        assertThatIllegalArgumentException().isThrownBy(() -> new EncryptedTotpSecret(new byte[0]));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new MfaResetWindow(NOW, SubjectId.generate(), NOW.minusSeconds(1)));
        assertThat(HASH.toString()).doesNotContain("argon2id");
        assertThat(new DirectoryAccount("ana.prue01").toString()).doesNotContain("ana");
        assertThat(TokenHash.of("token")).isEqualTo(TokenHash.of("token")).hasToString("TokenHash[REDACTED]");
        assertThat(DomainFixtures.TOTP.ciphertext()).containsExactly(1, 2, 3);
        assertThat(DomainFixtures.TOTP.toString()).doesNotContain("1");
    }

    @Test
    void revocationEventsCarryTheTokenDeadline() {
        SubjectId subject = SubjectId.generate();
        SessionId sessionId = new SessionId(UUID.randomUUID());

        SessionRevoked one = SessionRevoked.ofSession(subject, sessionId, SessionRevoked.Reason.SIGN_OUT, NOW);
        SessionRevoked all = SessionRevoked.ofAllSessions(subject, SessionRevoked.Reason.MEMBERSHIP_ENDED, NOW);

        assertThat(one.scope()).isEqualTo(SessionRevoked.Scope.SESSION);
        assertThat(one.sessionId()).contains(sessionId);
        assertThat(all.sessionId()).isEmpty();
        assertThat(all.tokensExpireBy()).isEqualTo(NOW.plus(Duration.ofMinutes(10)));
    }

    @Test
    void auditRecordsGetTheirOwnIdentifier() {
        AuditRecorded record = AuditRecorded.of(
                AuditRecorded.Action.SIGN_IN,
                SubjectId.generate(),
                "MEMBER",
                "Subject",
                "id",
                AuditRecorded.Outcome.SUCCESS,
                NOW);

        assertThat(record.recordId().version()).isEqualTo(7);
        assertThat(record.occurredAt()).isEqualTo(NOW);
    }
}
