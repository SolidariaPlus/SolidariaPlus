package com.solidariaplus.identity.identityaccess.application.port.in;

import com.solidariaplus.identity.identityaccess.domain.model.vo.MemberId;
import java.util.UUID;

public interface EndMembershipUseCase {
    void end(Command command);

    record Command(UUID eventId, MemberId memberId) {}
}
