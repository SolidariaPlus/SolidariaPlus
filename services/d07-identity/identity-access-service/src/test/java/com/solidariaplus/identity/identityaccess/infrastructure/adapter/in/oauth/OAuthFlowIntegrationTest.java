package com.solidariaplus.identity.identityaccess.infrastructure.adapter.in.oauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jwt.SignedJWT;
import com.solidariaplus.identity.identityaccess.ServiceIntegrationTest;
import com.solidariaplus.identity.identityaccess.domain.model.vo.SubjectId;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.Principal;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
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
class OAuthFlowIntegrationTest extends ServiceIntegrationTest {

    private static final String MEMBER_SUB = "01930000-0000-7000-8000-000000000101";
    private static final String MEMBER_ID = "01930000-0000-7000-8000-000000000201";
    private static final String TELLER_SUB = "01930000-0000-7000-8000-000000000001";
    private static final String BOARD_PRESIDENT_SUB = "01930000-0000-7000-8000-000000000002";
    private static final String WEB_CLIENT = "solidaria-web";
    private static final String REDIRECT_URI = "http://localhost/callback";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OAuth2AuthorizationService authorizationService;

    @Autowired
    private RegisteredClientRepository registeredClients;

    // --- PKCE flow: member claims ---

    @Test
    void validPkceFlowMemberSubjectAccessTokenHasMidAndMemberRole() throws Exception {
        PkcePair pkce = generatePkce();
        String code = uniqueCode();
        storeAuthorization(code, pkce.challenge(), new SubjectId(UUID.fromString(MEMBER_SUB)));

        String accessToken = exchangeCode(code, pkce.verifier(), REDIRECT_URI);

        SignedJWT jwt = SignedJWT.parse(accessToken);
        Map<String, Object> claims = jwt.getJWTClaimsSet().getClaims();
        assertThat(claims).containsKey("mid");
        assertThat((String) claims.get("mid")).isEqualTo(MEMBER_ID);
        assertThat(castList(claims.get("roles"))).containsExactly("MEMBER");
        assertThat(castList(claims.get("amr"))).containsExactly("pwd");
        assertThat(claims).containsKey("sid");
        assertThat(claims).doesNotContainKey("fn");
    }

    // --- PKCE flow: staff claims ---

    @Test
    void validPkceFlowStaffSubjectAccessTokenHasRolesButNoMid() throws Exception {
        PkcePair pkce = generatePkce();
        String code = uniqueCode();
        storeAuthorization(code, pkce.challenge(), new SubjectId(UUID.fromString(TELLER_SUB)));

        String accessToken = exchangeCode(code, pkce.verifier(), REDIRECT_URI);

        SignedJWT jwt = SignedJWT.parse(accessToken);
        Map<String, Object> claims = jwt.getJWTClaimsSet().getClaims();
        assertThat(castList(claims.get("roles"))).containsExactly("TELLER");
        assertThat(claims).doesNotContainKey("mid");
        assertThat(claims).doesNotContainKey("fn");
    }

    @Test
    void validPkceFlowStaffWithFunctionAccessTokenHasFnClaim() throws Exception {
        PkcePair pkce = generatePkce();
        String code = uniqueCode();
        storeAuthorization(code, pkce.challenge(), new SubjectId(UUID.fromString(BOARD_PRESIDENT_SUB)));

        String accessToken = exchangeCode(code, pkce.verifier(), REDIRECT_URI);

        SignedJWT jwt = SignedJWT.parse(accessToken);
        Map<String, Object> claims = jwt.getJWTClaimsSet().getClaims();
        assertThat(castList(claims.get("roles"))).containsExactly("BOARD");
        assertThat(castList(claims.get("fn"))).containsExactly("PRESIDENT");
        assertThat(claims).doesNotContainKey("mid");
    }

    // --- PKCE validation ---

    @Test
    void missingCodeVerifierTokenEndpointRejectsTheClientWithoutRedirect() throws Exception {
        PkcePair pkce = generatePkce();
        String code = uniqueCode();
        storeAuthorization(code, pkce.challenge(), new SubjectId(UUID.fromString(MEMBER_SUB)));

        mockMvc.perform(post("/oauth2/token")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param(OAuth2ParameterNames.GRANT_TYPE, AuthorizationGrantType.AUTHORIZATION_CODE.getValue())
                        .param(OAuth2ParameterNames.CODE, code)
                        .param(OAuth2ParameterNames.REDIRECT_URI, REDIRECT_URI)
                        .param(OAuth2ParameterNames.CLIENT_ID, WEB_CLIENT))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void wrongCodeVerifierTokenEndpointReturnsBadRequest() throws Exception {
        PkcePair pkce = generatePkce();
        PkcePair otherPkce = generatePkce();
        String code = uniqueCode();
        storeAuthorization(code, pkce.challenge(), new SubjectId(UUID.fromString(MEMBER_SUB)));

        mockMvc.perform(post("/oauth2/token")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param(OAuth2ParameterNames.GRANT_TYPE, AuthorizationGrantType.AUTHORIZATION_CODE.getValue())
                        .param(OAuth2ParameterNames.CODE, code)
                        .param(OAuth2ParameterNames.REDIRECT_URI, REDIRECT_URI)
                        .param(OAuth2ParameterNames.CLIENT_ID, WEB_CLIENT)
                        .param(PkceParameterNames.CODE_VERIFIER, otherPkce.verifier()))
                .andExpect(status().isBadRequest());
    }

    // --- Redirect URI validation ---

    @Test
    void unregisteredRedirectUriAuthorizationEndpointDoesNotRedirectToIt() throws Exception {
        PkcePair pkce = generatePkce();
        String evilRedirectUri = "https://evil.example.com/callback";

        MvcResult result = mockMvc.perform(get("/oauth2/authorize")
                        .param(OAuth2ParameterNames.RESPONSE_TYPE, "code")
                        .param(OAuth2ParameterNames.CLIENT_ID, WEB_CLIENT)
                        .param(OAuth2ParameterNames.REDIRECT_URI, evilRedirectUri)
                        .param(PkceParameterNames.CODE_CHALLENGE, pkce.challenge())
                        .param(PkceParameterNames.CODE_CHALLENGE_METHOD, "S256"))
                .andReturn();

        // RFC 6749 §4.1.2.1: MUST NOT redirect to an invalid redirection URI
        String location = result.getResponse().getHeader("Location");
        if (location != null) {
            assertThat(location).doesNotContain("evil.example.com");
        }
    }

    // --- JWKS ---

    @Test
    void jwksEndpointReturnsTwoKeys() throws Exception {
        MvcResult result =
                mockMvc.perform(get("/oauth2/jwks")).andExpect(status().isOk()).andReturn();

        JWKSet jwkSet = JWKSet.parse(result.getResponse().getContentAsString());
        assertThat(jwkSet.getKeys()).hasSize(2);
        assertThat(jwkSet.getKeyByKeyId("test-current")).isNotNull();
        assertThat(jwkSet.getKeyByKeyId("test-previous")).isNotNull();
    }

    @Test
    void jwksEndpointHasCacheControlHeader() throws Exception {
        mockMvc.perform(get("/oauth2/jwks"))
                .andExpect(status().isOk())
                .andExpect(result -> assertThat(result.getResponse().getHeader("Cache-Control"))
                        .contains("max-age=86400"));
    }

    // --- Independent JOSE validation ---

    @Test
    void accessTokenValidatesAgainstPublishedJwks() throws Exception {
        PkcePair pkce = generatePkce();
        String code = uniqueCode();
        storeAuthorization(code, pkce.challenge(), new SubjectId(UUID.fromString(MEMBER_SUB)));

        String accessToken = exchangeCode(code, pkce.verifier(), REDIRECT_URI);

        MvcResult jwksResult =
                mockMvc.perform(get("/oauth2/jwks")).andExpect(status().isOk()).andReturn();
        JWKSet jwkSet = JWKSet.parse(jwksResult.getResponse().getContentAsString());

        SignedJWT jwt = SignedJWT.parse(accessToken);
        String kid = jwt.getHeader().getKeyID();
        ECKey signingKey = (ECKey) jwkSet.getKeyByKeyId(kid);

        assertThat(signingKey).as("signing key kid=%s must be in JWKS", kid).isNotNull();
        assertThat(jwt.verify(new ECDSAVerifier(signingKey.toECPublicKey())))
                .as("access token signature must verify against the JWKS public key")
                .isTrue();
    }

    @Test
    void accessTokenHasExpectedStaticClaims() throws Exception {
        PkcePair pkce = generatePkce();
        String code = uniqueCode();
        storeAuthorization(code, pkce.challenge(), new SubjectId(UUID.fromString(MEMBER_SUB)));

        String accessToken = exchangeCode(code, pkce.verifier(), REDIRECT_URI);

        SignedJWT jwt = SignedJWT.parse(accessToken);
        Map<String, Object> claims = jwt.getJWTClaimsSet().getClaims();
        assertThat(castList(claims.get("aud"))).containsExactly("solidaria-api");
        assertThat(jwt.getHeader().getAlgorithm().getName()).isEqualTo("ES256");
    }

    // --- Activation codes by member number (screen B10) ---

    @Test
    void tellerIssuesActivationCodeByMemberNumber() throws Exception {
        MvcResult result = mockMvc.perform(post("/members/by-number/1003/activation-codes")
                        .header("Authorization", "Bearer " + accessTokenFor(TELLER_SUB)))
                .andExpect(status().isCreated())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertThat((String) com.jayway.jsonpath.JsonPath.read(body, "$.memberNumber"))
                .isEqualTo("1003");
        assertThat((String) com.jayway.jsonpath.JsonPath.read(body, "$.memberId"))
                .isEqualTo("01930000-0000-7000-8000-000000000203");
        assertThat((String) com.jayway.jsonpath.JsonPath.read(body, "$.code")).matches("^[A-HJ-NP-Z2-9]{8}$");
        assertThat(result.getResponse().getHeader("Cache-Control")).contains("no-store");
    }

    @Test
    void memberCannotIssueActivationCodeByMemberNumber() throws Exception {
        mockMvc.perform(post("/members/by-number/1003/activation-codes")
                        .header("Authorization", "Bearer " + accessTokenFor(MEMBER_SUB)))
                .andExpect(status().isForbidden());
    }

    @Test
    void anonymousCannotIssueActivationCodeByMemberNumber() throws Exception {
        mockMvc.perform(post("/members/by-number/1003/activation-codes")).andExpect(status().isUnauthorized());
    }

    @Test
    void unknownOrMalformedMemberNumberIsNotFound() throws Exception {
        String token = accessTokenFor(TELLER_SUB);
        for (String number : List.of("9999999999", "12ab")) {
            MvcResult result = mockMvc.perform(post("/members/by-number/" + number + "/activation-codes")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isNotFound())
                    .andReturn();
            assertThat((String) com.jayway.jsonpath.JsonPath.read(
                            result.getResponse().getContentAsString(), "$.code"))
                    .isEqualTo("MEMBER_NOT_FOUND");
        }
    }

    // --- helpers ---

    private String accessTokenFor(String sub) throws Exception {
        PkcePair pkce = generatePkce();
        String code = uniqueCode();
        storeAuthorization(code, pkce.challenge(), new SubjectId(UUID.fromString(sub)));
        return exchangeCode(code, pkce.verifier(), REDIRECT_URI);
    }

    private String exchangeCode(String code, String codeVerifier, String redirectUri) throws Exception {
        MvcResult result = mockMvc.perform(post("/oauth2/token")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param(OAuth2ParameterNames.GRANT_TYPE, AuthorizationGrantType.AUTHORIZATION_CODE.getValue())
                        .param(OAuth2ParameterNames.CODE, code)
                        .param(OAuth2ParameterNames.REDIRECT_URI, redirectUri)
                        .param(OAuth2ParameterNames.CLIENT_ID, WEB_CLIENT)
                        .param(PkceParameterNames.CODE_VERIFIER, codeVerifier))
                .andExpect(status().isOk())
                .andReturn();

        // Extract access_token from JSON without depending on a specific Jackson version
        String body = result.getResponse().getContentAsString();
        return com.jayway.jsonpath.JsonPath.read(body, "$.access_token");
    }

    @SuppressWarnings("unchecked")
    private static List<String> castList(Object value) {
        return (List<String>) value;
    }

    private void storeAuthorization(String codeValue, String codeChallenge, SubjectId sub) {
        RegisteredClient client = registeredClients.findByClientId(WEB_CLIENT);
        Instant now = Instant.now();
        OAuth2AuthorizationCode code = new OAuth2AuthorizationCode(codeValue, now, now.plusSeconds(120));
        UsernamePasswordAuthenticationToken principal =
                UsernamePasswordAuthenticationToken.authenticated(sub.value().toString(), null, List.of());

        OAuth2Authorization authorization = OAuth2Authorization.withRegisteredClient(client)
                .id(UUID.randomUUID().toString())
                .principalName(sub.value().toString())
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizedScopes(Set.of(OidcScopes.OPENID, "offline_access"))
                .attribute(Principal.class.getName(), principal)
                .attribute(
                        OAuth2AuthorizationRequest.class.getName(),
                        OAuth2AuthorizationRequest.authorizationCode()
                                .authorizationUri("http://localhost/oauth2/authorize")
                                .clientId(WEB_CLIENT)
                                .redirectUri(REDIRECT_URI)
                                .scopes(Set.of(OidcScopes.OPENID, "offline_access"))
                                .state("state")
                                .additionalParameters(Map.of(
                                        PkceParameterNames.CODE_CHALLENGE,
                                        codeChallenge,
                                        PkceParameterNames.CODE_CHALLENGE_METHOD,
                                        "S256"))
                                .build())
                .attribute(OAuth2ParameterNames.REDIRECT_URI, REDIRECT_URI)
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

    private static String uniqueCode() {
        return "test-code-" + UUID.randomUUID();
    }

    private record PkcePair(String verifier, String challenge) {}
}
