package com.solidariaplus.identity.identityaccess.application.port.in;

import com.solidariaplus.identity.identityaccess.domain.model.vo.MemberId;
import com.solidariaplus.identity.identityaccess.domain.model.vo.MemberNumber;
import java.util.UUID;

public interface RegisterMemberUseCase {
    void register(Command command);

    record Command(UUID eventId, MemberId memberId, MemberNumber memberNumber) {}
}
