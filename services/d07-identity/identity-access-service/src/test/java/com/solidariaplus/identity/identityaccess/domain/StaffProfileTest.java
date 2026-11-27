package com.solidariaplus.identity.identityaccess.domain;

import static com.solidariaplus.identity.identityaccess.domain.DomainFixtures.NOW;
import static com.solidariaplus.identity.identityaccess.domain.DomainFixtures.TOTP;
import static com.solidariaplus.identity.identityaccess.domain.MemberAccountTest.assertCode;
import static org.assertj.core.api.Assertions.assertThat;

import com.solidariaplus.identity.identityaccess.domain.exception.ErrorCode;
import com.solidariaplus.identity.identityaccess.domain.model.aggregate.StaffProfile;
import com.solidariaplus.identity.identityaccess.domain.model.aggregate.Subject;
import com.solidariaplus.identity.identityaccess.domain.model.vo.DirectoryAccount;
import com.solidariaplus.identity.identityaccess.domain.model.vo.MfaResetWindow;
import com.solidariaplus.identity.identityaccess.domain.model.vo.SecondFactor;
import com.solidariaplus.identity.identityaccess.domain.model.vo.SubjectId;
import java.time.Duration;
import java.util.Set;
import org.junit.jupiter.api.Test;

class StaffProfileTest {
    private final SubjectId president = SubjectId.generate();
    private final StaffProfile teller = new StaffProfile(
            Subject.forStaff().getSub(), new DirectoryAccount("ana.prue01"), Set.of("TELLER"), Set.of());

    @Test
    void firstSignInRequiresEnrolmentThenVerification() {
        assertThat(teller.secondFactor(NOW)).isEqualTo(SecondFactor.ENROL);

        teller.enrolTotp(TOTP, NOW);

        assertThat(teller.secondFactor(NOW)).isEqualTo(SecondFactor.VERIFY);
        assertThat(teller.getTotpSecretEnc()).isPresent();
        assertCode(() -> teller.enrolTotp(TOTP, NOW), ErrorCode.AUTH_MFA_REQUIRED);
    }

    @Test
    void resetOpensA24HourEnrolmentWindow() {
        teller.enrolTotp(TOTP, NOW);

        MfaResetWindow window = teller.resetMfaByBoard(president, NOW);

        assertThat(window.enrolmentDeadline()).isEqualTo(NOW.plus(Duration.ofHours(24)));
        assertThat(teller.secondFactor(NOW.plusSeconds(60))).isEqualTo(SecondFactor.ENROL);
        assertThat(teller.secondFactor(NOW.plus(Duration.ofHours(25)))).isEqualTo(SecondFactor.REFUSED);
        assertThat(teller.canAuthenticate(NOW.plus(Duration.ofHours(25)))).isFalse();
        assertCode(() -> teller.enrolTotp(TOTP, NOW.plus(Duration.ofHours(25))), ErrorCode.AUTH_MFA_REQUIRED);
    }

    @Test
    void nobodyResetsTheirOwnAuthenticator() {
        assertCode(() -> teller.resetMfaByBoard(teller.getSub(), NOW), ErrorCode.SAME_PERSON_NOT_ALLOWED);
    }

    @Test
    void directoryGroupsReplaceRolesAndFunctions() {
        teller.syncDirectory(Set.of("BOARD"), Set.of("PRESIDENT"));

        assertThat(teller.getRoles()).containsExactly("BOARD");
        assertThat(teller.getFunctions()).containsExactly("PRESIDENT");
        assertThat(teller.getDirectoryAccount().username()).isEqualTo("ana.prue01");
    }

    @Test
    void reconstitutedProfileKeepsTheResetWindow() {
        MfaResetWindow window = new MfaResetWindow(NOW, president, NOW.plus(Duration.ofHours(24)));
        StaffProfile stored = StaffProfile.reconstitute(
                teller.getSub(), teller.getDirectoryAccount(), Set.of(), Set.of(), null, window);

        assertThat(stored.getResetWindow()).contains(window);
        assertThat(stored.secondFactor(NOW)).isEqualTo(SecondFactor.ENROL);
    }
}
