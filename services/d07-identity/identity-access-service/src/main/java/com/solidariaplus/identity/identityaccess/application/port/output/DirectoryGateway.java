package com.solidariaplus.identity.identityaccess.application.port.output;

import com.solidariaplus.identity.identityaccess.domain.model.vo.DirectoryAccount;
import java.util.Optional;
import java.util.Set;

public interface DirectoryGateway {
    Optional<DirectoryIdentity> authenticate(String username, String password);

    Optional<DirectoryIdentity> lookup(DirectoryAccount account);

    record DirectoryIdentity(DirectoryAccount account, Set<String> roles, Set<String> functions) {}
}
