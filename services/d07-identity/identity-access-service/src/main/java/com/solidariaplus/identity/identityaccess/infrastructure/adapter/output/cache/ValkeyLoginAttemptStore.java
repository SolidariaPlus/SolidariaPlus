package com.solidariaplus.identity.identityaccess.infrastructure.adapter.output.cache;

import com.solidariaplus.identity.identityaccess.application.port.output.LoginAttemptStore;
import com.solidariaplus.identity.identityaccess.domain.model.vo.TokenHash;
import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/** Keys ia:fail:{sha256(identifier)} and ia:lock:{sha256(identifier)}: no personal data in the key. */
@Component
class ValkeyLoginAttemptStore implements LoginAttemptStore {
    private final StringRedisTemplate valkey;
    private final ValkeyKeys keys;
    private final ExpiringCounter counter;

    ValkeyLoginAttemptStore(StringRedisTemplate valkey, ValkeyKeys keys, ExpiringCounter counter) {
        this.valkey = valkey;
        this.keys = keys;
        this.counter = counter;
    }

    @Override
    public boolean isLocked(String identifier) {
        return Boolean.TRUE.equals(valkey.hasKey(key("lock", identifier)));
    }

    @Override
    public long recordFailure(String identifier, Duration window) {
        return counter.increment(key("fail", identifier), window);
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
        return keys.of(kind, TokenHash.of(identifier).sha256Hex());
    }
}
