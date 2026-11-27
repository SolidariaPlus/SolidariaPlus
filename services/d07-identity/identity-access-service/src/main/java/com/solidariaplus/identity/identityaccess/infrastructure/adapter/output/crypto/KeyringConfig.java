package com.solidariaplus.identity.identityaccess.infrastructure.adapter.output.crypto;

import java.nio.ByteBuffer;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.cryptography.materialproviders.IKeyring;
import software.amazon.cryptography.materialproviders.MaterialProviders;
import software.amazon.cryptography.materialproviders.model.AesWrappingAlg;
import software.amazon.cryptography.materialproviders.model.CreateAwsKmsKeyringInput;
import software.amazon.cryptography.materialproviders.model.CreateRawAesKeyringInput;
import software.amazon.cryptography.materialproviders.model.MaterialProvidersConfig;

@Configuration(proxyBeanMethods = false)
class KeyringConfig {
    private static final MaterialProviders PROVIDERS = MaterialProviders.builder()
            .MaterialProvidersConfig(MaterialProvidersConfig.builder().build())
            .build();

    @Bean
    @ConditionalOnProperty("solidaria.encryption.kms-key-id")
    IKeyring kmsKeyring(@Value("${solidaria.encryption.kms-key-id}") String kmsKeyId) {
        return PROVIDERS.CreateAwsKmsKeyring(CreateAwsKmsKeyringInput.builder()
                .kmsKeyId(kmsKeyId)
                .kmsClient(KmsClient.create())
                .build());
    }

    @Bean
    @ConditionalOnMissingBean(IKeyring.class)
    IKeyring rawAesKeyring(@Value("${solidaria.encryption.local-key}") String base64Key) {
        return PROVIDERS.CreateRawAesKeyring(CreateRawAesKeyringInput.builder()
                .keyNamespace("solidaria-local")
                .keyName("personal-data")
                .wrappingKey(ByteBuffer.wrap(Base64.getDecoder().decode(base64Key)))
                .wrappingAlg(AesWrappingAlg.ALG_AES256_GCM_IV12_TAG16)
                .build());
    }
}
