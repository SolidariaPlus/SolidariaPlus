package com.solidariaplus.identity.identityaccess.application.port.in;

import com.solidariaplus.identity.identityaccess.domain.model.vo.SessionId;

public interface SessionStatusUseCase {
    boolean isRevoked(SessionId sessionId);
}
