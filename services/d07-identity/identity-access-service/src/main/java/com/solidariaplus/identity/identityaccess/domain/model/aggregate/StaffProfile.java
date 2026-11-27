package com.solidariaplus.identity.identityaccess.domain.model.aggregate;

import com.solidariaplus.identity.identityaccess.domain.exception.DomainException;
import com.solidariaplus.identity.identityaccess.domain.exception.ErrorCode;
import com.solidariaplus.identity.identityaccess.domain.model.vo.DirectoryAccount;
import com.solidariaplus.identity.identityaccess.domain.model.vo.EncryptedTotpSecret;
import com.solidariaplus.identity.identityaccess.domain.model.vo.MfaResetWindow;
import com.solidariaplus.identity.identityaccess.domain.model.vo.SecondFactor;
import com.solidariaplus.identity.identityaccess.domain.model.vo.SubjectId;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class StaffProfile {
    private static final Duration MFA_ENROLMENT_GRACE_PERIOD = Duration.ofHours(24);

    private final SubjectId sub;
    private final DirectoryAccount directoryAccount;
    private Set<String> roles;
    private Set<String> functions;
    private EncryptedTotpSecret totpSecretEnc;
    private MfaResetWindow resetWindow;

    public StaffProfile(SubjectId sub, DirectoryAccount directoryAccount, Set<String> roles, Set<String> functions) {
        this.sub = Objects.requireNonNull(sub, "sub cannot be null");
        this.directoryAccount = Objects.requireNonNull(directoryAccount, "directoryAccount cannot be null");
        syncDirectory(roles, functions);
    }

    public static StaffProfile reconstitute(
            SubjectId sub,
            DirectoryAccount directoryAccount,
            Set<String> roles,
            Set<String> functions,
            EncryptedTotpSecret totpSecretEnc,
            MfaResetWindow resetWindow) {
        StaffProfile profile = new StaffProfile(sub, directoryAccount, roles, functions);
        profile.totpSecretEnc = totpSecretEnc;
        profile.resetWindow = resetWindow;
        return profile;
    }

    public MfaResetWindow resetMfaByBoard(SubjectId boardMemberSub, Instant now) {
        if (sub.equals(boardMemberSub)) {
            throw new DomainException(ErrorCode.SAME_PERSON_NOT_ALLOWED);
        }
        this.totpSecretEnc = null;
        this.resetWindow = new MfaResetWindow(now, boardMemberSub, now.plus(MFA_ENROLMENT_GRACE_PERIOD));
        return resetWindow;
    }

    public void enrolTotp(EncryptedTotpSecret newTotpCiphertext, Instant now) {
        if (secondFactor(now) != SecondFactor.ENROL) {
            throw new DomainException(ErrorCode.AUTH_MFA_REQUIRED);
        }
        this.totpSecretEnc = Objects.requireNonNull(newTotpCiphertext, "newTotpCiphertext cannot be null");
        this.resetWindow = null;
    }

    public void syncDirectory(Set<String> directoryRoles, Set<String> directoryFunctions) {
        this.roles = Set.copyOf(directoryRoles);
        this.functions = Set.copyOf(directoryFunctions);
    }

    public boolean canAuthenticate(Instant now) {
        return resetWindow == null || !resetWindow.isExpired(now);
    }

    public SecondFactor secondFactor(Instant now) {
        if (totpSecretEnc != null) {
            return SecondFactor.VERIFY;
        }
        return canAuthenticate(now) ? SecondFactor.ENROL : SecondFactor.REFUSED;
    }

    public SubjectId getSub() {
        return sub;
    }

    public DirectoryAccount getDirectoryAccount() {
        return directoryAccount;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public Set<String> getFunctions() {
        return functions;
    }

    public Optional<EncryptedTotpSecret> getTotpSecretEnc() {
        return Optional.ofNullable(totpSecretEnc);
    }

    public Optional<MfaResetWindow> getResetWindow() {
        return Optional.ofNullable(resetWindow);
    }
}
