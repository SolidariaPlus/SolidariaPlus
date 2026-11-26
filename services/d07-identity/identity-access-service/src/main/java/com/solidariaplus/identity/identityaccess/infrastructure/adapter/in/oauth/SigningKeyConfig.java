package com.solidariaplus.identity.identityaccess.infrastructure.adapter.in.oauth;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
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
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class SigningKeyConfig {

    @Bean
    JWKSource<SecurityContext> jwkSource(
            @Value("${signing.kid}") String keyId,
            @Value("${signing.privateKey}") String privateKey,
            @Value("${signing.publicKey}") String publicKey)
            throws GeneralSecurityException {
        KeyFactory keys = KeyFactory.getInstance("EC");
        ECPublicKey verificationKey = (ECPublicKey)
                keys.generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(publicKey)));
        ECPrivateKey signingKey = (ECPrivateKey)
                keys.generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(privateKey)));
        ECKey key = new ECKey.Builder(Curve.P_256, verificationKey)
                .privateKey(signingKey)
                .keyID(keyId)
                .algorithm(JWSAlgorithm.ES256)
                .keyUse(KeyUse.SIGNATURE)
                .build();
        return new ImmutableJWKSet<>(new JWKSet(key));
    }
}
