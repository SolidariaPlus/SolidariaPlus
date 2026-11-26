package com.solidariaplus.identity.identityaccess.application.service;

import com.solidariaplus.identity.identityaccess.application.port.in.EndMembershipUseCase;
import com.solidariaplus.identity.identityaccess.application.port.in.RegisterMemberUseCase;
import com.solidariaplus.identity.identityaccess.application.port.in.RevokeSessionsUseCase;
import com.solidariaplus.identity.identityaccess.application.port.out.ProcessedEventStore;
import com.solidariaplus.identity.identityaccess.domain.event.SessionRevoked.Reason;
import com.solidariaplus.identity.identityaccess.domain.model.aggregate.MemberAccount;
import com.solidariaplus.identity.identityaccess.domain.model.aggregate.Subject;
import com.solidariaplus.identity.identityaccess.domain.repository.MemberAccountRepository;
import com.solidariaplus.identity.identityaccess.domain.repository.SubjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MembershipService implements RegisterMemberUseCase, EndMembershipUseCase {
    private final SubjectRepository subjects;
    private final MemberAccountRepository accounts;
    private final ProcessedEventStore processedEvents;
    private final RevokeSessionsUseCase sessions;

    public MembershipService(
            SubjectRepository subjects,
            MemberAccountRepository accounts,
            ProcessedEventStore processedEvents,
            RevokeSessionsUseCase sessions) {
        this.subjects = subjects;
        this.accounts = accounts;
        this.processedEvents = processedEvents;
        this.sessions = sessions;
    }

    @Override
    public void register(RegisterMemberUseCase.Command command) {
        if (!processedEvents.markProcessed(command.eventId())) {
            return;
        }
        Subject subject = Subject.forMember(command.memberId());
        subjects.save(subject);
        accounts.save(MemberAccount.onboard(command.memberId(), subject.getSub(), command.memberNumber()));
    }

    @Override
    public void end(EndMembershipUseCase.Command command) {
        if (!processedEvents.markProcessed(command.eventId())) {
            return;
        }
        accounts.findById(command.memberId()).ifPresent(account -> {
            account.disable();
            accounts.save(account);
            sessions.revokeAllSessions(account.getSub(), Reason.MEMBERSHIP_ENDED);
        });
    }
}
