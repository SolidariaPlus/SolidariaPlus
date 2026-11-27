package com.solidariaplus.identity.identityaccess.infrastructure.adapter.in.web;

import com.solidariaplus.identity.identityaccess.application.port.in.SecondFactorUseCase;
import com.solidariaplus.identity.identityaccess.domain.exception.DomainException;
import com.solidariaplus.identity.identityaccess.domain.exception.ErrorCode;
import com.solidariaplus.identity.identityaccess.domain.model.vo.SecondFactor;
import com.solidariaplus.identity.identityaccess.infrastructure.adapter.in.web.SignInSession.Pending;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/mfa")
class MfaController {
    private static final String VIEW = "mfa";
    private static final String SIGN_IN_AGAIN = "redirect:/login";

    private final SecondFactorUseCase secondFactor;
    private final SecondFactorSuccessHandler signIn;

    MfaController(SecondFactorUseCase secondFactor, SecondFactorSuccessHandler signIn) {
        this.secondFactor = secondFactor;
        this.signIn = signIn;
    }

    @GetMapping
    String page(HttpServletRequest request, Model model) {
        return SignInSession.pending(request)
                .map(pending -> form(request, pending, model))
                .orElse(SIGN_IN_AGAIN);
    }

    @PostMapping
    String submit(@RequestParam String otp, HttpServletRequest request, HttpServletResponse response, Model model) {
        Optional<Pending> found = SignInSession.pending(request);
        if (found.isEmpty()) {
            return SIGN_IN_AGAIN;
        }
        Pending pending = found.get();
        try {
            check(request, pending, otp);
        } catch (DomainException e) {
            boolean locked = e.getCode() == ErrorCode.AUTH_ACCOUNT_LOCKED;
            response.setStatus(locked ? HttpStatus.LOCKED.value() : HttpStatus.UNAUTHORIZED.value());
            model.addAttribute("error", e.getCode().name());
            return form(request, pending, model);
        }
        SignInSession.finish(request.getSession());
        return "redirect:" + signIn.complete(request, response, pending.authentication());
    }

    private void check(HttpServletRequest request, Pending pending, String otp) {
        if (pending.secondFactor() == SecondFactor.ENROL) {
            secondFactor.enrol(pending.sub(), enrolmentSecret(request), otp);
        } else {
            secondFactor.verify(pending.sub(), otp);
        }
    }

    private String form(HttpServletRequest request, Pending pending, Model model) {
        boolean enrol = pending.secondFactor() == SecondFactor.ENROL;
        model.addAttribute("enrol", enrol);
        if (enrol) {
            String secret = enrolmentSecret(request);
            model.addAttribute("secret", secret);
            model.addAttribute("otpauthUri", "otpauth://totp/Solidaria%2B?secret=" + secret + "&issuer=Solidaria%2B");
        }
        return VIEW;
    }

    private String enrolmentSecret(HttpServletRequest request) {
        return SignInSession.enrolmentSecret(request.getSession(), secondFactor::newSecret);
    }
}
