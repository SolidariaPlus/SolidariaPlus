package com.solidariaplus.identity.identityaccess.infrastructure.adapter.output.cache;

import java.time.Duration;
import java.util.List;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

/** Counter whose TTL starts with its first increment; one script, so a crash never leaves it without TTL. */
@Component
class ExpiringCounter {
    private static final RedisScript<Long> INCREMENT = RedisScript.of("""
            local count = redis.call('INCR', KEYS[1])
            if count == 1 then
                redis.call('PEXPIRE', KEYS[1], ARGV[1])
            end
            return count
            """, Long.class);

    private final StringRedisTemplate valkey;

    ExpiringCounter(StringRedisTemplate valkey) {
        this.valkey = valkey;
    }

    long increment(String key, Duration ttl) {
        Long count = valkey.execute(INCREMENT, List.of(key), String.valueOf(ttl.toMillis()));
        return count == null ? 0 : count;
    }
}
