package com.turnosodontologia.application.cumplimiento;

import com.turnosodontologia.application.AuditEventPublisher;
import com.turnosodontologia.application.CurrentActorProvider;
import com.turnosodontologia.domain.auditoria.ActorSimulado;
import com.turnosodontologia.domain.auditoria.AuditEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Traza append-only (C-05) de cada exportación, respaldo y restore. */
final class CumplimientoAuditoria {

    static final String ENTIDAD = "cumplimiento_operacion";

    private CumplimientoAuditoria() {
    }

    static void publicar(AuditEventPublisher publisher, ActorSimulado actor, String tipoEvento,
                         OperacionCumplimiento operacion, String motivo) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("tipoOperacion", operacion.tipo());
        metadata.put("alcance", operacion.alcance());
        metadata.put("estado", operacion.estado());
        metadata.put("formato", operacion.formato());
        if (operacion.pacienteId() != null) {
            metadata.put("pacienteId", operacion.pacienteId());
        }
        if (operacion.checksumSha256() != null) {
            metadata.put("checksumSha256", operacion.checksumSha256());
        }
        if (operacion.filas() != null) {
            metadata.put("filas", operacion.filas());
        }
        if (operacion.respaldoOrigenId() != null) {
            metadata.put("respaldoOrigenId", operacion.respaldoOrigenId());
        }
        if (operacion.error() != null) {
            metadata.put("error", operacion.error());
        }
        publisher.publish(AuditEvent.de(tipoEvento, ENTIDAD, operacion.id(), actor,
                motivo == null ? "" : motivo, metadata, UUID.randomUUID().toString()));
    }

    static ActorSimulado actorActual(CurrentActorProvider provider) {
        return provider.current();
    }
}
