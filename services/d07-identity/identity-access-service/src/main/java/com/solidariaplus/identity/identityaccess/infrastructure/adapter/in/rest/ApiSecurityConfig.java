package com.solidariaplus.identity.identityaccess.infrastructure.adapter.in.rest;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authorization.AuthorityAuthorizationManager;
import org.springframework.security.authorization.AuthorizationManagers;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration(proxyBeanMethods = false)
class ApiSecurityConfig {

    @Bean
    @Order(2)
    SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher("/members/**", "/staff/**", "/sessions/**")
                .authorizeHttpRequests(requests -> requests.requestMatchers(
                                HttpMethod.POST, "/members/*/activation-codes", "/members/by-number/*/activation-codes")
                        .hasRole("TELLER")
                        .requestMatchers(HttpMethod.POST, "/staff/authenticator-resets")
                        .access(AuthorizationManagers.allOf(
                                AuthorityAuthorizationManager.hasRole("BOARD"),
                                AuthorityAuthorizationManager.hasAnyAuthority("FN_PRESIDENT", "FN_COMPLIANCE")))
                        .anyRequest()
                        .authenticated())
                .oauth2ResourceServer(server -> server.jwt(jwt -> jwt.jwtAuthenticationConverter(authentication())))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(AbstractHttpConfigurer::disable);
        return http.build();
    }

    private static JwtAuthenticationConverter authentication() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(ApiSecurityConfig::authorities);
        return converter;
    }

    private static Collection<GrantedAuthority> authorities(Jwt jwt) {
        return Stream.concat(claim(jwt, "roles", "ROLE_"), claim(jwt, "fn", "FN_"))
                .<GrantedAuthority>map(SimpleGrantedAuthority::new)
                .toList();
    }

    private static Stream<String> claim(Jwt jwt, String name, String prefix) {
        List<String> values = jwt.getClaimAsStringList(name);
        return values == null ? Stream.empty() : values.stream().map(value -> prefix + value);
    }
}
