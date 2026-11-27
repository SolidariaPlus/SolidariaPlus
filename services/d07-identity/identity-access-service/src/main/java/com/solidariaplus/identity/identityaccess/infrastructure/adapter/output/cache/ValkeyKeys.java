package com.solidariaplus.identity.identityaccess.infrastructure.adapter.output.cache;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Builds the keys of MS-17 under the prefix its Valkey ACL user may access (ADR-055). */
@Component
class ValkeyKeys {
    private final String prefix;

    ValkeyKeys(@Value("${solidaria.cache.key-prefix}") String prefix) {
        this.prefix = prefix;
    }

    String of(String kind, String id) {
        return prefix + kind + ":" + id;
    }
}
