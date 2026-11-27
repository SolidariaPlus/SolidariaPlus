package com.solidariaplus.identity.identityaccess.infrastructure.adapter.in.web;

import com.solidariaplus.identity.identityaccess.application.port.in.SignInRateLimitUseCase;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

/** Answers 429 with the RateLimitedPage of the contract when an address submits too many sign-in forms. */
final class SignInRateLimitFilter extends OncePerRequestFilter {
    private static final Set<String> SIGN_IN_FORMS = Set.of("/login", "/activate", "/mfa");
    private static final String PAGE = """
            <!doctype html><html lang="es"><head><meta charset="utf-8">\
            <title>Demasiadas solicitudes · Solidaria+</title></head><body><main>\
            <h1>Demasiadas solicitudes</h1><p>Espere un momento e intente de nuevo.</p></main></body></html>""";

    private final SignInRateLimitUseCase rateLimit;

    SignInRateLimitFilter(SignInRateLimitUseCase rateLimit) {
        this.rateLimit = rateLimit;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !HttpMethod.POST.matches(request.getMethod()) || !SIGN_IN_FORMS.contains(request.getServletPath());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Optional<Duration> wait = rateLimit.register(request.getRemoteAddr());
        if (wait.isEmpty()) {
            chain.doFilter(request, response);
            return;
        }
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSeconds(wait.get())));
        response.setContentType(MediaType.TEXT_HTML_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(PAGE);
    }

    private static long retryAfterSeconds(Duration wait) {
        // Retry-After is a whole number of seconds and at least 1 (contract header RetryAfter).
        return Math.max(1, wait.plusMillis(999).toSeconds());
    }
}
