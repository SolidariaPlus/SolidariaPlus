package com.solidariaplus.identity.identityaccess.application.service;

import com.solidariaplus.identity.identityaccess.application.port.in.ResolveMemberIdUseCase;
import com.solidariaplus.identity.identityaccess.application.port.in.SignInUseCase;
import com.solidariaplus.identity.identityaccess.application.port.out.DirectoryGateway;
import com.solidariaplus.identity.identityaccess.application.port.out.DirectoryGateway.DirectoryIdentity;
import com.solidariaplus.identity.identityaccess.application.port.out.EventPublisher;
import com.solidariaplus.identity.identityaccess.application.port.out.LoginAttemptStore;
import com.solidariaplus.identity.identityaccess.application.port.out.PasswordHasher;
import com.solidariaplus.identity.identityaccess.domain.event.AuditRecorded;
import com.solidariaplus.identity.identityaccess.domain.event.AuditRecorded.Action;
import com.solidariaplus.identity.identityaccess.domain.event.AuditRecorded.Outcome;
import com.solidariaplus.identity.identityaccess.domain.exception.DomainException;
import com.solidariaplus.identity.identityaccess.domain.exception.ErrorCode;
import com.solidariaplus.identity.identityaccess.domain.model.aggregate.MemberAccount;
import com.solidariaplus.identity.identityaccess.domain.model.aggregate.StaffProfile;
import com.solidariaplus.identity.identityaccess.domain.model.aggregate.Subject;
import com.solidariaplus.identity.identityaccess.domain.model.vo.MemberId;
import com.solidariaplus.identity.identityaccess.domain.model.vo.MemberNumber;
import com.solidariaplus.identity.identityaccess.domain.model.vo.SubjectId;
import com.solidariaplus.identity.identityaccess.domain.repository.MemberAccountRepository;
import com.solidariaplus.identity.identityaccess.domain.repository.StaffProfileRepository;
import com.solidariaplus.identity.identityaccess.domain.repository.SubjectRepository;
import com.solidariaplus.identity.identityaccess.domain.service.LockoutPolicy;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SignInService implements SignInUseCase, ResolveMemberIdUseCase {
    private static final String MEMBER_ROLE = "MEMBER";

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
    public AuthenticatedSubject signIn(String username, String password) {
        if (attempts.isLocked(username)) {
            throw new DomainException(ErrorCode.AUTH_ACCOUNT_LOCKED);
        }
        Instant now = clock.instant();
        Optional<AuthenticatedSubject> subject = MemberNumber.isMemberNumber(username)
                ? authenticateMember(new MemberNumber(username), password)
                : authenticateStaff(username, password, now);
        return subject.map(authenticated -> succeed(username, authenticated, now))
                .orElseThrow(() -> fail(username));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MemberId> memberIdOf(SubjectId sub) {
        return subjects.findById(sub).flatMap(Subject::getMemberId);
    }

    private Optional<AuthenticatedSubject> authenticateMember(MemberNumber memberNumber, String password) {
        return accounts.findByMemberNumber(memberNumber)
                .filter(MemberAccount::canSignIn)
                .filter(account -> account.getPasswordHash()
                        .filter(hash -> hasher.matches(password, hash))
                        .isPresent())
                .map(account -> new AuthenticatedSubject(account.getSub(), Set.of(MEMBER_ROLE), Set.of()));
    }

    private Optional<AuthenticatedSubject> authenticateStaff(String username, String password, Instant now) {
        return directory.authenticate(username, password).flatMap(identity -> {
            StaffProfile profile =
                    staffProfiles.findByDirectoryAccount(identity.account()).orElseGet(() -> registerStaff(identity));
            profile.syncRoles(identity.roles());
            staffProfiles.save(profile);
            return Optional.of(new AuthenticatedSubject(profile.getSub(), identity.roles(), identity.functions()))
                    .filter(authenticated -> profile.canAuthenticate(now));
        });
    }

    private StaffProfile registerStaff(DirectoryIdentity identity) {
        Subject subject = Subject.forStaff();
        subjects.save(subject);
        return new StaffProfile(subject.getSub(), identity.account(), identity.roles());
    }

    private AuthenticatedSubject succeed(String username, AuthenticatedSubject subject, Instant now) {
        attempts.clear(username);
        String sub = subject.sub().value().toString();
        events.publish(AuditRecorded.of(
                Action.SIGN_IN,
                subject.sub(),
                String.join(",", subject.roles()),
                "Subject",
                sub,
                Outcome.SUCCESS,
                now));
        return subject;
    }

    private DomainException fail(String username) {
        if (LockoutPolicy.shouldLock(attempts.recordFailure(username, LockoutPolicy.WINDOW))) {
            attempts.lock(username, LockoutPolicy.WINDOW);
            return new DomainException(ErrorCode.AUTH_ACCOUNT_LOCKED);
        }
        return new DomainException(ErrorCode.UNAUTHENTICATED);
    }
}
