package com.solidariaplus.identity.identityaccess.infrastructure.adapter.in.rest;

import com.solidariaplus.identity.identityaccess.application.port.in.IssueActivationCodeUseCase;
import com.solidariaplus.identity.identityaccess.application.port.in.IssueActivationCodeUseCase.IssuedCode;
import com.solidariaplus.identity.identityaccess.domain.model.vo.MemberId;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class ActivationCodeController {
    private final IssueActivationCodeUseCase activationCodes;

    ActivationCodeController(IssueActivationCodeUseCase activationCodes) {
        this.activationCodes = activationCodes;
    }

    @PostMapping("/members/{memberId}/activation-codes")
    ResponseEntity<IssuedCode> issue(@PathVariable UUID memberId, @AuthenticationPrincipal Jwt caller) {
        IssuedCode issued = activationCodes.issue(new MemberId(memberId), Callers.subject(caller));
        return ResponseEntity.created(
                        URI.create("/members/" + memberId + "/activation-codes/" + issued.activationCodeId()))
                .cacheControl(CacheControl.noStore())
                .body(issued);
    }
}
