package com.solidariaplus.identity.identityaccess.application.service;

import com.solidariaplus.identity.identityaccess.application.port.in.ActivateAccountUseCase;
import com.solidariaplus.identity.identityaccess.application.port.in.IssueActivationCodeUseCase;
import com.solidariaplus.identity.identityaccess.application.port.output.EventPublisher;
import com.solidariaplus.identity.identityaccess.application.port.output.PasswordHasher;
import com.solidariaplus.identity.identityaccess.domain.event.AuditRecorded;
import com.solidariaplus.identity.identityaccess.domain.event.AuditRecorded.Action;
import com.solidariaplus.identity.identityaccess.domain.event.AuditRecorded.Outcome;
import com.solidariaplus.identity.identityaccess.domain.exception.DomainException;
import com.solidariaplus.identity.identityaccess.domain.exception.ErrorCode;
import com.solidariaplus.identity.identityaccess.domain.model.aggregate.MemberAccount;
import com.solidariaplus.identity.identityaccess.domain.model.entity.ActivationCode;
import com.solidariaplus.identity.identityaccess.domain.model.vo.MemberId;
import com.solidariaplus.identity.identityaccess.domain.model.vo.MemberNumber;
import com.solidariaplus.identity.identityaccess.domain.model.vo.SubjectId;
import com.solidariaplus.identity.identityaccess.domain.repository.MemberAccountRepository;
import com.solidariaplus.identity.identityaccess.domain.service.ActivationCodeGenerator;
import com.solidariaplus.identity.identityaccess.domain.service.PasswordPolicy;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AccountActivationService implements IssueActivationCodeUseCase, ActivateAccountUseCase {
    private static final String ENTITY_TYPE = "MemberAccount";

    private final MemberAccountRepository accounts;
    private final PasswordHasher hasher;
    private final EventPublisher events;
    private final Clock clock;

    public AccountActivationService(
            MemberAccountRepository accounts, PasswordHasher hasher, EventPublisher events, Clock clock) {
        this.accounts = accounts;
        this.hasher = hasher;
        this.events = events;
        this.clock = clock;
    }

    @Override
    public IssuedCode issue(MemberId memberId, SubjectId tellerSub) {
        MemberAccount account =
                accounts.findById(memberId).orElseThrow(() -> new DomainException(ErrorCode.MEMBER_NOT_FOUND));
        String code = ActivationCodeGenerator.generate();
        Instant now = clock.instant();
        ActivationCode issued = account.issueActivationCode(hasher.hash(code), now);
        accounts.save(account);
        audit(Action.ACTIVATION_CODE_ISSUED, tellerSub, "TELLER", memberId, now);
        return new IssuedCode(issued.getCodeId(), memberId.value(), code, issued.getCreatedAt(), issued.getExpiresAt());
    }

    @Override
    public void activate(ActivateAccountUseCase.Command command) {
        PasswordPolicy.validate(command.newPassword());
        MemberAccount account = Optional.of(command.memberNumber())
                .filter(MemberNumber::isMemberNumber)
                .flatMap(number -> accounts.findByMemberNumber(new MemberNumber(number)))
                .filter(candidate -> candidate
                        .getActiveCode()
                        .filter(code -> hasher.matches(command.activationCode(), code.getCodeHash()))
                        .isPresent())
                .orElseThrow(() -> new DomainException(ErrorCode.AUTH_ACTIVATION_CODE_INVALID));
        Instant now = clock.instant();
        account.activate(hasher.hash(command.newPassword()), now);
        accounts.save(account);
        audit(Action.ACCOUNT_ACTIVATED, account.getSub(), "MEMBER", account.getMemberId(), now);
    }

    private void audit(Action action, SubjectId actor, String role, MemberId memberId, Instant now) {
        events.publish(AuditRecorded.of(
                action, actor, role, ENTITY_TYPE, memberId.value().toString(), Outcome.SUCCESS, now));
    }
}
