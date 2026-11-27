package com.solidariaplus.identity.identityaccess.infrastructure.adapter.in.web;

import com.solidariaplus.identity.identityaccess.application.port.in.SecondFactorUseCase;
import com.solidariaplus.identity.identityaccess.domain.model.vo.SecondFactor;
import com.solidariaplus.identity.identityaccess.domain.model.vo.SubjectId;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.FactorGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Component;

@Component
class SecondFactorSuccessHandler implements AuthenticationSuccessHandler {
    private final SecondFactorUseCase secondFactor;
    private final AuthenticationSuccessHandler withoutSecondFactor =
            new SavedRequestAwareAuthenticationSuccessHandler();
    private final SecurityContextRepository contexts = new HttpSessionSecurityContextRepository();
    private final RequestCache requests = new HttpSessionRequestCache();
    private final RedirectStrategy redirects = new DefaultRedirectStrategy();

    SecondFactorSuccessHandler(SecondFactorUseCase secondFactor) {
        this.secondFactor = secondFactor;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {
        SecondFactor required = secondFactor.requirementOf(new SubjectId(UUID.fromString(authentication.getName())));
        if (required == SecondFactor.NONE) {
            withoutSecondFactor.onAuthenticationSuccess(request, response, authentication);
            return;
        }
        SignInSession.begin(request.getSession(), authentication, required);
        SecurityContextHolder.clearContext();
        contexts.saveContext(SecurityContextHolder.createEmptyContext(), request, response);
        redirects.sendRedirect(request, response, "/mfa");
    }

    String complete(HttpServletRequest request, HttpServletResponse response, Authentication passwordStep) {
        // Keeps the password factor and adds the TOTP factor; the ID token auth_time is the latest of both.
        List<GrantedAuthority> factors = Stream.concat(
                        passwordStep.getAuthorities().stream(),
                        Stream.of(FactorGrantedAuthority.fromAuthority(SignInSession.SECOND_FACTOR_AUTHORITY)))
                .toList();
        Authentication authenticated =
                UsernamePasswordAuthenticationToken.authenticated(passwordStep.getName(), null, factors);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authenticated);
        SecurityContextHolder.setContext(context);
        contexts.saveContext(context, request, response);
        String target = Optional.ofNullable(requests.getRequest(request, response))
                .map(SavedRequest::getRedirectUrl)
                .orElse("/");
        requests.removeRequest(request, response);
        return target;
    }
}
