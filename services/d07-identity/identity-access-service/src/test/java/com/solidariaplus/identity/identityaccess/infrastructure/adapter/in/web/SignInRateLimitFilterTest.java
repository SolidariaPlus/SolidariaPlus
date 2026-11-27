package com.solidariaplus.identity.identityaccess.infrastructure.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class SignInRateLimitFilterTest {
    private final List<String> counted = new ArrayList<>();

    @Test
    void limitedAddressGetsTheWaitPage() throws Exception {
        MockHttpServletResponse response = filter(Optional.of(Duration.ofMillis(1_200)), "POST", "/login");

        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getHeader("Retry-After")).isEqualTo("2");
        assertThat(response.getContentAsString()).contains("Demasiadas solicitudes");
        assertThat(counted).containsExactly("203.0.113.7");
    }

    @Test
    void retryAfterIsAtLeastOneSecond() throws Exception {
        MockHttpServletResponse response = filter(Optional.of(Duration.ZERO), "POST", "/mfa");

        assertThat(response.getHeader("Retry-After")).isEqualTo("1");
    }

    @Test
    void addressWithinTheLimitContinues() throws Exception {
        MockHttpServletResponse response = filter(Optional.empty(), "POST", "/activate");

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(counted).hasSize(1);
    }

    @Test
    void onlySignInFormsAreCounted() throws Exception {
        filter(Optional.of(Duration.ofSeconds(5)), "GET", "/login");
        MockHttpServletResponse response = filter(Optional.of(Duration.ofSeconds(5)), "POST", "/oauth2/token");

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(counted).isEmpty();
    }

    private MockHttpServletResponse filter(Optional<Duration> wait, String method, String path) throws Exception {
        SignInRateLimitFilter filter = new SignInRateLimitFilter(address -> {
            counted.add(address);
            return wait;
        });
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setServletPath(path);
        request.setRemoteAddr("203.0.113.7");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }
}
