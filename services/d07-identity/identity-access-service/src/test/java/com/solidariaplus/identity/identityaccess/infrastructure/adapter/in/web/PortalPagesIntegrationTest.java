package com.solidariaplus.identity.identityaccess.infrastructure.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.solidariaplus.identity.identityaccess.ServiceIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class PortalPagesIntegrationTest extends ServiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void loginPageHandsTheServerStateToThePortalBundle() throws Exception {
        String html = render("/login?error=locked");

        assertThat(html)
                .contains("data-page=\"login\"")
                .contains("data-action=\"/login\"")
                .contains("data-csrf-parameter=\"_csrf\"")
                .containsPattern("data-csrf-token=\"[^\"]+\"")
                .contains("data-error=\"locked\"")
                .contains("data-activated=\"false\"")
                .contains("data-activate-url=\"/activate\"")
                .contains("/portal/portal.js")
                .contains("<noscript>");
    }

    @Test
    void loginPageMapsAnyOtherErrorToCredentials() throws Exception {
        assertThat(render("/login?error=")).contains("data-error=\"credentials\"");
        assertThat(render("/login?activated"))
                .contains("data-activated=\"true\"")
                .doesNotContain("data-error=");
    }

    @Test
    void activatePageExposesItsContract() throws Exception {
        assertThat(render("/activate"))
                .contains("data-page=\"activate\"")
                .contains("data-action=\"/activate\"")
                .contains("data-login-url=\"/login\"")
                .doesNotContain("data-error=");
    }

    @Test
    void portalAssetsArePublic() throws Exception {
        int status = mockMvc.perform(get("/portal/portal.js"))
                .andReturn()
                .getResponse()
                .getStatus();
        assertThat(status).isNotIn(401, 403, 302);
    }

    private String render(String path) throws Exception {
        return mockMvc.perform(get(path))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }
}
