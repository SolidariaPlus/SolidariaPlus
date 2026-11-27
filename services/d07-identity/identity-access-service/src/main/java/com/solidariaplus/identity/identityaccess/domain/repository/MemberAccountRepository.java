package com.solidariaplus.identity.identityaccess.domain.repository;

import com.solidariaplus.identity.identityaccess.domain.model.aggregate.MemberAccount;
import com.solidariaplus.identity.identityaccess.domain.model.vo.MemberId;
import com.solidariaplus.identity.identityaccess.domain.model.vo.MemberNumber;
import java.util.Optional;

public interface MemberAccountRepository {
    void save(MemberAccount account);

    Optional<MemberAccount> findById(MemberId memberId);

    Optional<MemberAccount> findByMemberNumber(MemberNumber memberNumber);
}
