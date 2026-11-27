package com.solidariaplus.identity.identityaccess.infrastructure.adapter.in.oauth;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration(proxyBeanMethods = false)
class SigningKeyConfig {

    @Bean
    JWKSource<SecurityContext> jwkSource(
            @Value("${signing.kid}") String keyId,
            @Value("${signing.privateKey}") String privateKey,
            @Value("${signing.publicKey}") String publicKey,
            @Value("${signing.previous-kid:}") String previousKeyId,
            @Value("${signing.previous-publicKey:}") String previousPublicKey)
            throws GeneralSecurityException {
        KeyFactory keyFactory = KeyFactory.getInstance("EC");

        ECKey currentKey = buildSigningKey(keyFactory, keyId, privateKey, publicKey);

        List<JWK> keys = new ArrayList<>();
        keys.add(currentKey);

        if (StringUtils.hasText(previousKeyId) && StringUtils.hasText(previousPublicKey)) {
            keys.add(buildVerificationKey(keyFactory, previousKeyId, previousPublicKey));
        }

        return new ImmutableJWKSet<>(new JWKSet(keys));
    }

    private static ECKey buildSigningKey(KeyFactory keyFactory, String kid, String privateKeyB64, String publicKeyB64)
            throws GeneralSecurityException {
        ECPublicKey ecPublicKey = (ECPublicKey) keyFactory.generatePublic(
                new X509EncodedKeySpec(Base64.getDecoder().decode(publicKeyB64)));
        ECPrivateKey ecPrivateKey = (ECPrivateKey) keyFactory.generatePrivate(
                new PKCS8EncodedKeySpec(Base64.getDecoder().decode(privateKeyB64)));
        return new ECKey.Builder(Curve.P_256, ecPublicKey)
                .privateKey(ecPrivateKey)
                .keyID(kid)
                .algorithm(JWSAlgorithm.ES256)
                .keyUse(KeyUse.SIGNATURE)
                .build();
    }

    /** Public-only key: published in JWKS during the 90-day rotation overlap (RNF-17-04). */
    private static ECKey buildVerificationKey(KeyFactory keyFactory, String kid, String publicKeyB64)
            throws GeneralSecurityException {
        ECPublicKey ecPublicKey = (ECPublicKey) keyFactory.generatePublic(
                new X509EncodedKeySpec(Base64.getDecoder().decode(publicKeyB64)));
        return new ECKey.Builder(Curve.P_256, ecPublicKey)
                .keyID(kid)
                .algorithm(JWSAlgorithm.ES256)
                .keyUse(KeyUse.SIGNATURE)
                .build();
    }
}
