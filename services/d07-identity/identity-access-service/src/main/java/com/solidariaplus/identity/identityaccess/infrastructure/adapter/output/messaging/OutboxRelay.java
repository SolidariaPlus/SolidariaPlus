package com.solidariaplus.identity.identityaccess.infrastructure.adapter.output.messaging;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class OutboxRelay {
    private static final String CONTENT_TYPE = "application/cloudevents+json";

    private final JdbcClient jdbc;
    private final KafkaTemplate<String, String> kafka;

    OutboxRelay(JdbcClient jdbc, KafkaTemplate<String, String> kafka) {
        this.jdbc = jdbc;
        this.kafka = kafka;
    }

    @Scheduled(fixedDelayString = "${solidaria.outbox.poll-interval-ms:200}")
    @Transactional
    public void relay() {
        jdbc.sql("""
                        SELECT event_id, topic, event_key, payload::text AS payload FROM outbox
                        WHERE published_at IS NULL ORDER BY occurred_at LIMIT 100 FOR UPDATE SKIP LOCKED
                        """)
                .query((rs, row) -> new Pending(
                        rs.getObject("event_id", UUID.class),
                        rs.getString("topic"),
                        rs.getString("event_key"),
                        rs.getString("payload")))
                .list()
                .forEach(this::publish);
    }

    private void publish(Pending pending) {
        ProducerRecord<String, String> record = new ProducerRecord<>(pending.topic(), pending.key(), pending.payload());
        record.headers().add("content-type", CONTENT_TYPE.getBytes(StandardCharsets.UTF_8));
        kafka.send(record).join();
        jdbc.sql("UPDATE outbox SET published_at = now() WHERE event_id = :id")
                .param("id", pending.eventId())
                .update();
    }

    private record Pending(UUID eventId, String topic, String key, String payload) {}
}
