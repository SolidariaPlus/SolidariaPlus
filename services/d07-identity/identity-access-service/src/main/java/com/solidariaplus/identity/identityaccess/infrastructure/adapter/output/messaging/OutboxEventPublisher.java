package com.solidariaplus.identity.identityaccess.infrastructure.adapter.output.messaging;

import com.solidariaplus.identity.identityaccess.application.port.output.EventPublisher;
import com.solidariaplus.identity.identityaccess.domain.event.AuditRecorded;
import com.solidariaplus.identity.identityaccess.domain.event.DomainEvent;
import com.solidariaplus.identity.identityaccess.domain.event.SessionRevoked;
import com.solidariaplus.identity.identityaccess.domain.service.Uuids;
import com.solidariaplus.identity.identityaccess.infrastructure.config.CorrelationId;
import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
class OutboxEventPublisher implements EventPublisher {
    private static final String SOURCE = "/services/identity-access-service";

    private final JdbcClient jdbc;
    private final JsonMapper mapper;

    OutboxEventPublisher(JdbcClient jdbc, JsonMapper mapper) {
        this.jdbc = jdbc;
        this.mapper = mapper;
    }

    @Override
    public void publish(DomainEvent event) {
        Message message = message(event);
        UUID eventId = Uuids.v7();
        Map<String, Object> cloudEvent = new LinkedHashMap<>();
        cloudEvent.put("specversion", "1.0");
        cloudEvent.put("id", eventId);
        cloudEvent.put("source", SOURCE);
        cloudEvent.put("type", "ec.solidariaplus." + message.context() + "." + message.name() + ".v1");
        cloudEvent.put("subject", message.key());
        cloudEvent.put("time", event.occurredAt());
        cloudEvent.put("datacontenttype", "application/json");
        cloudEvent.put("dataschema", "urn:solidariaplus:schema:" + message.context() + ":" + message.name() + ":v1");
        cloudEvent.put("correlationid", CorrelationId.current());
        cloudEvent.put("data", message.data());
        jdbc.sql("""
                        INSERT INTO outbox (event_id, topic, event_key, event_type, payload, occurred_at)
                        VALUES (:id, :topic, :key, :type, CAST(:payload AS jsonb), :occurredAt)
                        """)
                .param("id", eventId)
                .param("topic", message.topic())
                .param("key", message.key())
                .param("type", message.name())
                .param("payload", mapper.writeValueAsString(cloudEvent))
                .param("occurredAt", Timestamp.from(event.occurredAt()))
                .update();
    }

    private static Message message(DomainEvent event) {
        return switch (event) {
            case SessionRevoked revoked -> sessionRevoked(revoked);
            case AuditRecorded audit -> auditRecord(audit);
        };
    }

    private static Message sessionRevoked(SessionRevoked event) {
        String subject = event.subject().value().toString();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("subject", subject);
        event.sessionId().ifPresent(sessionId -> data.put("sessionId", sessionId.value()));
        data.put("scope", event.scope());
        data.put("reason", event.reason());
        data.put("revokedAt", event.occurredAt());
        data.put("tokensExpireBy", event.tokensExpireBy());
        return new Message("identity-access.events", "identity", "SessionRevoked", subject, data);
    }

    private static Message auditRecord(AuditRecorded event) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("recordId", event.recordId());
        data.put("recordType", "ACTION");
        data.put("action", event.action());
        data.put("actorSub", event.actorSub().value());
        data.put("actorRole", event.actorRole());
        data.put("entityType", event.entityType());
        data.put("entityId", event.entityId());
        data.put("outcome", event.outcome());
        data.put("occurredAt", event.occurredAt());
        return new Message("audit.records", "compliance", "AuditRecord", event.entityId(), data);
    }

    private record Message(String topic, String context, String name, String key, Map<String, Object> data) {}
}
