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

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class SobreturnoService {

    private final TurnoRepository turnos;
    private final OcupacionRepository ocupaciones;
    private final ConfiguracionSchedulingService configuracion;
    private final CurrentActorProvider actorProvider;
    private final AuditEventPublisher publisher;
    private final AvisosTurno avisos;

    public SobreturnoService(TurnoRepository turnos,
                             OcupacionRepository ocupaciones,
                             ConfiguracionSchedulingService configuracion,
                             CurrentActorProvider actorProvider,
                             AuditEventPublisher publisher,
                             AvisosTurno avisos) {
        this.turnos = turnos;
        this.ocupaciones = ocupaciones;
        this.configuracion = configuracion;
        this.actorProvider = actorProvider;
        this.publisher = publisher;
        this.avisos = avisos;
    }

    public Turno reprogramarConSobreturno(Long turnoId, Instant nuevoInicio,
                                          String motivo, String actor) {
        if (motivo == null || motivo.isBlank()) {
            throw new IllegalArgumentException("El motivo del sobreturno es obligatorio");
        }
        if (actor == null || actor.isBlank()) {
            throw new IllegalArgumentException("El actor del sobreturno es obligatorio");
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
            throw new IllegalArgumentException("El destino del sobreturno está ocupado");
        }

        Ocupacion original = ocupaciones.findByTurnoIdAndActivoTrue(turnoId)
                .orElseThrow(() -> new IllegalStateException("El turno no tiene ocupación activa"));
        original.desactivar();
        ocupaciones.saveAndFlush(original);

        turno.reprogramar(nuevoInicio, nuevoFin, actor);
        turno.marcarSobreturno(motivo, actor);
        Turno guardado = turnos.saveAndFlush(turno);

        ocupaciones.saveAndFlush(Ocupacion.paraTurno(guardado.getId(),
                profesionalId, sillonId, nuevoInicio, nuevoFin));

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("motivo", motivo);
        metadata.put("actorAutorizo", actor);
        metadata.put("nuevoInicio", nuevoInicio.toString());
        metadata.put("nuevoFin", nuevoFin.toString());
        publisher.publish(AuditEvent.de("turno.sobreturno", "turno", guardado.getId(),
                autor, motivo, metadata, UUID.randomUUID().toString()));
        avisos.reprogramado(guardado.getId());
        return guardado;
    }
}
