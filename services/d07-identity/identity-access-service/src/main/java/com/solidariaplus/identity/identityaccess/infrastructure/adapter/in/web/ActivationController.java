package com.solidariaplus.identity.identityaccess.infrastructure.adapter.in.web;

import com.solidariaplus.identity.identityaccess.application.port.in.ActivateAccountUseCase;
import com.solidariaplus.identity.identityaccess.domain.exception.DomainException;
import com.solidariaplus.identity.identityaccess.domain.exception.ErrorCode;
import com.solidariaplus.identity.identityaccess.domain.model.vo.MemberNumber;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
class ActivationController {
    private static final String VIEW = "activate";

    private final ActivateAccountUseCase activation;

    ActivationController(ActivateAccountUseCase activation) {
        this.activation = activation;
    }

    @GetMapping("/activate")
    String page() {
        return VIEW;
    }

    @PostMapping("/activate")
    String activate(
            @RequestParam String memberNumber,
            @RequestParam String activationCode,
            @RequestParam String newPassword,
            HttpServletResponse response,
            Model model) {
        try {
            activation.activate(new ActivateAccountUseCase.Command(memberNumber, activationCode, newPassword));
            return "redirect:/login?activated";
        } catch (DomainException e) {
            boolean weakPassword = e.getCode() == ErrorCode.VALIDATION_FAILED;
            response.setStatus(
                    weakPassword ? HttpStatus.UNPROCESSABLE_CONTENT.value() : HttpStatus.BAD_REQUEST.value());
            model.addAttribute("error", e.getCode().name());
            if (MemberNumber.isMemberNumber(memberNumber)) {
                model.addAttribute("memberNumber", memberNumber);
            }
            return VIEW;
        }
    }
}
