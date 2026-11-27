package com.solidariaplus.identity.identityaccess.infrastructure.adapter.in.rest;

import com.solidariaplus.identity.identityaccess.application.port.in.RevokeSessionsUseCase;
import com.solidariaplus.identity.identityaccess.domain.event.SessionRevoked.Reason;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class SessionController {
    private final RevokeSessionsUseCase sessions;

    SessionController(RevokeSessionsUseCase sessions) {
        this.sessions = sessions;
    }

    @DeleteMapping("/sessions/current")
    ResponseEntity<Void> endCurrentSession(@AuthenticationPrincipal Jwt caller) {
        sessions.revokeSession(Callers.session(caller), Reason.SIGN_OUT);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/sessions")
    ResponseEntity<Void> endAllSessions(@AuthenticationPrincipal Jwt caller) {
        sessions.revokeAllSessions(Callers.subject(caller), Reason.SIGN_OUT);
        return ResponseEntity.noContent().build();
    }
}
