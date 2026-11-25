package com.turnosodontologia.export;

import com.turnosodontologia.domain.auditoria.ActorSimulado;
import com.turnosodontologia.domain.auditoria.AuditEvent;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class ExportAuditEvents {

    public static final String EXPORT_REQUESTED = "EXPORT_REQUESTED";
    public static final String EXPORT_COMPLETED = "EXPORT_COMPLETED";
    public static final String EXPORT_FAILED = "EXPORT_FAILED";
    public static final String BACKUP_EXECUTED = "BACKUP_EXECUTED";
    public static final String BACKUP_FAILED = "BACKUP_FAILED";
    public static final String RESTORE_DRY_RUN_VERIFIED = "RESTORE_DRY_RUN_VERIFIED";
    public static final String RESTORE_EXECUTED = "RESTORE_EXECUTED";

    public static final Set<String> TIPOS = Set.of(EXPORT_REQUESTED, EXPORT_COMPLETED,
            EXPORT_FAILED, BACKUP_EXECUTED, BACKUP_FAILED,
            RESTORE_DRY_RUN_VERIFIED, RESTORE_EXECUTED);

    private ExportAuditEvents() {
    }

    public static AuditEvent de(String tipo, String correlationId, ActorSimulado actor,
                                String motivo, Map<String, Object> metadata,
                                Instant occurredAt, String resultado) {
        Map<String, Object> enriquecida = new LinkedHashMap<>();
        enriquecida.put("correlationId", correlationId);
        enriquecida.put("actor", actor == null ? null : actor.nombre());
        enriquecida.put("occurredAt", occurredAt.toString());
        enriquecida.put("motivo", motivo);
        enriquecida.put("resultado", resultado);
        if (metadata != null) {
            enriquecida.putAll(metadata);
        }
        return new AuditEvent(tipo, "export_backup_restore", null, actor, occurredAt,
                motivo, enriquecida, correlationId);
    }
}
