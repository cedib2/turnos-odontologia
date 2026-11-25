package com.turnosodontologia.export;

import com.turnosodontologia.application.AuditEventPublisher;
import com.turnosodontologia.domain.auditoria.ActorSimulado;

import java.time.Instant;
import java.util.Map;

public final class BackupScheduleService {

    private final ExportOperationalConfig config;
    private final BackupStoragePort storage;
    private final AuditEventPublisher eventos;
    private final OpsAlertPort alertas;

    public BackupScheduleService(ExportOperationalConfig config, BackupStoragePort storage,
                                 AuditEventPublisher eventos, OpsAlertPort alertas) {
        this.config = config;
        this.storage = storage;
        this.eventos = eventos;
        this.alertas = alertas;
    }

    public void ejecutar(String tenantId, String artefactoId, byte[] contenido,
                         String correlationId, ActorSimulado actor) {
        try {
            storage.guardar(tenantId, artefactoId, contenido);
            eventos.publish(ExportAuditEvents.de(ExportAuditEvents.BACKUP_EXECUTED, correlationId,
                    actor, "respaldo diario", Map.of("tenantId", tenantId, "artefactoId", artefactoId),
                    Instant.now(), "OK"));
        } catch (RuntimeException fallo) {
            eventos.publish(ExportAuditEvents.de(ExportAuditEvents.BACKUP_FAILED, correlationId,
                    actor, fallo.getMessage(),
                    Map.of("tenantId", tenantId, "artefactoId", artefactoId),
                    Instant.now(), "FALLIDO"));
            alertas.alertar(new ExportAlert(correlationId, "BACKUP_FAILED",
                    "No se pudo completar el respaldo: " + fallo.getMessage(), Instant.now()));
            throw fallo;
        }
    }

    public void aplicarRetencion(LocalBackupStorage storageLocal, Instant ahora) {
        for (String tenantId : storageLocal.tenantIds()) {
            for (String artefactoId : storageLocal.listar(tenantId)) {
                BackupObjeto objeto = storageLocal.objeto(tenantId, artefactoId);
                Instant limite = objeto.guardadoEn().atZone(ExportOperationalConfig.ZONA)
                        .plusYears(config.retencionAnios()).toInstant();
                if (!ahora.isBefore(limite) && !objeto.expiradoLogico()) {
                    storageLocal.marcarExpiradoLogico(tenantId, artefactoId);
                }
            }
        }
    }
}
