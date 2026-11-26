package com.solidariaplus.identity.identityaccess.infrastructure.adapter.in.rest;

import com.solidariaplus.identity.identityaccess.application.port.in.ResetStaffAuthenticatorUseCase;
import com.solidariaplus.identity.identityaccess.application.port.in.ResetStaffAuthenticatorUseCase.Result;
import com.solidariaplus.identity.identityaccess.domain.model.vo.DirectoryAccount;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
class StaffAuthenticatorController {
    private final ResetStaffAuthenticatorUseCase resets;

    StaffAuthenticatorController(ResetStaffAuthenticatorUseCase resets) {
        this.resets = resets;
    }

    @PostMapping("/staff/authenticator-resets")
    ResponseEntity<AuthenticatorReset> reset(
            @Valid @RequestBody ResetRequest request, @AuthenticationPrincipal Jwt caller) {
        Callers.requireSecondFactor(caller);
        Result result = resets.reset(Callers.subject(caller), new DirectoryAccount(request.directoryAccount()));
        AuthenticatorReset body = new AuthenticatorReset(
                result.staffSub().value(),
                result.window().resetAt(),
                result.window().resetByBoardMember().value(),
                result.window().enrolmentDeadline(),
                result.sessionsRevoked());
        return ResponseEntity.status(HttpStatus.CREATED)
                .cacheControl(CacheControl.noStore())
                .body(body);
    }

    record ResetRequest(@NotBlank String directoryAccount) {}

    record AuthenticatorReset(
            UUID staffSub, Instant resetAt, UUID resetBySub, Instant enrolmentDeadline, int sessionsRevoked) {}
}
