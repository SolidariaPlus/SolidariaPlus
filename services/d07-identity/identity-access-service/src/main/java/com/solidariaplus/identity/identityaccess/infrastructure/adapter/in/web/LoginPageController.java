package com.solidariaplus.identity.identityaccess.infrastructure.adapter.in.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
class LoginPageController {
    static final String LOCKED = "locked";
    static final String CREDENTIALS = "credentials";

    @GetMapping("/login")
    String login(HttpServletRequest request, Model model) {
        boolean failed = request.getParameterMap().containsKey("error");
        String error = failed ? (LOCKED.equals(request.getParameter("error")) ? LOCKED : CREDENTIALS) : null;
        model.addAttribute("loginError", error);
        model.addAttribute("activated", request.getParameterMap().containsKey("activated"));
        return "login";
    }
}
