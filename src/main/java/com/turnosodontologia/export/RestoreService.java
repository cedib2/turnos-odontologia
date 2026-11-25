package com.turnosodontologia.export;

import com.turnosodontologia.application.AuditEventPublisher;
import com.turnosodontologia.audit.AuditoriaIntegridadService;
import com.turnosodontologia.domain.auditoria.ActorSimulado;

import java.time.Instant;
import java.util.Map;

public final class RestoreService {

    private final AuditEventPublisher eventos;
    private final AuditoriaIntegridadService integridad;
    private final RestoreCountPort conteos;

    public RestoreService(AuditEventPublisher eventos, AuditoriaIntegridadService integridad,
                          RestoreCountPort conteos) {
        this.eventos = eventos;
        this.integridad = integridad;
        this.conteos = conteos;
    }

    public void restaurar(RestoreDestino destino, ExportSnapshot snapshot,
                          String correlationId, ActorSimulado actor) {
        validarDestino(destino);
        validarCadena();
        validarConteos(snapshot);

        eventos.publish(ExportAuditEvents.de(ExportAuditEvents.RESTORE_DRY_RUN_VERIFIED,
                correlationId, actor, "cadena y conteos verificados",
                Map.of("destino", destino.nombre(), "entorno", "aislado"),
                Instant.now(), "VERIFICADO"));
        eventos.publish(ExportAuditEvents.de(ExportAuditEvents.RESTORE_EXECUTED,
                correlationId, actor, "restore en entorno aislado",
                Map.of("destino", destino.nombre(), "entorno", "aislado"),
                Instant.now(), "OK"));
    }

    private void validarDestino(RestoreDestino destino) {
        if (destino == null || !destino.aislado()) {
            throw new IllegalArgumentException("Destino de producción rechazado: restore requiere aislado");
        }
    }

    private void validarCadena() {
        if (!integridad.verificar().valida()) {
            throw new IllegalArgumentException("Cadena de auditoría inválida: restore cancelado");
        }
    }

    private void validarConteos(ExportSnapshot snapshot) {
        for (Map.Entry<String, Integer> esperado : snapshot.conteos().entrySet()) {
            int obtenido = conteos.contar(esperado.getKey());
            if (obtenido != esperado.getValue()) {
                throw new IllegalArgumentException("Conteo divergente en " + esperado.getKey()
                        + ": esperado " + esperado.getValue() + ", obtenido " + obtenido);
            }
        }
    }
}
