package com.solidariaplus.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Rejects requests whose access token belongs to a revoked session before the token's own expiry.
 *
 * <p>The revocation state is kept in Valkey under the key {@code revoked:<sid>}. The filter caches
 * "not revoked" locally for 10 s to avoid a cache read on every request; "revoked" entries are
 * never cached so a revocation takes effect within one TTL cycle.
 */
public class RevokedSessionFilter extends OncePerRequestFilter {

    private static final String KEY_PREFIX = "revoked:";
    private static final long LOCAL_CACHE_SECONDS = 10L;

    private final StringRedisTemplate redis;
    private final Set<String> notRevokedCache = ConcurrentHashMap.newKeySet();

    public RevokedSessionFilter(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String sid = extractSid(request);
        if (sid != null && isRevoked(sid)) {
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "Session revoked");
            return;
        }
        chain.doFilter(request, response);
    }

    private static String extractSid(HttpServletRequest request) {
        Object principal = request.getUserPrincipal();
        if (principal instanceof JwtAuthenticationToken token) {
            return token.getToken().getClaimAsString("sid");
        }
        return null;
    }

    private boolean isRevoked(String sid) {
        if (notRevokedCache.contains(sid)) {
            return false;
        }
        Boolean exists = redis.hasKey(KEY_PREFIX + sid);
        if (Boolean.TRUE.equals(exists)) {
            return true;
        }
        notRevokedCache.add(sid);
        // Expire the local cache entry
        scheduler(sid);
        return false;
    }

    private void scheduler(String sid) {
        Thread.ofVirtual().start(() -> {
            try {
                TimeUnit.SECONDS.sleep(LOCAL_CACHE_SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                notRevokedCache.remove(sid);
            }
        });
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Tokens without a sid claim (service-to-service) are not session-bound
        return false;
    }
}
