package com.solidariaplus.identity.identityaccess.application.port.output;

import java.util.UUID;

public interface ProcessedEventStore {
    boolean markProcessed(UUID eventId, String eventType);
}
