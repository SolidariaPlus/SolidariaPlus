package com.solidariaplus.identity.identityaccess.infrastructure.adapter.output.directory;

import com.solidariaplus.identity.identityaccess.application.port.output.DirectoryGateway.DirectoryIdentity;
import com.solidariaplus.identity.identityaccess.domain.model.vo.DirectoryAccount;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("solidaria.directory")
record DirectoryProperties(Map<String, Grant> groups, Map<String, List<String>> localAccounts, String localPassword) {

    record Grant(String role, String function) {}

    DirectoryIdentity identity(DirectoryAccount account, Collection<String> memberOf) {
        List<Grant> grants =
                memberOf.stream().map(groups::get).filter(Objects::nonNull).toList();
        return new DirectoryIdentity(account, collect(grants, Grant::role), collect(grants, Grant::function));
    }

    private static Set<String> collect(List<Grant> grants, Function<Grant, String> field) {
        return grants.stream().map(field).filter(Objects::nonNull).collect(Collectors.toUnmodifiableSet());
    }
}
