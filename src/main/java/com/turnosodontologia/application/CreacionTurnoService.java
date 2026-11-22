package com.turnosodontologia.application;

import com.turnosodontologia.domain.agenda.Ocupacion;
import com.turnosodontologia.domain.agenda.Profesional;
import com.turnosodontologia.domain.agenda.Sillon;
import com.turnosodontologia.domain.agenda.Tratamiento;
import com.turnosodontologia.domain.agenda.Turno;
import com.turnosodontologia.domain.auditoria.ActorSimulado;
import com.turnosodontologia.domain.auditoria.AuditEvent;
import com.turnosodontologia.domain.identidad.Paciente;
import com.turnosodontologia.infrastructure.persistence.OcupacionRepository;
import com.turnosodontologia.infrastructure.persistence.PacienteRepository;
import com.turnosodontologia.infrastructure.persistence.ProfesionalRepository;
import com.turnosodontologia.infrastructure.persistence.SillonRepository;
import com.turnosodontologia.infrastructure.persistence.TratamientoRepository;
import com.turnosodontologia.infrastructure.persistence.TurnoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

@Service
@Transactional
public class CreacionTurnoService {

    private final PacienteRepository pacientes;
    private final ProfesionalRepository profesionales;
    private final SillonRepository sillones;
    private final TratamientoRepository tratamientos;
    private final TurnoRepository turnos;
    private final OcupacionRepository ocupaciones;
    private final ConfiguracionSchedulingService configuracion;
    private final CurrentActorProvider actorProvider;
    private final AuditEventPublisher publisher;

    public CreacionTurnoService(PacienteRepository pacientes,
                                ProfesionalRepository profesionales,
                                SillonRepository sillones,
                                TratamientoRepository tratamientos,
                                TurnoRepository turnos,
                                OcupacionRepository ocupaciones,
                                ConfiguracionSchedulingService configuracion,
                                CurrentActorProvider actorProvider,
                                AuditEventPublisher publisher) {
        this.pacientes = pacientes;
        this.profesionales = profesionales;
        this.sillones = sillones;
        this.tratamientos = tratamientos;
        this.turnos = turnos;
        this.ocupaciones = ocupaciones;
        this.configuracion = configuracion;
        this.actorProvider = actorProvider;
        this.publisher = publisher;
    }

    public Turno crearTurno(Long pacienteId, Long profesionalId, Long sillonId,
                            String codigoTratamiento, Instant inicio,
                            String estado, String origen) {
        validarNoVacio(estado, "estado");
        validarNoVacio(origen, "origen");

        int duracion = configuracion.duracionMinutos(codigoTratamiento);
        if (duracion <= 0) {
            throw new IllegalArgumentException("La duración del tratamiento debe ser positiva");
        }

        Paciente paciente = pacientes.findById(pacienteId)
                .orElseThrow(() -> new IllegalArgumentException("Paciente inexistente: " + pacienteId));
        Profesional profesional = profesionales.findById(profesionalId)
                .orElseThrow(() -> new IllegalArgumentException("Profesional inexistente: " + profesionalId));
        Sillon sillon = sillones.findById(sillonId)
                .orElseThrow(() -> new IllegalArgumentException("Sillón inexistente: " + sillonId));
        Tratamiento tratamiento = tratamientos.findByCodigo(codigoTratamiento)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Tratamiento inexistente: " + codigoTratamiento));
        Instant fin = inicio.plus(duracion, ChronoUnit.MINUTES);

        ActorSimulado actor = actorProvider.current();
        Turno turno = new Turno(paciente, profesional, sillon, tratamiento,
                inicio, fin, duracion, estado, origen, null);
        turno.registrarCreacion(actor.nombre());
        Turno guardado = turnos.saveAndFlush(turno);

        ocupaciones.saveAndFlush(Ocupacion.paraTurno(guardado.getId(),
                profesional.getId(), sillon.getId(), inicio, fin));

        publisher.publish(AuditEvent.creacionTurno(guardado.getId(), actor, "",
                Map.of("estado", estado, "origen", origen,
                        "codigoTratamiento", codigoTratamiento, "duracionMinutos", duracion)));
        return guardado;
    }

    private void validarNoVacio(String valor, String nombre) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(nombre + " no puede estar vacío");
        }
    }
}
