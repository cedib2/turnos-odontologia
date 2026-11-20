package com.turnosodontologia.application;

import com.turnosodontologia.domain.agenda.Profesional;
import com.turnosodontologia.domain.agenda.Sillon;
import com.turnosodontologia.domain.agenda.Tratamiento;
import com.turnosodontologia.domain.agenda.Turno;
import com.turnosodontologia.domain.auditoria.AuditEvent;
import com.turnosodontologia.domain.identidad.DocumentoTipo;
import com.turnosodontologia.domain.identidad.Paciente;
import com.turnosodontologia.infrastructure.audit.InMemoryAuditEventRecorder;
import com.turnosodontologia.infrastructure.persistence.ConsultorioRepository;
import com.turnosodontologia.infrastructure.persistence.OcupacionRepository;
import com.turnosodontologia.infrastructure.persistence.PacienteRepository;
import com.turnosodontologia.infrastructure.persistence.ProfesionalRepository;
import com.turnosodontologia.infrastructure.persistence.SillonRepository;
import com.turnosodontologia.infrastructure.persistence.TratamientoRepository;
import com.turnosodontologia.infrastructure.persistence.TurnoRepository;
import com.turnosodontologia.support.PostgresFlywayTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CreacionTurnoIntegrationTest extends PostgresFlywayTest {

    private static final Instant INICIO = Instant.parse("2026-12-16T13:00:00Z");

    @Autowired
    private CreacionTurnoService service;

    @Autowired
    private TurnoRepository turnos;

    @Autowired
    private OcupacionRepository ocupaciones;

    @Autowired
    private PacienteRepository pacientes;

    @Autowired
    private ProfesionalRepository profesionales;

    @Autowired
    private SillonRepository sillones;

    @Autowired
    private ConsultorioRepository consultorios;

    @Autowired
    private TratamientoRepository tratamientos;

    @Autowired
    private InMemoryAuditEventRecorder recorder;

    @BeforeEach
    void limpiarEventos() {
        recorder.clear();
    }

    @Test
    void creaTurnoConDuracionDelCatalogoYLoDejaVisibleEnLaAgenda() {
        Long paciente = crearPaciente("PAC-INT-1");
        Long profesional = crearProfesional("MP-INT-1");
        Long sillon = crearSillon("S-INT-1");
        crearTratamiento("LIMPIEZA-INT", 45);

        Turno creado = service.crearTurno(paciente, profesional, sillon, "LIMPIEZA-INT",
                INICIO, "fixture-estado", "fixture-origen");

        Turno desdeAgenda = turnos.findById(creado.getId()).orElseThrow();
        assertEquals(INICIO, desdeAgenda.getInicio());
        assertEquals(INICIO.plus(45, ChronoUnit.MINUTES), desdeAgenda.getFin());
        assertEquals(45, desdeAgenda.getDuracionMinutos());
        assertEquals(1, ocupaciones.countByActivoTrue());
        assertEquals(1, recorder.eventos().size());
        AuditEvent evento = recorder.eventos().get(0);
        assertEquals("turno.creado", evento.tipo());
        assertEquals("recepcionista", evento.actor().nombre());
        assertEquals("SIMULADO", evento.actor().modo());
        assertNotNull(evento.correlationId());
    }

    @Test
    void solapamientoRechazadoNoDejaTurnoNiEventoDeConfirmacion() {
        Long paciente = crearPaciente("PAC-INT-2");
        Long profesional = crearProfesional("MP-INT-2");
        Long sillon = crearSillon("S-INT-2");
        crearTratamiento("LIMPIEZA-INT-2", 60);

        service.crearTurno(paciente, profesional, sillon, "LIMPIEZA-INT-2",
                INICIO, "fixture-estado", "fixture-origen");
        recorder.clear();

        assertThrows(DataIntegrityViolationException.class, () ->
                service.crearTurno(paciente, profesional, sillon, "LIMPIEZA-INT-2",
                        INICIO.plus(30, ChronoUnit.MINUTES), "fixture-estado", "fixture-origen"));

        assertEquals(1, turnos.count());
        assertEquals(1, ocupaciones.countByActivoTrue());
        assertEquals(0, recorder.eventos().size());
    }

    private Long crearPaciente(String documento) {
        Paciente paciente = pacientes.saveAndFlush(new Paciente(DocumentoTipo.DNI,
                documento, "Paciente", "Fixture", "555"));
        return paciente.getId();
    }

    private Long crearProfesional(String matricula) {
        Profesional profesional = profesionales.saveAndFlush(new Profesional(
                matricula, "Profesional", "Fixture"));
        return profesional.getId();
    }

    private Long crearSillon(String numero) {
        Sillon sillon = sillones.saveAndFlush(new Sillon(numero,
                consultorios.findById(1L).orElseThrow()));
        return sillon.getId();
    }

    private void crearTratamiento(String codigo, int duracion) {
        tratamientos.saveAndFlush(new Tratamiento(codigo, "Fixture", duracion));
    }
}
