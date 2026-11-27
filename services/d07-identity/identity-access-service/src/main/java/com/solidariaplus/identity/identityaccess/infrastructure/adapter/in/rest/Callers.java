package com.solidariaplus.identity.identityaccess.infrastructure.adapter.in.rest;

import com.solidariaplus.identity.identityaccess.domain.exception.DomainException;
import com.solidariaplus.identity.identityaccess.domain.exception.ErrorCode;
import com.solidariaplus.identity.identityaccess.domain.model.vo.SessionId;
import com.solidariaplus.identity.identityaccess.domain.model.vo.SubjectId;
import java.util.List;
import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;

final class Callers {
    private Callers() {}

    static SubjectId subject(Jwt jwt) {
        return new SubjectId(UUID.fromString(jwt.getSubject()));
    }

    static SessionId session(Jwt jwt) {
        return new SessionId(UUID.fromString(jwt.getClaimAsString("sid")));
    }

    static void requireSecondFactor(Jwt jwt) {
        List<String> methods = jwt.getClaimAsStringList("amr");
        if (methods == null || !methods.contains("mfa")) {
            throw new DomainException(ErrorCode.AUTH_MFA_REQUIRED);
        }
    }
}
