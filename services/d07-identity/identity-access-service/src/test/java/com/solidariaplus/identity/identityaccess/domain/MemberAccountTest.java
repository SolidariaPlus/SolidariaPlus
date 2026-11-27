package com.solidariaplus.identity.identityaccess.domain;

import static com.solidariaplus.identity.identityaccess.domain.DomainFixtures.HASH;
import static com.solidariaplus.identity.identityaccess.domain.DomainFixtures.NOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.solidariaplus.identity.identityaccess.domain.exception.DomainException;
import com.solidariaplus.identity.identityaccess.domain.exception.ErrorCode;
import com.solidariaplus.identity.identityaccess.domain.model.aggregate.MemberAccount;
import com.solidariaplus.identity.identityaccess.domain.model.aggregate.Subject;
import com.solidariaplus.identity.identityaccess.domain.model.entity.ActivationCode;
import com.solidariaplus.identity.identityaccess.domain.model.vo.MemberAccountStatus;
import com.solidariaplus.identity.identityaccess.domain.model.vo.MemberId;
import com.solidariaplus.identity.identityaccess.domain.model.vo.MemberNumber;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MemberAccountTest {
    private final MemberId memberId = new MemberId(UUID.randomUUID());
    private final Subject subject = Subject.forMember(memberId);
    private final MemberAccount account = MemberAccount.onboard(memberId, subject.getSub(), new MemberNumber("000123"));

    @Test
    void onboardedAccountWaitsForActivation() {
        assertThat(account.getStatus()).isEqualTo(MemberAccountStatus.PENDING_ACTIVATION);
        assertThat(account.canSignIn()).isFalse();
        assertThat(account.getActiveCode()).isEmpty();
        assertThat(subject.getMemberId()).contains(memberId);
    }

    @Test
    void activationConsumesTheCodeAndSetsThePassword() {
        ActivationCode code = account.issueActivationCode(HASH, NOW);

        account.activate(HASH, NOW.plusSeconds(60));

        assertThat(account.canSignIn()).isTrue();
        assertThat(account.getPasswordHash()).contains(HASH);
        assertThat(code.isUsed()).isTrue();
        assertThat(code.getExpiresAt()).isEqualTo(NOW.plus(Duration.ofHours(72)));
    }

    @Test
    void usedExpiredOrMissingCodesAreRejectedAlike() {
        assertCode(() -> account.activate(HASH, NOW), ErrorCode.AUTH_ACTIVATION_CODE_INVALID);
        account.issueActivationCode(HASH, NOW);
        assertCode(
                () -> account.activate(HASH, NOW.plus(Duration.ofHours(73))), ErrorCode.AUTH_ACTIVATION_CODE_INVALID);
        account.activate(HASH, NOW);
        assertCode(() -> account.activate(HASH, NOW), ErrorCode.AUTH_ACTIVATION_CODE_INVALID);
    }

    @Test
    void endedMembershipBlocksNewCodes() {
        account.disable();

        assertThat(account.canSignIn()).isFalse();
        assertCode(() -> account.issueActivationCode(HASH, NOW), ErrorCode.MEMBER_NOT_ELIGIBLE);
    }

    @Test
    void reconstitutedAccountKeepsItsState() {
        MemberAccount stored = MemberAccount.reconstitute(
                memberId, subject.getSub(), account.getMemberNumber(), HASH, null, null, MemberAccountStatus.ACTIVE);

        assertThat(stored.canSignIn()).isTrue();
        assertThat(stored.getMemberId()).isEqualTo(memberId);
        assertThat(stored.getTotpSecretEnc()).isEmpty();
    }

    static void assertCode(Runnable action, ErrorCode code) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(
                        DomainException.class, e -> assertThat(e.getCode()).isEqualTo(code));
    }
}
