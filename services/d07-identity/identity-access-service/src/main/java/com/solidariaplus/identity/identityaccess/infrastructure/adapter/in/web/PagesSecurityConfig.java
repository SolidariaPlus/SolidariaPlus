package com.solidariaplus.identity.identityaccess.infrastructure.adapter.in.web;

import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.ExceptionMappingAuthenticationFailureHandler;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration(proxyBeanMethods = false)
class PagesSecurityConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/login").setViewName("login");
        registry.addViewController("/activate").setViewName("activate");
    }

    @Bean
    @Order(3)
    SecurityFilterChain pagesFilterChain(HttpSecurity http, SecondFactorSuccessHandler successHandler)
            throws Exception {
        ExceptionMappingAuthenticationFailureHandler failureHandler =
                new ExceptionMappingAuthenticationFailureHandler();
        failureHandler.setDefaultFailureUrl("/login?error=credentials");
        failureHandler.setExceptionMappings(Map.of(LockedException.class.getName(), "/login?error=locked"));
        http.authorizeHttpRequests(requests -> requests.requestMatchers(
                                "/login", "/activate", "/mfa", "/error", "/actuator/health/**", "/actuator/prometheus")
                        .permitAll()
                        .anyRequest()
                        .authenticated())
                .formLogin(form ->
                        form.loginPage("/login").successHandler(successHandler).failureHandler(failureHandler));
        return http.build();
    }
}
