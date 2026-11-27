package com.solidariaplus.identity.identityaccess.domain.model.aggregate;

import com.solidariaplus.identity.identityaccess.domain.exception.DomainException;
import com.solidariaplus.identity.identityaccess.domain.exception.ErrorCode;
import com.solidariaplus.identity.identityaccess.domain.model.entity.ActivationCode;
import com.solidariaplus.identity.identityaccess.domain.model.vo.Argon2Hash;
import com.solidariaplus.identity.identityaccess.domain.model.vo.EncryptedTotpSecret;
import com.solidariaplus.identity.identityaccess.domain.model.vo.MemberAccountStatus;
import com.solidariaplus.identity.identityaccess.domain.model.vo.MemberId;
import com.solidariaplus.identity.identityaccess.domain.model.vo.MemberNumber;
import com.solidariaplus.identity.identityaccess.domain.model.vo.SubjectId;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public final class MemberAccount {
    private final MemberId memberId;
    private final SubjectId sub;
    private final MemberNumber memberNumber;
    private Argon2Hash passwordHash;
    private EncryptedTotpSecret totpSecretEnc;
    private ActivationCode activeCode;
    private MemberAccountStatus status;

    public static MemberAccount onboard(MemberId memberId, SubjectId sub, MemberNumber memberNumber) {
        return new MemberAccount(memberId, sub, memberNumber, MemberAccountStatus.PENDING_ACTIVATION);
    }

    public static MemberAccount reconstitute(
            MemberId memberId,
            SubjectId sub,
            MemberNumber memberNumber,
            Argon2Hash passwordHash,
            EncryptedTotpSecret totpSecretEnc,
            ActivationCode activeCode,
            MemberAccountStatus status) {
        MemberAccount account = new MemberAccount(memberId, sub, memberNumber, status);
        account.passwordHash = passwordHash;
        account.totpSecretEnc = totpSecretEnc;
        account.activeCode = activeCode;
        return account;
    }

    private MemberAccount(MemberId memberId, SubjectId sub, MemberNumber memberNumber, MemberAccountStatus status) {
        this.memberId = Objects.requireNonNull(memberId, "memberId cannot be null");
        this.sub = Objects.requireNonNull(sub, "sub cannot be null");
        this.memberNumber = Objects.requireNonNull(memberNumber, "memberNumber cannot be null");
        this.status = Objects.requireNonNull(status, "status cannot be null");
    }

    public ActivationCode issueActivationCode(Argon2Hash codeHash, Instant now) {
        requireNotDisabled(ErrorCode.MEMBER_NOT_ELIGIBLE);
        activeCode = ActivationCode.issue(codeHash, now);
        return activeCode;
    }

    public void activate(Argon2Hash newPasswordHash, Instant now) {
        requireNotDisabled(ErrorCode.AUTH_ACTIVATION_CODE_INVALID);
        getActiveCode()
                .orElseThrow(() -> new DomainException(ErrorCode.AUTH_ACTIVATION_CODE_INVALID))
                .consume(now);
        passwordHash = Objects.requireNonNull(newPasswordHash, "newPasswordHash cannot be null");
        status = MemberAccountStatus.ACTIVE;
    }

    public void disable() {
        status = MemberAccountStatus.DISABLED;
    }

    public boolean canSignIn() {
        return status == MemberAccountStatus.ACTIVE;
    }

    private void requireNotDisabled(ErrorCode code) {
        if (status == MemberAccountStatus.DISABLED) {
            throw new DomainException(code);
        }
    }

    public MemberId getMemberId() {
        return memberId;
    }

    public SubjectId getSub() {
        return sub;
    }

    public MemberNumber getMemberNumber() {
        return memberNumber;
    }

    public Optional<Argon2Hash> getPasswordHash() {
        return Optional.ofNullable(passwordHash);
    }

    public Optional<EncryptedTotpSecret> getTotpSecretEnc() {
        return Optional.ofNullable(totpSecretEnc);
    }

    public Optional<ActivationCode> getActiveCode() {
        return Optional.ofNullable(activeCode);
    }

    public MemberAccountStatus getStatus() {
        return status;
    }
}
