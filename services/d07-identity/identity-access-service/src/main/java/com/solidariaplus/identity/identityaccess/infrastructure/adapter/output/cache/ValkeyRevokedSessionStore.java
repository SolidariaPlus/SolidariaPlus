package com.solidariaplus.identity.identityaccess.infrastructure.adapter.output.cache;

import com.solidariaplus.identity.identityaccess.application.port.output.RevokedSessionStore;
import com.solidariaplus.identity.identityaccess.domain.model.vo.SessionId;
import java.time.Duration;
import java.time.Instant;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/** Key ia:revoked:{sid} holding the revocation instant until the last access token of the session expires. */
@Component
class ValkeyRevokedSessionStore implements RevokedSessionStore {
    private final StringRedisTemplate valkey;
    private final ValkeyKeys keys;

    ValkeyRevokedSessionStore(StringRedisTemplate valkey, ValkeyKeys keys) {
        this.valkey = valkey;
        this.keys = keys;
    }

    @Override
    public void markRevoked(SessionId sessionId, Instant revokedAt, Duration accessTokenLifetime) {
        valkey.opsForValue().set(key(sessionId), revokedAt.toString(), accessTokenLifetime);
    }

    @Override
    public boolean isRevoked(SessionId sessionId) {
        return Boolean.TRUE.equals(valkey.hasKey(key(sessionId)));
    }

    private String key(SessionId sessionId) {
        return keys.of("revoked", sessionId.value().toString());
    }
}
