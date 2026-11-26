package com.solidariaplus.identity.identityaccess.domain.repository;

import com.solidariaplus.identity.identityaccess.domain.model.aggregate.StaffProfile;
import com.solidariaplus.identity.identityaccess.domain.model.vo.DirectoryAccount;
import java.util.Optional;

public interface StaffProfileRepository {
    void save(StaffProfile profile);

    Optional<StaffProfile> findByDirectoryAccount(DirectoryAccount directoryAccount);
}
