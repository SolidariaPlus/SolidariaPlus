package com.solidariaplus.identity.identityaccess.infrastructure.adapter.in.oauth;

import static com.solidariaplus.identity.identityaccess.infrastructure.adapter.in.oauth.SignInAuthenticationProvider.FUNCTION_PREFIX;
import static com.solidariaplus.identity.identityaccess.infrastructure.adapter.in.oauth.SignInAuthenticationProvider.ROLE_PREFIX;

import com.solidariaplus.identity.identityaccess.application.port.in.ResolveMemberIdUseCase;
import com.solidariaplus.identity.identityaccess.domain.model.vo.SubjectId;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.stereotype.Component;

@Component
class AccessTokenClaimsCustomizer implements OAuth2TokenCustomizer<JwtEncodingContext> {
    private static final String AUDIENCE = "solidaria-api";

    private final ResolveMemberIdUseCase members;

    AccessTokenClaimsCustomizer(ResolveMemberIdUseCase members) {
        this.members = members;
    }

    @Override
    public void customize(JwtEncodingContext context) {
        if (!OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType())) {
            return;
        }
        JwtClaimsSet.Builder claims = context.getClaims().audience(List.of(AUDIENCE));
        if (AuthorizationGrantType.CLIENT_CREDENTIALS.equals(context.getAuthorizationGrantType())) {
            claims.claim("roles", List.of()).claim("amr", List.of("client_secret"));
            return;
        }
        Authentication principal = context.getPrincipal();
        claims.claim("roles", authoritiesWithPrefix(principal, ROLE_PREFIX))
                .claim("amr", List.of("pwd"))
                .claim("sid", context.getAuthorization().getId());
        List<String> functions = authoritiesWithPrefix(principal, FUNCTION_PREFIX);
        if (!functions.isEmpty()) {
            claims.claim("fn", functions);
        }
        members.memberIdOf(new SubjectId(UUID.fromString(principal.getName())))
                .ifPresent(memberId -> claims.claim("mid", memberId.value().toString()));
    }

    private static List<String> authoritiesWithPrefix(Authentication principal, String prefix) {
        return principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith(prefix))
                .map(authority -> authority.substring(prefix.length()))
                .toList();
    }
}
