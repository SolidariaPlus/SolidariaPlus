package com.solidariaplus.identity.identityaccess.domain.repository;

import com.solidariaplus.identity.identityaccess.domain.model.aggregate.StaffProfile;
import com.solidariaplus.identity.identityaccess.domain.model.vo.DirectoryAccount;
import com.solidariaplus.identity.identityaccess.domain.model.vo.SubjectId;
import java.util.Optional;

public interface StaffProfileRepository {
    void save(StaffProfile profile);

    Optional<StaffProfile> findBySub(SubjectId sub);

    Optional<StaffProfile> findByDirectoryAccount(DirectoryAccount directoryAccount);
}
