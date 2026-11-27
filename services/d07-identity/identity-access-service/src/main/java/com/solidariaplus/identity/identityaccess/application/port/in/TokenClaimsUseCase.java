package com.solidariaplus.identity.identityaccess.application.port.in;

import com.solidariaplus.identity.identityaccess.domain.model.vo.MemberId;
import com.solidariaplus.identity.identityaccess.domain.model.vo.SubjectId;
import java.util.Optional;
import java.util.Set;

public interface TokenClaimsUseCase {
    Claims claimsOf(SubjectId sub);

    record Claims(Set<String> roles, Set<String> functions, Optional<MemberId> memberId) {}
}
