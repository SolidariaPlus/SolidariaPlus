package com.solidariaplus.identity.identityaccess.application.service;

import com.solidariaplus.identity.identityaccess.application.port.in.RevokeSessionsUseCase;
import com.solidariaplus.identity.identityaccess.application.port.in.SessionTokenUseCase;
import com.solidariaplus.identity.identityaccess.application.port.output.DirectoryGateway;
import com.solidariaplus.identity.identityaccess.application.port.output.DirectoryGateway.DirectoryIdentity;
import com.solidariaplus.identity.identityaccess.application.port.output.EventPublisher;
import com.solidariaplus.identity.identityaccess.domain.event.AuditRecorded;
import com.solidariaplus.identity.identityaccess.domain.event.AuditRecorded.Action;
import com.solidariaplus.identity.identityaccess.domain.event.AuditRecorded.Outcome;
import com.solidariaplus.identity.identityaccess.domain.event.SessionRevoked;
import com.solidariaplus.identity.identityaccess.domain.event.SessionRevoked.Reason;
import com.solidariaplus.identity.identityaccess.domain.model.aggregate.Session;
import com.solidariaplus.identity.identityaccess.domain.model.aggregate.StaffProfile;
import com.solidariaplus.identity.identityaccess.domain.model.vo.SessionId;
import com.solidariaplus.identity.identityaccess.domain.model.vo.SubjectId;
import com.solidariaplus.identity.identityaccess.domain.model.vo.TokenHash;
import com.solidariaplus.identity.identityaccess.domain.repository.SessionRepository;
import com.solidariaplus.identity.identityaccess.domain.repository.StaffProfileRepository;
import com.solidariaplus.identity.identityaccess.domain.service.SessionPolicy;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SessionRevocationService implements RevokeSessionsUseCase, SessionTokenUseCase {
    private final SessionRepository sessions;
    private final StaffProfileRepository staffProfiles;
    private final DirectoryGateway directory;
    private final EventPublisher events;
    private final Clock clock;

    public SessionRevocationService(
            SessionRepository sessions,
            StaffProfileRepository staffProfiles,
            DirectoryGateway directory,
            EventPublisher events,
            Clock clock) {
        this.sessions = sessions;
        this.staffProfiles = staffProfiles;
        this.directory = directory;
        this.events = events;
        this.clock = clock;
    }

    @Override
    public void recordIssued(SessionId sessionId, SubjectId sub, String issuedToken) {
        TokenHash issued = TokenHash.of(issuedToken);
        Session session =
                sessions.findById(sessionId).orElseGet(() -> Session.start(sessionId, sub, issued, clock.instant()));
        session.rotateRefreshToken(issued);
        sessions.save(session);
    }

    @Override
    public boolean accept(SessionId sessionId, String presentedToken) {
        Instant now = clock.instant();
        return sessions.findById(sessionId)
                .filter(session -> accept(session, TokenHash.of(presentedToken), now))
                .isPresent();
    }

    @Override
    public void revokeSession(SessionId sessionId, Reason reason) {
        sessions.findById(sessionId).ifPresent(session -> revoke(session, reason, clock.instant()));
    }

    @Override
    public int revokeAllSessions(SubjectId sub, Reason reason) {
        Instant now = clock.instant();
        List<Session> active = sessions.findActiveBySubject(sub);
        active.forEach(session -> {
            session.revoke(now);
            sessions.save(session);
        });
        publish(SessionRevoked.ofAllSessions(sub, reason, now));
        return active.size();
    }

    private boolean accept(Session session, TokenHash presented, Instant now) {
        if (session.isReuse(presented)) {
            revoke(session, Reason.REFRESH_TOKEN_REUSE, now);
            return false;
        }
        Optional<StaffProfile> staff = staffProfiles.findBySub(session.getSub());
        return session.isActive(now, SessionPolicy.maxLifetime(staff.isEmpty()))
                && staff.map(profile -> directoryAllows(profile, session, now)).orElse(true);
    }

    private boolean directoryAllows(StaffProfile profile, Session session, Instant now) {
        Optional<DirectoryIdentity> identity = directory.lookup(profile.getDirectoryAccount());
        identity.ifPresentOrElse(
                found -> {
                    profile.syncDirectory(found.roles(), found.functions());
                    staffProfiles.save(profile);
                },
                () -> revoke(session, Reason.DIRECTORY_ACCOUNT_DISABLED, now));
        return identity.isPresent();
    }

    private void revoke(Session session, Reason reason, Instant now) {
        if (session.isRevoked()) {
            return;
        }
        session.revoke(now);
        sessions.save(session);
        publish(SessionRevoked.ofSession(session.getSub(), session.getSessionId(), reason, now));
    }

    private void publish(SessionRevoked revoked) {
        events.publish(revoked);
        events.publish(AuditRecorded.of(
                Action.SESSION_REVOKED,
                revoked.subject(),
                "SYSTEM",
                "Session",
                revoked.subject().value().toString(),
                Outcome.SUCCESS,
                revoked.occurredAt()));
    }
}
