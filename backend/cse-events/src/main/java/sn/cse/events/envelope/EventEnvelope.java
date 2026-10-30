package sn.cse.events.envelope;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.time.Instant;
import java.util.UUID;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
public record EventEnvelope<T>(
        UUID eventId,
        EventType type,
        String version,
        Instant occurredAt,
        String aggregateId,
        String correlationId,
        String actorId,
        T payload
) {
    public static <T> EventEnvelope<T> of(EventType type, String aggregateId, String correlationId, String actorId, T payload) {
        return new EventEnvelope<>(
                UUID.randomUUID(),
                type,
                "1.0",
                Instant.now(),
                aggregateId,
                correlationId != null ? correlationId : UUID.randomUUID().toString(),
                actorId,
                payload
        );
    }
}
