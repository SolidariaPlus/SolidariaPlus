package com.solidariaplus.identity.identityaccess.infrastructure.config;

import com.solidariaplus.identity.identityaccess.domain.service.Uuids;
import java.util.Optional;
import org.slf4j.MDC;

public final class CorrelationId {
    static final String MDC_KEY = "correlationId";
    static final String HEADER = "X-Correlation-Id";

    private CorrelationId() {}

    public static String current() {
        return Optional.ofNullable(MDC.get(MDC_KEY)).orElseGet(() -> Uuids.v7().toString());
    }
}
