package com.solidariaplus.identity.identityaccess.infrastructure.adapter.in.oauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nimbusds.jwt.SignedJWT;
import com.solidariaplus.identity.identityaccess.ServiceIntegrationTest;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@AutoConfigureMockMvc
class M2mTokenIntegrationTest extends ServiceIntegrationTest {

    private static final String SERVICE_CLIENT_ID = "solidaria-services";
    private static final String SERVICE_CLIENT_SECRET = "test-services-secret";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void clientCredentials_validSecret_returnsAccessTokenWithServiceRole() throws Exception {
        String credentials =
                Base64.getEncoder().encodeToString((SERVICE_CLIENT_ID + ":" + SERVICE_CLIENT_SECRET).getBytes());

        MvcResult result = mockMvc.perform(post("/oauth2/token")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .header(HttpHeaders.AUTHORIZATION, "Basic " + credentials)
                        .param(OAuth2ParameterNames.GRANT_TYPE, AuthorizationGrantType.CLIENT_CREDENTIALS.getValue())
                        .param(OAuth2ParameterNames.SCOPE, "solidaria.internal"))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        String accessToken = com.jayway.jsonpath.JsonPath.read(body, "$.access_token");
        assertThat(accessToken).isNotBlank();

        SignedJWT jwt = SignedJWT.parse(accessToken);
        Map<String, Object> claims = jwt.getJWTClaimsSet().getClaims();

        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) claims.get("roles");
        assertThat(roles).containsExactly("SERVICE");

        @SuppressWarnings("unchecked")
        List<String> amr = (List<String>) claims.get("amr");
        assertThat(amr).containsExactly("client_secret");

        assertThat(claims).doesNotContainKey("sid");
        assertThat(claims).doesNotContainKey("mid");
    }

    @Test
    void clientCredentials_wrongSecret_returnsUnauthorized() throws Exception {
        String credentials = Base64.getEncoder().encodeToString((SERVICE_CLIENT_ID + ":wrong-secret").getBytes());

        mockMvc.perform(post("/oauth2/token")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .header(HttpHeaders.AUTHORIZATION, "Basic " + credentials)
                        .param(OAuth2ParameterNames.GRANT_TYPE, AuthorizationGrantType.CLIENT_CREDENTIALS.getValue()))
                .andExpect(status().isUnauthorized());
    }
}
