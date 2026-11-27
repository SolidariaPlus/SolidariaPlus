package com.solidariaplus.identity.identityaccess.infrastructure.adapter.output.directory;

import com.solidariaplus.identity.identityaccess.application.port.output.DirectoryGateway;
import com.solidariaplus.identity.identityaccess.domain.model.vo.DirectoryAccount;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.ldap.LdapName;
import org.springframework.context.annotation.Profile;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.filter.AndFilter;
import org.springframework.ldap.filter.EqualsFilter;
import org.springframework.ldap.filter.Filter;
import org.springframework.ldap.filter.HardcodedFilter;
import org.springframework.ldap.support.LdapUtils;
import org.springframework.stereotype.Component;

@Component
@Profile("!local")
class LdapDirectoryGateway implements DirectoryGateway {
    private static final Filter ENABLED = new HardcodedFilter("(!(userAccountControl:1.2.840.113556.1.4.803:=2))");

    private final LdapTemplate ldap;
    private final DirectoryProperties properties;

    LdapDirectoryGateway(LdapTemplate ldap, DirectoryProperties properties) {
        this.ldap = ldap;
        this.properties = properties;
    }

    @Override
    public Optional<DirectoryIdentity> authenticate(String username, String password) {
        boolean bound = ldap.authenticate(
                LdapUtils.emptyLdapName(), userFilter(username).encode(), password);
        return bound ? lookup(new DirectoryAccount(username)) : Optional.empty();
    }

    @Override
    public Optional<DirectoryIdentity> lookup(DirectoryAccount account) {
        AttributesMapper<List<String>> groups = attributes -> groupNames(attributes.get("memberOf"));
        return ldap
                .search(
                        LdapUtils.emptyLdapName(),
                        userFilter(account.username()).encode(),
                        groups)
                .stream()
                .findFirst()
                .map(memberOf -> properties.identity(account, memberOf));
    }

    private static Filter userFilter(String username) {
        return new AndFilter().and(new EqualsFilter("sAMAccountName", username)).and(ENABLED);
    }

    private static List<String> groupNames(Attribute memberOf) throws NamingException {
        List<String> names = new ArrayList<>();
        if (memberOf == null) {
            return names;
        }
        NamingEnumeration<?> values = memberOf.getAll();
        while (values.hasMore()) {
            names.add(LdapUtils.getStringValue(new LdapName(values.next().toString()), "cn"));
        }
        return names;
    }
}
