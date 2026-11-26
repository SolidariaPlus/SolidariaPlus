package com.solidariaplus.identity.identityaccess.application.port.in;

import com.solidariaplus.identity.identityaccess.domain.event.SessionRevoked.Reason;
import com.solidariaplus.identity.identityaccess.domain.model.vo.SessionId;
import com.solidariaplus.identity.identityaccess.domain.model.vo.SubjectId;

public interface RevokeSessionsUseCase {
    void revokeSession(SessionId sessionId, Reason reason);

    int revokeAllSessions(SubjectId sub, Reason reason);
}
