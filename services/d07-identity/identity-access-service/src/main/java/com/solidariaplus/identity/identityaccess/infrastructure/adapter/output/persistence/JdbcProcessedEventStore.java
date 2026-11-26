package com.solidariaplus.identity.identityaccess.infrastructure.adapter.output.persistence;

import com.solidariaplus.identity.identityaccess.application.port.output.ProcessedEventStore;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class JdbcProcessedEventStore implements ProcessedEventStore {
    private static final String INSERT =
            "INSERT INTO processed_events (event_id, event_type) VALUES (:id, :type) ON CONFLICT DO NOTHING";

    private final JdbcClient jdbc;

    JdbcProcessedEventStore(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean markProcessed(UUID eventId, String eventType) {
        return jdbc.sql(INSERT).param("id", eventId).param("type", eventType).update() == 1;
    }
}
