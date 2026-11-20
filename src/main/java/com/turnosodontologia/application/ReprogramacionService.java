package com.turnosodontologia.application;

import com.turnosodontologia.domain.agenda.Ocupacion;
import com.turnosodontologia.domain.agenda.Turno;
import com.turnosodontologia.domain.auditoria.ActorSimulado;
import com.turnosodontologia.domain.auditoria.AuditEvent;
import com.turnosodontologia.infrastructure.persistence.OcupacionRepository;
import com.turnosodontologia.infrastructure.persistence.TurnoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class ReprogramacionService {

    private final TurnoRepository turnos;
    private final OcupacionRepository ocupaciones;
    private final ConfiguracionSchedulingService configuracion;
    private final CurrentActorProvider actorProvider;
    private final AuditEventPublisher publisher;

    public ReprogramacionService(TurnoRepository turnos,
                                 OcupacionRepository ocupaciones,
                                 ConfiguracionSchedulingService configuracion,
                                 CurrentActorProvider actorProvider,
                                 AuditEventPublisher publisher) {
        this.turnos = turnos;
        this.ocupaciones = ocupaciones;
        this.configuracion = configuracion;
        this.actorProvider = actorProvider;
        this.publisher = publisher;
    }

    public Turno reprogramar(Long turnoId, Instant nuevoInicio, String actor) {
        if (actor == null || actor.isBlank()) {
            throw new IllegalArgumentException("El actor de la reprogramación es obligatorio");
        }
        ActorSimulado simulado = actorProvider.current();
        if (!simulado.nombre().equals(actor) && !ActorSimulado.system().nombre().equals(actor)) {
            throw new IllegalArgumentException("Actor simulado no admitido: " + actor);
        }
        ActorSimulado autor = new ActorSimulado(actor, simulado.modo());

        Turno turno = turnos.findById(turnoId)
                .orElseThrow(() -> new IllegalArgumentException("Turno inexistente: " + turnoId));
        Instant nuevoFin = nuevoInicio.plus(turno.getDuracionMinutos(), ChronoUnit.MINUTES);
        LocalDate fecha = nuevoInicio.atZone(configuracion.zonaHoraria()).toLocalDate();

        if (configuracion.esFeriado(fecha)) {
            throw new IllegalArgumentException("El destino cae en feriado: " + fecha);
        }
        if (!configuracion.estaDentroDeHorario(turno.getProfesional().getId().intValue(),
                nuevoInicio, nuevoFin)) {
            throw new IllegalArgumentException("El destino está fuera del horario de atención");
        }

        Long profesionalId = turno.getProfesional().getId();
        Long sillonId = turno.getSillon().getId();
        boolean ocupado = ocupaciones.findActivosQueSolapan(profesionalId, sillonId,
                        nuevoInicio, nuevoFin)
                .stream()
                .anyMatch(ocupacion -> !turnoId.equals(ocupacion.getTurnoId()));
        if (ocupado) {
            throw new IllegalArgumentException("El destino de la reprogramación está ocupado");
        }

        Ocupacion original = ocupaciones.findByTurnoIdAndActivoTrue(turnoId)
                .orElseThrow(() -> new IllegalStateException("El turno no tiene ocupación activa"));
        original.desactivar();
        ocupaciones.saveAndFlush(original);

        turno.reprogramar(nuevoInicio, nuevoFin, actor);
        Turno guardado = turnos.saveAndFlush(turno);
        ocupaciones.saveAndFlush(Ocupacion.paraTurno(guardado.getId(),
                profesionalId, sillonId, nuevoInicio, nuevoFin));

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("actorAutorizo", actor);
        metadata.put("nuevoInicio", nuevoInicio.toString());
        metadata.put("nuevoFin", nuevoFin.toString());
        publicarDespuesDeConfirmar(AuditEvent.de("turno.reprogramado", "turno", guardado.getId(),
                autor, "reprogramación", metadata, UUID.randomUUID().toString()));
        return guardado;
    }

    private void publicarDespuesDeConfirmar(AuditEvent evento) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publisher.publish(evento);
                }
            });
        } else {
            publisher.publish(evento);
        }
    }
}
