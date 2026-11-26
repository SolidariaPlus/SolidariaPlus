package com.solidariaplus.identity.identityaccess.infrastructure.adapter.output.cache;

import com.solidariaplus.identity.identityaccess.application.port.output.LoginAttemptStore;
import com.solidariaplus.identity.identityaccess.domain.model.vo.TokenHash;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
class ValkeyLoginAttemptStore implements LoginAttemptStore {
    private final StringRedisTemplate valkey;
    private final String keyPrefix;

    ValkeyLoginAttemptStore(StringRedisTemplate valkey, @Value("${solidaria.cache.key-prefix}") String keyPrefix) {
        this.valkey = valkey;
        this.keyPrefix = keyPrefix;
    }

    @Override
    public boolean isLocked(String identifier) {
        return Boolean.TRUE.equals(valkey.hasKey(key("lock", identifier)));
    }

    @Override
    public long recordFailure(String identifier, Duration window) {
        String key = key("fail", identifier);
        Long failures = valkey.opsForValue().increment(key);
        if (failures != null && failures == 1) {
            valkey.expire(key, window);
        }
        return failures == null ? 0 : failures;
    }

    @Override
    public void lock(String identifier, Duration duration) {
        valkey.opsForValue().set(key("lock", identifier), "1", duration);
    }

    @Override
    public void clear(String identifier) {
        valkey.delete(key("fail", identifier));
    }

    private String key(String kind, String identifier) {
        return keyPrefix + kind + ":" + TokenHash.of(identifier).sha256Hex();
    }
}
