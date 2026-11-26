package com.solidariaplus.identity.identityaccess.domain.repository;

import com.solidariaplus.identity.identityaccess.domain.model.aggregate.Session;
import com.solidariaplus.identity.identityaccess.domain.model.vo.SessionId;
import com.solidariaplus.identity.identityaccess.domain.model.vo.SubjectId;
import java.util.List;
import java.util.Optional;

public interface SessionRepository {
    void save(Session session);

    Optional<Session> findById(SessionId sessionId);

    List<Session> findActiveBySubject(SubjectId sub);
}
