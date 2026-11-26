package com.solidariaplus.identity.identityaccess.infrastructure.adapter.output.directory;

import com.solidariaplus.identity.identityaccess.application.port.output.DirectoryGateway;
import com.solidariaplus.identity.identityaccess.domain.model.vo.DirectoryAccount;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
class LocalDirectoryGateway implements DirectoryGateway {
    private final DirectoryProperties properties;

    LocalDirectoryGateway(DirectoryProperties properties) {
        this.properties = properties;
    }

    @Override
    public Optional<DirectoryIdentity> authenticate(String username, String password) {
        boolean valid = MessageDigest.isEqual(
                properties.localPassword().getBytes(StandardCharsets.UTF_8), password.getBytes(StandardCharsets.UTF_8));
        return valid ? lookup(new DirectoryAccount(username)) : Optional.empty();
    }

    @Override
    public Optional<DirectoryIdentity> lookup(DirectoryAccount account) {
        return Optional.ofNullable(properties.localAccounts().get(account.username()))
                .map(groups -> properties.identity(account, groups));
    }
}
