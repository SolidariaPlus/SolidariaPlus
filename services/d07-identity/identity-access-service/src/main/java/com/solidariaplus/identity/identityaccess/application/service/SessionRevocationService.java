package com.solidariaplus.identity.identityaccess.application.service;

import com.solidariaplus.identity.identityaccess.application.port.in.RevokeSessionsUseCase;
import com.solidariaplus.identity.identityaccess.application.port.out.EventPublisher;
import com.solidariaplus.identity.identityaccess.domain.event.SessionRevoked;
import com.solidariaplus.identity.identityaccess.domain.event.SessionRevoked.Reason;
import com.solidariaplus.identity.identityaccess.domain.model.aggregate.Session;
import com.solidariaplus.identity.identityaccess.domain.model.vo.SessionId;
import com.solidariaplus.identity.identityaccess.domain.model.vo.SubjectId;
import com.solidariaplus.identity.identityaccess.domain.repository.SessionRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SessionRevocationService implements RevokeSessionsUseCase {
    private final SessionRepository sessions;
    private final EventPublisher events;
    private final Clock clock;

    public SessionRevocationService(SessionRepository sessions, EventPublisher events, Clock clock) {
        this.sessions = sessions;
        this.events = events;
        this.clock = clock;
    }

    @Override
    public void revokeSession(SessionId sessionId, Reason reason) {
        Instant now = clock.instant();
        sessions.findById(sessionId).filter(session -> !session.isRevoked()).ifPresent(session -> {
            revoke(session, now);
            events.publish(SessionRevoked.ofSession(session.getSub(), sessionId, reason, now));
        });
    }

    @Override
    public int revokeAllSessions(SubjectId sub, Reason reason) {
        Instant now = clock.instant();
        List<Session> active = sessions.findActiveBySubject(sub);
        active.forEach(session -> revoke(session, now));
        events.publish(SessionRevoked.ofAllSessions(sub, reason, now));
        return active.size();
    }

    private void revoke(Session session, Instant now) {
        session.revoke(now);
        sessions.save(session);
    }
}
