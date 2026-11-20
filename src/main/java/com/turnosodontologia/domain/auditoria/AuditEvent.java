package com.turnosodontologia.domain.auditoria;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public record AuditEvent(String tipo, String entidad, Long entidadId, ActorSimulado actor,
                         Instant occurredAt, String motivo, Map<String, Object> metadata,
                         String correlationId) {

    public AuditEvent {
        metadata = metadata == null
                ? Map.of()
                : Collections.unmodifiableMap(new HashMap<>(metadata));
    }

    public static AuditEvent creacionTurno(Long entidadId, ActorSimulado actor,
                                           String motivo, Map<String, Object> metadata) {
        return de("turno.creado", "turno", entidadId, actor, motivo, metadata,
                UUID.randomUUID().toString());
    }

    public static AuditEvent de(String tipo, String entidad, Long entidadId, ActorSimulado actor,
                                String motivo, Map<String, Object> metadata, String correlationId) {
        return new AuditEvent(tipo, entidad, entidadId, actor, Instant.now(), motivo,
                metadata, correlationId);
    }
}
