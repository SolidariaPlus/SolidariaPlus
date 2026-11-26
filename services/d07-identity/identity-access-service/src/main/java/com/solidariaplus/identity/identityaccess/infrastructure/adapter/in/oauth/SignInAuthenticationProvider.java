package com.solidariaplus.identity.identityaccess.infrastructure.adapter.in.oauth;

import com.solidariaplus.identity.identityaccess.application.port.in.SignInUseCase;
import com.solidariaplus.identity.identityaccess.application.port.in.SignInUseCase.AuthenticatedSubject;
import com.solidariaplus.identity.identityaccess.domain.exception.DomainException;
import com.solidariaplus.identity.identityaccess.domain.exception.ErrorCode;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

@Component
class SignInAuthenticationProvider implements AuthenticationProvider {
    static final String ROLE_PREFIX = "ROLE_";
    static final String FUNCTION_PREFIX = "FN_";

    private final SignInUseCase signIn;

    SignInAuthenticationProvider(SignInUseCase signIn) {
        this.signIn = signIn;
    }

    @Override
    public Authentication authenticate(Authentication authentication) {
        try {
            AuthenticatedSubject subject =
                    signIn.signIn(authentication.getName(), String.valueOf(authentication.getCredentials()));
            return UsernamePasswordAuthenticationToken.authenticated(
                    subject.sub().value().toString(), null, authorities(subject));
        } catch (DomainException e) {
            throw e.getCode() == ErrorCode.AUTH_ACCOUNT_LOCKED
                    ? new LockedException(e.getMessage())
                    : new BadCredentialsException(e.getMessage());
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }

    private static List<GrantedAuthority> authorities(AuthenticatedSubject subject) {
        return Stream.concat(
                        subject.roles().stream().map(role -> ROLE_PREFIX + role),
                        subject.functions().stream().map(function -> FUNCTION_PREFIX + function))
                .<GrantedAuthority>map(SimpleGrantedAuthority::new)
                .toList();
    }
}
