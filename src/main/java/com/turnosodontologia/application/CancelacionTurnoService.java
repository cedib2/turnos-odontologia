package com.turnosodontologia.application;

import com.turnosodontologia.application.notificaciones.AvisosTurno;
import com.turnosodontologia.domain.agenda.Ocupacion;
import com.turnosodontologia.domain.agenda.Turno;
import com.turnosodontologia.domain.auditoria.ActorSimulado;
import com.turnosodontologia.domain.auditoria.AuditEvent;
import com.turnosodontologia.infrastructure.persistence.OcupacionRepository;
import com.turnosodontologia.infrastructure.persistence.TurnoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Cancelación de un turno por el staff (recepción). No aplica la ventana de 48 hs hábiles: recepción es quien
 * atiende los cambios que el paciente ya no puede hacer por link (RN-AG-05). Libera el hueco en la misma
 * transacción y audita {@code turno.cancelado} con motivo e {@code inicioAnterior} (contrato con C-22).
 */
@Service
@Transactional
public class CancelacionTurnoService {

    private final TurnoRepository turnos;
    private final OcupacionRepository ocupaciones;
    private final CurrentActorProvider actores;
    private final AuditEventPublisher publisher;
    private final AvisosTurno avisos;

    public CancelacionTurnoService(TurnoRepository turnos, OcupacionRepository ocupaciones,
                                   CurrentActorProvider actores, AuditEventPublisher publisher,
                                   AvisosTurno avisos) {
        this.turnos = turnos;
        this.ocupaciones = ocupaciones;
        this.actores = actores;
        this.publisher = publisher;
        this.avisos = avisos;
    }

    public Turno cancelar(Long turnoId, String motivo) {
        if (motivo == null || motivo.isBlank()) {
            throw new IllegalArgumentException("El motivo de la cancelación es obligatorio");
        }
        ActorSimulado actor = actores.current();
        Turno turno = turnos.findById(turnoId)
                .orElseThrow(() -> new IllegalArgumentException("Turno inexistente: " + turnoId));
        if (!turno.isActivo()) {
            throw new IllegalStateException("El turno " + turnoId + " ya no está activo");
        }
        ocupaciones.findByTurnoIdAndActivoTrue(turnoId).ifPresent(ocupacion -> {
            ocupacion.desactivar();
            ocupaciones.saveAndFlush(ocupacion);
        });
        String inicioAnterior = turno.getInicio().toString();
        turno.cancelar(actor.nombre());
        Turno guardado = turnos.saveAndFlush(turno);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("canal", "staff");
        metadata.put("inicioAnterior", inicioAnterior);
        publisher.publish(AuditEvent.de("turno.cancelado", "turno", guardado.getId(), actor, motivo.trim(),
                metadata, UUID.randomUUID().toString()));
        avisos.cancelado(guardado.getId());
        return guardado;
    }
}
