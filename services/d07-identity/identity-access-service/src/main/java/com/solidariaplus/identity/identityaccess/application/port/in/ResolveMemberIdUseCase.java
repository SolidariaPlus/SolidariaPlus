package com.solidariaplus.identity.identityaccess.application.port.in;

import com.solidariaplus.identity.identityaccess.domain.model.vo.MemberId;
import com.solidariaplus.identity.identityaccess.domain.model.vo.SubjectId;
import java.util.Optional;

public interface ResolveMemberIdUseCase {
    Optional<MemberId> memberIdOf(SubjectId sub);
}
