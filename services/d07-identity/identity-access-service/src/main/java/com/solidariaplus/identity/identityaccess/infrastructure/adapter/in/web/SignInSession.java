package com.solidariaplus.identity.identityaccess.infrastructure.adapter.in.web;

import com.solidariaplus.identity.identityaccess.domain.model.vo.SecondFactor;
import com.solidariaplus.identity.identityaccess.domain.model.vo.SubjectId;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.security.core.Authentication;

public final class SignInSession {
    public static final String SECOND_FACTOR_AUTHORITY = "FACTOR_OTP";
    private static final String PENDING = SignInSession.class.getName() + ".pending";
    private static final String ENROLMENT_SECRET = SignInSession.class.getName() + ".secret";

    private SignInSession() {}

    record Pending(Authentication authentication, SecondFactor secondFactor) {
        SubjectId sub() {
            return new SubjectId(UUID.fromString(authentication.getName()));
        }
    }

    static void begin(HttpSession session, Authentication authentication, SecondFactor secondFactor) {
        session.setAttribute(PENDING, new Pending(authentication, secondFactor));
    }

    static Optional<Pending> pending(HttpServletRequest request) {
        return Optional.ofNullable(request.getSession(false)).map(session -> (Pending) session.getAttribute(PENDING));
    }

    static String enrolmentSecret(HttpSession session, Supplier<String> newSecret) {
        String secret = (String) session.getAttribute(ENROLMENT_SECRET);
        if (secret == null) {
            secret = newSecret.get();
            session.setAttribute(ENROLMENT_SECRET, secret);
        }
        return secret;
    }

    static void finish(HttpSession session) {
        session.removeAttribute(PENDING);
        session.removeAttribute(ENROLMENT_SECRET);
    }
}
