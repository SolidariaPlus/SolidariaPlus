package com.solidariaplus.identity.identityaccess.infrastructure.adapter.in.oauth;

import com.solidariaplus.identity.identityaccess.application.port.in.TokenClaimsUseCase;
import com.solidariaplus.identity.identityaccess.application.port.in.TokenClaimsUseCase.Claims;
import com.solidariaplus.identity.identityaccess.domain.model.vo.SubjectId;
import com.solidariaplus.identity.identityaccess.infrastructure.adapter.in.web.SignInSession;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.stereotype.Component;

@Component
class AccessTokenClaimsCustomizer implements OAuth2TokenCustomizer<JwtEncodingContext> {
    private static final String AUDIENCE = "solidaria-api";

    private final TokenClaimsUseCase tokenClaims;

    AccessTokenClaimsCustomizer(TokenClaimsUseCase tokenClaims) {
        this.tokenClaims = tokenClaims;
    }

    @Override
    public void customize(JwtEncodingContext context) {
        context.getJwsHeader().algorithm(SignatureAlgorithm.ES256);
        if (!OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType())) {
            return;
        }
        JwtClaimsSet.Builder claims = context.getClaims().audience(List.of(AUDIENCE));
        if (AuthorizationGrantType.CLIENT_CREDENTIALS.equals(context.getAuthorizationGrantType())) {
            claims.claim("roles", List.of("SERVICE")).claim("amr", List.of("client_secret"));
            return;
        }
        Authentication principal = context.getPrincipal();
        Claims subject = tokenClaims.claimsOf(new SubjectId(UUID.fromString(principal.getName())));
        claims.claim("roles", List.copyOf(subject.roles()))
                .claim("amr", authenticationMethods(principal))
                .claim("sid", context.getAuthorization().getId());
        if (!subject.functions().isEmpty()) {
            claims.claim("fn", List.copyOf(subject.functions()));
        }
        subject.memberId()
                .ifPresent(memberId -> claims.claim("mid", memberId.value().toString()));
    }

    private static List<String> authenticationMethods(Authentication principal) {
        boolean secondFactor = principal.getAuthorities().stream()
                .anyMatch(authority -> SignInSession.SECOND_FACTOR_AUTHORITY.equals(authority.getAuthority()));
        return secondFactor ? List.of("pwd", "otp", "mfa") : List.of("pwd");
    }
}
