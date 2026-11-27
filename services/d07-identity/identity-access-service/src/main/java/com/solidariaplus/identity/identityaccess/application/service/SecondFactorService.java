package com.solidariaplus.identity.identityaccess.application.service;

import com.solidariaplus.identity.identityaccess.application.port.in.SecondFactorUseCase;
import com.solidariaplus.identity.identityaccess.application.port.output.EventPublisher;
import com.solidariaplus.identity.identityaccess.application.port.output.LoginAttemptStore;
import com.solidariaplus.identity.identityaccess.application.port.output.SecretCipher;
import com.solidariaplus.identity.identityaccess.application.port.output.TotpAuthenticator;
import com.solidariaplus.identity.identityaccess.domain.event.AuditRecorded;
import com.solidariaplus.identity.identityaccess.domain.event.AuditRecorded.Action;
import com.solidariaplus.identity.identityaccess.domain.event.AuditRecorded.Outcome;
import com.solidariaplus.identity.identityaccess.domain.exception.DomainException;
import com.solidariaplus.identity.identityaccess.domain.exception.ErrorCode;
import com.solidariaplus.identity.identityaccess.domain.model.aggregate.StaffProfile;
import com.solidariaplus.identity.identityaccess.domain.model.vo.SecondFactor;
import com.solidariaplus.identity.identityaccess.domain.model.vo.SubjectId;
import com.solidariaplus.identity.identityaccess.domain.repository.StaffProfileRepository;
import com.solidariaplus.identity.identityaccess.domain.service.LockoutPolicy;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SecondFactorService implements SecondFactorUseCase {
    private final StaffProfileRepository staffProfiles;
    private final TotpAuthenticator totp;
    private final SecretCipher cipher;
    private final LoginAttemptStore attempts;
    private final EventPublisher events;
    private final Clock clock;

    public SecondFactorService(
            StaffProfileRepository staffProfiles,
            TotpAuthenticator totp,
            SecretCipher cipher,
            LoginAttemptStore attempts,
            EventPublisher events,
            Clock clock) {
        this.staffProfiles = staffProfiles;
        this.totp = totp;
        this.cipher = cipher;
        this.attempts = attempts;
        this.events = events;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public SecondFactor requirementOf(SubjectId sub) {
        return staffProfiles
                .findBySub(sub)
                .map(profile -> profile.secondFactor(clock.instant()))
                .orElse(SecondFactor.NONE);
    }

    @Override
    public String newSecret() {
        return totp.newSecret();
    }

    @Override
    public void enrol(SubjectId sub, String secret, String code) {
        Instant now = clock.instant();
        StaffProfile profile = checkCode(sub, secret, code, now);
        profile.enrolTotp(cipher.encrypt(secret, sub), now);
        staffProfiles.save(profile);
        events.publish(AuditRecorded.of(
                Action.MFA_ENROLLED, sub, "STAFF", "StaffProfile", sub.value().toString(), Outcome.SUCCESS, now));
    }

    @Override
    @Transactional(readOnly = true)
    public void verify(SubjectId sub, String code) {
        StaffProfile profile = staffProfile(sub);
        String secret = profile.getTotpSecretEnc()
                .map(ciphertext -> cipher.decrypt(ciphertext, sub))
                .orElseThrow(() -> new DomainException(ErrorCode.AUTH_MFA_REQUIRED));
        checkCode(sub, secret, code, clock.instant());
    }

    private StaffProfile checkCode(SubjectId sub, String secret, String code, Instant now) {
        String identifier = sub.value().toString();
        if (attempts.isLocked(identifier)) {
            throw new DomainException(ErrorCode.AUTH_ACCOUNT_LOCKED);
        }
        if (totp.verify(secret, code, now)) {
            attempts.clear(identifier);
            return staffProfile(sub);
        }
        if (LockoutPolicy.shouldLock(attempts.recordFailure(identifier, LockoutPolicy.WINDOW))) {
            attempts.lock(identifier, LockoutPolicy.WINDOW);
            throw new DomainException(ErrorCode.AUTH_ACCOUNT_LOCKED);
        }
        throw new DomainException(ErrorCode.AUTH_MFA_REQUIRED);
    }

    private StaffProfile staffProfile(SubjectId sub) {
        return staffProfiles.findBySub(sub).orElseThrow(() -> new DomainException(ErrorCode.AUTH_MFA_REQUIRED));
    }
}
