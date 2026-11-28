package com.solidariaplus.identity.identityaccess.infrastructure.adapter.in.oauth;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import com.solidariaplus.identity.identityaccess.application.port.in.RevokeSessionsUseCase;
import com.solidariaplus.identity.identityaccess.application.port.in.SessionStatusUseCase;
import com.solidariaplus.identity.identityaccess.application.port.in.SessionTokenUseCase;
import java.util.Set;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationProvider;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.token.DelegatingOAuth2TokenGenerator;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.JwtGenerator;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.CacheControlHeadersWriter;
import org.springframework.security.web.header.writers.DelegatingRequestMatcherHeaderWriter;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;

@Configuration(proxyBeanMethods = false)
class AuthorizationServerConfig {

    @Bean
    @Order(1)
    SecurityFilterChain authorizationServerFilterChain(HttpSecurity http, RegisteredClientRepository clients)
            throws Exception {
        PublicClientRefreshAuthentication publicRefresh = new PublicClientRefreshAuthentication(clients);
        LoopbackAwareRequestValidator loopbackValidator = new LoopbackAwareRequestValidator();
        RefreshCookieRequestFilter cookieFilter = new RefreshCookieRequestFilter();
        http.oauth2AuthorizationServer(server -> {
                    http.securityMatcher(server.getEndpointsMatcher());
                    server.oidc(Customizer.withDefaults())
                            .clientAuthentication(client -> client.authenticationConverters(
                                            converters -> converters.addFirst(publicRefresh))
                                    .authenticationProviders(providers -> providers.addFirst(publicRefresh)))
                            .authorizationEndpoint(endpoint ->
                                    endpoint.authenticationProviders(providers -> providers.stream()
                                            .filter(p ->
                                                    p instanceof OAuth2AuthorizationCodeRequestAuthenticationProvider)
                                            .map(p -> (OAuth2AuthorizationCodeRequestAuthenticationProvider) p)
                                            .forEach(p -> p.setAuthenticationValidator(loopbackValidator))))
                            .tokenEndpoint(endpoint ->
                                    endpoint.accessTokenResponseHandler(new RefreshCookieTokenResponseHandler()));
                })
                .addFilterBefore(cookieFilter, UsernamePasswordAuthenticationFilter.class)
                .headers(headers -> headers.cacheControl(cache -> cache.disable())
                        .addHeaderWriter(new DelegatingRequestMatcherHeaderWriter(
                                new NegatedRequestMatcher(DiscoveryCacheFilter.CACHED_REQUESTS),
                                new CacheControlHeadersWriter())))
                .authorizeHttpRequests(requests -> requests.anyRequest().authenticated())
                .oauth2ResourceServer(server -> server.jwt(Customizer.withDefaults()))
                .exceptionHandling(exceptions -> exceptions.defaultAuthenticationEntryPointFor(
                        new LoginUrlAuthenticationEntryPoint("/login"), browserNavigation()));
        return http.build();
    }

    private static MediaTypeRequestMatcher browserNavigation() {
        MediaTypeRequestMatcher matcher = new MediaTypeRequestMatcher(MediaType.TEXT_HTML);
        matcher.setIgnoredMediaTypes(Set.of(MediaType.ALL));
        return matcher;
    }

    @Bean
    OAuth2TokenGenerator<?> tokenGenerator(
            JWKSource<SecurityContext> keys, OAuth2TokenCustomizer<JwtEncodingContext> claimsCustomizer) {
        NimbusJwtEncoder encoder = new NimbusJwtEncoder(keys);
        encoder.setJwkSelector(candidates -> candidates.stream()
                .filter(JWK::isPrivate)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No private signing key is configured")));
        JwtGenerator jwtGenerator = new JwtGenerator(encoder);
        jwtGenerator.setJwtCustomizer(claimsCustomizer);
        return new DelegatingOAuth2TokenGenerator(jwtGenerator, new SessionRefreshTokens());
    }

    /** Decoder of the resource servers of MS-17 (REST API and userinfo): revoked sessions are refused. */
    @Bean
    JwtDecoder jwtDecoder(JWKSource<SecurityContext> keys, SessionStatusUseCase sessions) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSource(keys)
                .jwsAlgorithm(SignatureAlgorithm.ES256)
                .build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefault(), new ActiveSessionValidator(sessions)));
        return decoder;
    }

    @Bean
    OAuth2AuthorizationService authorizationService(
            JdbcOperations jdbc,
            RegisteredClientRepository clients,
            SessionTokenUseCase sessionTokens,
            RevokeSessionsUseCase revocations) {
        return new SessionTrackingAuthorizationService(
                new JdbcOAuth2AuthorizationService(jdbc, clients), sessionTokens, revocations);
    }
}
