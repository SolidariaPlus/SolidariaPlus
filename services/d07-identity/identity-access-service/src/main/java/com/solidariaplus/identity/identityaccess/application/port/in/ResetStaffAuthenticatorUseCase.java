package com.solidariaplus.identity.identityaccess.application.port.in;

import com.solidariaplus.identity.identityaccess.domain.model.vo.DirectoryAccount;
import com.solidariaplus.identity.identityaccess.domain.model.vo.MfaResetWindow;
import com.solidariaplus.identity.identityaccess.domain.model.vo.SubjectId;

public interface ResetStaffAuthenticatorUseCase {
    Result reset(SubjectId boardMemberSub, DirectoryAccount target);

    record Result(SubjectId staffSub, MfaResetWindow window, int sessionsRevoked) {}
}
