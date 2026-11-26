package com.solidariaplus.identity.identityaccess.infrastructure.adapter.in.oauth;

import com.solidariaplus.identity.identityaccess.application.port.in.SignInUseCase;
import com.solidariaplus.identity.identityaccess.domain.exception.DomainException;
import com.solidariaplus.identity.identityaccess.domain.exception.ErrorCode;
import com.solidariaplus.identity.identityaccess.domain.model.vo.SubjectId;
import java.util.List;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.FactorGrantedAuthority;
import org.springframework.stereotype.Component;

@Component
class SignInAuthenticationProvider implements AuthenticationProvider {
    private final SignInUseCase signIn;

    SignInAuthenticationProvider(SignInUseCase signIn) {
        this.signIn = signIn;
    }

    @Override
    public Authentication authenticate(Authentication authentication) {
        try {
            SubjectId sub = signIn.signIn(authentication.getName(), String.valueOf(authentication.getCredentials()));
            // Spring Security 7 derives the ID token auth_time from the factor authorities of the principal.
            return UsernamePasswordAuthenticationToken.authenticated(
                    sub.value().toString(),
                    null,
                    List.of(FactorGrantedAuthority.fromAuthority(FactorGrantedAuthority.PASSWORD_AUTHORITY)));
        } catch (DomainException e) {
            throw e.getCode() == ErrorCode.AUTH_ACCOUNT_LOCKED
                    ? new LockedException(e.getCode().name())
                    : new BadCredentialsException(e.getCode().name());
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
