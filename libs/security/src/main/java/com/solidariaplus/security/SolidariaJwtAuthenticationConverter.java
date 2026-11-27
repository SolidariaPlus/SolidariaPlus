package com.solidariaplus.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Converts the {@code roles} and {@code fn} JWT claims into Spring Security {@link GrantedAuthority}
 * instances so resource servers can use {@code hasAuthority("ROLE_TELLER")} / {@code hasAuthority("FN_PRESIDENT")}.
 */
public class SolidariaJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = extractAuthorities(jwt);
        return new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
    }

    private static Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        List<GrantedAuthority> authorities = new ArrayList<>();

        List<String> roles = jwt.getClaimAsStringList("roles");
        if (roles != null) {
            roles.forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role)));
        }

        List<String> functions = jwt.getClaimAsStringList("fn");
        if (functions != null) {
            functions.forEach(fn -> authorities.add(new SimpleGrantedAuthority("FN_" + fn)));
        }

        return authorities;
    }
}
