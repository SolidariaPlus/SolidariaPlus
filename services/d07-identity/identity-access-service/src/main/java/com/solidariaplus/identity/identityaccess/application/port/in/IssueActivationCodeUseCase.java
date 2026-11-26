package com.solidariaplus.identity.identityaccess.application.port.in;

import com.solidariaplus.identity.identityaccess.domain.model.vo.MemberId;
import com.solidariaplus.identity.identityaccess.domain.model.vo.SubjectId;
import java.time.Instant;
import java.util.UUID;

public interface IssueActivationCodeUseCase {
    IssuedCode issue(MemberId memberId, SubjectId tellerSub);

    record IssuedCode(UUID activationCodeId, MemberId memberId, String code, Instant issuedAt, Instant expiresAt) {}
}
