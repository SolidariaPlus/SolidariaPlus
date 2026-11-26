package com.solidariaplus.identity.identityaccess.application.port.in;

import com.solidariaplus.identity.identityaccess.domain.model.vo.SessionId;
import com.solidariaplus.identity.identityaccess.domain.model.vo.SubjectId;

public interface SessionTokenUseCase {
    void recordIssued(SessionId sessionId, SubjectId sub, String issuedToken);

    boolean accept(SessionId sessionId, String presentedToken);
}
