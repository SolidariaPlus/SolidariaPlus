package com.solidariaplus.identity.identityaccess.application.service;

import com.solidariaplus.identity.identityaccess.application.port.in.SignInUseCase;
import com.solidariaplus.identity.identityaccess.application.port.output.DirectoryGateway;
import com.solidariaplus.identity.identityaccess.application.port.output.DirectoryGateway.DirectoryIdentity;
import com.solidariaplus.identity.identityaccess.application.port.output.EventPublisher;
import com.solidariaplus.identity.identityaccess.application.port.output.LoginAttemptStore;
import com.solidariaplus.identity.identityaccess.application.port.output.PasswordHasher;
import com.solidariaplus.identity.identityaccess.domain.event.AuditRecorded;
import com.solidariaplus.identity.identityaccess.domain.event.AuditRecorded.Action;
import com.solidariaplus.identity.identityaccess.domain.event.AuditRecorded.Outcome;
import com.solidariaplus.identity.identityaccess.domain.exception.DomainException;
import com.solidariaplus.identity.identityaccess.domain.exception.ErrorCode;
import com.solidariaplus.identity.identityaccess.domain.model.aggregate.MemberAccount;
import com.solidariaplus.identity.identityaccess.domain.model.aggregate.StaffProfile;
import com.solidariaplus.identity.identityaccess.domain.model.aggregate.Subject;
import com.solidariaplus.identity.identityaccess.domain.model.vo.MemberNumber;
import com.solidariaplus.identity.identityaccess.domain.model.vo.SubjectId;
import com.solidariaplus.identity.identityaccess.domain.repository.MemberAccountRepository;
import com.solidariaplus.identity.identityaccess.domain.repository.StaffProfileRepository;
import com.solidariaplus.identity.identityaccess.domain.repository.SubjectRepository;
import com.solidariaplus.identity.identityaccess.domain.service.LockoutPolicy;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SignInService implements SignInUseCase {
    private final SubjectRepository subjects;
    private final MemberAccountRepository accounts;
    private final StaffProfileRepository staffProfiles;
    private final DirectoryGateway directory;
    private final PasswordHasher hasher;
    private final LoginAttemptStore attempts;
    private final EventPublisher events;
    private final Clock clock;

    public SignInService(
            SubjectRepository subjects,
            MemberAccountRepository accounts,
            StaffProfileRepository staffProfiles,
            DirectoryGateway directory,
            PasswordHasher hasher,
            LoginAttemptStore attempts,
            EventPublisher events,
            Clock clock) {
        this.subjects = subjects;
        this.accounts = accounts;
        this.staffProfiles = staffProfiles;
        this.directory = directory;
        this.hasher = hasher;
        this.attempts = attempts;
        this.events = events;
        this.clock = clock;
    }

    @Override
    public SubjectId signIn(String username, String password) {
        if (attempts.isLocked(username)) {
            throw new DomainException(ErrorCode.AUTH_ACCOUNT_LOCKED);
        }
        Instant now = clock.instant();
        Optional<SubjectId> subject = MemberNumber.isMemberNumber(username)
                ? authenticateMember(new MemberNumber(username), password)
                : authenticateStaff(username, password, now);
        return subject.map(sub -> succeed(username, sub, now)).orElseThrow(() -> fail(username));
    }

    private Optional<SubjectId> authenticateMember(MemberNumber memberNumber, String password) {
        return accounts.findByMemberNumber(memberNumber)
                .filter(MemberAccount::canSignIn)
                .filter(account -> account.getPasswordHash()
                        .filter(hash -> hasher.matches(password, hash))
                        .isPresent())
                .map(MemberAccount::getSub);
    }

    private Optional<SubjectId> authenticateStaff(String username, String password, Instant now) {
        return directory.authenticate(username, password).flatMap(identity -> {
            StaffProfile profile =
                    staffProfiles.findByDirectoryAccount(identity.account()).orElseGet(() -> registerStaff(identity));
            profile.syncDirectory(identity.roles(), identity.functions());
            staffProfiles.save(profile);
            return Optional.of(profile.getSub()).filter(sub -> profile.canAuthenticate(now));
        });
    }

    private StaffProfile registerStaff(DirectoryIdentity identity) {
        Subject subject = Subject.forStaff();
        subjects.save(subject);
        return new StaffProfile(subject.getSub(), identity.account(), identity.roles(), identity.functions());
    }

    private SubjectId succeed(String username, SubjectId sub, Instant now) {
        attempts.clear(username);
        events.publish(AuditRecorded.of(
                Action.SIGN_IN, sub, "SUBJECT", "Subject", sub.value().toString(), Outcome.SUCCESS, now));
        return sub;
    }

    private DomainException fail(String username) {
        if (LockoutPolicy.shouldLock(attempts.recordFailure(username, LockoutPolicy.WINDOW))) {
            attempts.lock(username, LockoutPolicy.WINDOW);
            return new DomainException(ErrorCode.AUTH_ACCOUNT_LOCKED);
        }
        return new DomainException(ErrorCode.UNAUTHENTICATED);
    }
}
