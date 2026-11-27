package com.solidariaplus.identity.identityaccess.application.service;

import com.solidariaplus.identity.identityaccess.application.port.in.ResetStaffAuthenticatorUseCase;
import com.solidariaplus.identity.identityaccess.application.port.in.RevokeSessionsUseCase;
import com.solidariaplus.identity.identityaccess.application.port.output.EventPublisher;
import com.solidariaplus.identity.identityaccess.domain.event.AuditRecorded;
import com.solidariaplus.identity.identityaccess.domain.event.AuditRecorded.Action;
import com.solidariaplus.identity.identityaccess.domain.event.AuditRecorded.Outcome;
import com.solidariaplus.identity.identityaccess.domain.event.SessionRevoked.Reason;
import com.solidariaplus.identity.identityaccess.domain.exception.DomainException;
import com.solidariaplus.identity.identityaccess.domain.exception.ErrorCode;
import com.solidariaplus.identity.identityaccess.domain.model.aggregate.StaffProfile;
import com.solidariaplus.identity.identityaccess.domain.model.vo.DirectoryAccount;
import com.solidariaplus.identity.identityaccess.domain.model.vo.MfaResetWindow;
import com.solidariaplus.identity.identityaccess.domain.model.vo.SubjectId;
import com.solidariaplus.identity.identityaccess.domain.repository.StaffProfileRepository;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class StaffAuthenticatorService implements ResetStaffAuthenticatorUseCase {
    private final StaffProfileRepository staffProfiles;
    private final RevokeSessionsUseCase sessions;
    private final EventPublisher events;
    private final Clock clock;

    public StaffAuthenticatorService(
            StaffProfileRepository staffProfiles, RevokeSessionsUseCase sessions, EventPublisher events, Clock clock) {
        this.staffProfiles = staffProfiles;
        this.sessions = sessions;
        this.events = events;
        this.clock = clock;
    }

    @Override
    public Result reset(SubjectId boardMemberSub, DirectoryAccount target) {
        StaffProfile profile = staffProfiles
                .findByDirectoryAccount(target)
                .orElseThrow(() -> new DomainException(ErrorCode.NOT_FOUND));
        MfaResetWindow window = profile.resetMfaByBoard(boardMemberSub, clock.instant());
        staffProfiles.save(profile);
        int revoked = sessions.revokeAllSessions(profile.getSub(), Reason.ADMINISTRATIVE);
        events.publish(AuditRecorded.of(
                Action.STAFF_AUTHENTICATOR_RESET,
                boardMemberSub,
                "BOARD",
                "StaffProfile",
                profile.getSub().value().toString(),
                Outcome.SUCCESS,
                window.resetAt()));
        return new Result(profile.getSub(), window, revoked);
    }
}
