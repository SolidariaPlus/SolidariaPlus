package com.solidariaplus.security;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * Auto-configures a resource server that accepts Solidaria JWTs (ES256, audience solidaria-api).
 *
 * <p>Adoption: add {@code solidariaplus:security} to the service's dependencies and set
 * {@code solidaria.security.jwk-set-uri} in application.yml. The filter chain bean name is
 * {@code solidariaResourceServerFilterChain}; override it to customise path rules.
 */
@AutoConfiguration
@ConditionalOnClass(HttpSecurity.class)
@EnableConfigurationProperties(SolidariaSecurityProperties.class)
public class SolidariaSecurityAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(SolidariaJwtAuthenticationConverter.class)
    public SolidariaJwtAuthenticationConverter solidariaJwtAuthenticationConverter() {
        return new SolidariaJwtAuthenticationConverter();
    }

    @Bean
    @ConditionalOnMissingBean(name = "solidariaJwtDecoder")
    @ConditionalOnProperty("solidaria.security.jwk-set-uri")
    public JwtDecoder solidariaJwtDecoder(SolidariaSecurityProperties props) {
        NimbusJwtDecoder decoder =
                NimbusJwtDecoder.withJwkSetUri(props.getJwkSetUri()).build();

        OAuth2TokenValidator<Jwt> base = props.getIssuerUri() != null
                ? JwtValidators.createDefaultWithIssuer(props.getIssuerUri())
                : JwtValidators.createDefault();

        OAuth2TokenValidator<Jwt> audienceValidator = new SolidariaAudienceValidator(props.getAudience());

        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(base, audienceValidator));
        return decoder;
    }

    @Bean
    @ConditionalOnMissingBean(RevokedSessionFilter.class)
    @ConditionalOnProperty("solidaria.security.revoked-sessions-uri")
    public RevokedSessionFilter revokedSessionFilter(StringRedisTemplate redis) {
        return new RevokedSessionFilter(redis);
    }
}
