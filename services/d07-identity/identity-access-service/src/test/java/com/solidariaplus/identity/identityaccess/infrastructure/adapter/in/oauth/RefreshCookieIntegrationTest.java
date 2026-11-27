package com.solidariaplus.identity.identityaccess.infrastructure.adapter.in.oauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.solidariaplus.identity.identityaccess.ServiceIntegrationTest;
import com.solidariaplus.identity.identityaccess.domain.model.vo.SubjectId;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.Principal;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.core.endpoint.PkceParameterNames;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationCode;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@AutoConfigureMockMvc
class RefreshCookieIntegrationTest extends ServiceIntegrationTest {

    private static final String MEMBER_SUB = "01930000-0000-7000-8000-000000000101";
    private static final String WEB_CLIENT = "solidaria-web";
    private static final String MOBILE_CLIENT = "solidaria-mobile";
    private static final String REDIRECT_URI_WEB = "http://localhost/auth/callback";
    private static final String REDIRECT_URI_MOBILE = "ec.solidariaplus.mobile:/oauth2/callback";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OAuth2AuthorizationService authorizationService;

    @Autowired
    private RegisteredClientRepository registeredClients;

    @Test
    void webClient_tokenResponse_refreshTokenDeliveredAsCookieNotInBody() throws Exception {
        PkcePair pkce = generatePkce();
        String code = "test-cookie-" + UUID.randomUUID();
        storeAuthorization(code, pkce.challenge(), WEB_CLIENT, REDIRECT_URI_WEB);

        MvcResult result = mockMvc.perform(post("/oauth2/token")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param(OAuth2ParameterNames.GRANT_TYPE, AuthorizationGrantType.AUTHORIZATION_CODE.getValue())
                        .param(OAuth2ParameterNames.CODE, code)
                        .param(OAuth2ParameterNames.REDIRECT_URI, REDIRECT_URI_WEB)
                        .param(OAuth2ParameterNames.CLIENT_ID, WEB_CLIENT)
                        .param(PkceParameterNames.CODE_VERIFIER, pkce.verifier()))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        String setCookie = result.getResponse().getHeader("Set-Cookie");

        assertThat(body).doesNotContain("refresh_token");
        assertThat(setCookie).isNotNull();
        assertThat(setCookie).contains(RefreshCookieTokenResponseHandler.COOKIE_NAME + "=");
        assertThat(setCookie).contains("HttpOnly");
        assertThat(setCookie).contains("Secure");
        assertThat(setCookie).contains("SameSite=Strict");
    }

    @Test
    void mobileClient_tokenResponse_refreshTokenInBodyNoCookie() throws Exception {
        PkcePair pkce = generatePkce();
        String code = "test-mobile-" + UUID.randomUUID();
        storeAuthorization(code, pkce.challenge(), MOBILE_CLIENT, REDIRECT_URI_MOBILE);

        MvcResult result = mockMvc.perform(post("/oauth2/token")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param(OAuth2ParameterNames.GRANT_TYPE, AuthorizationGrantType.AUTHORIZATION_CODE.getValue())
                        .param(OAuth2ParameterNames.CODE, code)
                        .param(OAuth2ParameterNames.REDIRECT_URI, REDIRECT_URI_MOBILE)
                        .param(OAuth2ParameterNames.CLIENT_ID, MOBILE_CLIENT)
                        .param(PkceParameterNames.CODE_VERIFIER, pkce.verifier()))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        String setCookie = result.getResponse().getHeader("Set-Cookie");

        assertThat(body).contains("refresh_token");
        assertThat(setCookie).isNull();
    }

    private void storeAuthorization(String codeValue, String codeChallenge, String clientId, String redirectUri) {
        RegisteredClient client = registeredClients.findByClientId(clientId);
        Instant now = Instant.now();
        OAuth2AuthorizationCode code = new OAuth2AuthorizationCode(codeValue, now, now.plusSeconds(120));
        SubjectId sub = new SubjectId(UUID.fromString(MEMBER_SUB));
        UsernamePasswordAuthenticationToken principal =
                UsernamePasswordAuthenticationToken.authenticated(sub.value().toString(), null, List.of());

        OAuth2Authorization authorization = OAuth2Authorization.withRegisteredClient(client)
                .id(UUID.randomUUID().toString())
                .principalName(sub.value().toString())
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizedScopes(Set.of(OidcScopes.OPENID, "offline_access"))
                .attribute(Principal.class.getName(), principal)
                .attribute(OAuth2ParameterNames.REDIRECT_URI, redirectUri)
                .attribute(PkceParameterNames.CODE_CHALLENGE, codeChallenge)
                .attribute(PkceParameterNames.CODE_CHALLENGE_METHOD, "S256")
                .token(code)
                .build();

        authorizationService.save(authorization);
    }

    private PkcePair generatePkce() throws Exception {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        String verifier = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII));
        String challenge = Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        return new PkcePair(verifier, challenge);
    }

    private record PkcePair(String verifier, String challenge) {}
}
