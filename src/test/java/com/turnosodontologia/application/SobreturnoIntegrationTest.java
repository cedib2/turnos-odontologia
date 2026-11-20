package com.turnosodontologia.application;

import com.turnosodontologia.domain.agenda.HorarioAtencion;
import com.turnosodontologia.domain.agenda.Sillon;
import com.turnosodontologia.domain.agenda.Turno;
import com.turnosodontologia.domain.identidad.DocumentoTipo;
import com.turnosodontologia.domain.identidad.Paciente;
import com.turnosodontologia.infrastructure.audit.InMemoryAuditEventRecorder;
import com.turnosodontologia.infrastructure.persistence.BloqueoRepository;
import com.turnosodontologia.infrastructure.persistence.ConsultorioRepository;
import com.turnosodontologia.infrastructure.persistence.HorarioAtencionRepository;
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

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SobreturnoIntegrationTest extends PostgresFlywayTest {

    private static final ZoneId ZONA = ZoneId.of("America/Argentina/Buenos_Aires");
    private static final LocalDate FECHA = LocalDate.of(2026, 12, 24);
    private static final Instant INICIO = local(13, 0);
    private static final Instant DESTINO = local(13, 30);
    private static final Instant BLOQUEO = local(14, 0);

    @Autowired
    private SobreturnoService service;

    @Autowired
    private CreacionTurnoService creacionTurnoService;

    @Autowired
    private BloqueoService bloqueoService;

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
    private HorarioAtencionRepository horarios;

    @Autowired
    private BloqueoRepository bloqueos;

    @Autowired
    private InMemoryAuditEventRecorder recorder;

    @BeforeEach
    void sembrarHorario() {
        horarios.saveAndFlush(new HorarioAtencion(null, FECHA.getDayOfWeek().getValue(),
                LocalTime.of(13, 0), LocalTime.of(15, 0)));
        recorder.clear();
    }

    @Test
    void mueveTurnoADestinoLibreYRegistraMotivoYActor() {
        Long[] contexto = crearContexto();
        Turno turno = creacionTurnoService.crearTurno(contexto[0], contexto[1], contexto[2],
                "TRAT-SOB-INT-1", INICIO, "fixture-estado", "fixture-origen");
        recorder.clear();

        Turno resultado = service.reprogramarConSobreturno(turno.getId(), DESTINO,
                "fixture-motivo", "recepcionista");

        assertEquals(DESTINO, turnos.findById(resultado.getId()).orElseThrow().getInicio());
        assertEquals(1, ocupaciones.countByActivoTrue());
        assertEquals(1, recorder.eventos().size());
        assertEquals("turno.sobreturno", recorder.eventos().get(0).tipo());
        assertEquals("fixture-motivo", recorder.eventos().get(0).motivo());
        assertEquals("recepcionista", recorder.eventos().get(0).actor().nombre());
        assertEquals("SIMULADO", recorder.eventos().get(0).actor().modo());
    }

    @Test
    void destinoOcupadoPorBloqueoNoModificaElTurnoNiEmiteEvento() {
        Long[] contexto = crearContexto();
        Turno turno = creacionTurnoService.crearTurno(contexto[0], contexto[1], contexto[2],
                "TRAT-SOB-INT-2", INICIO, "fixture-estado", "fixture-origen");
        bloqueoService.crearBloqueo(null, contexto[2], BLOQUEO, BLOQUEO.plusSeconds(1800),
                "fixture-bloqueo");
        recorder.clear();

        assertThrows(IllegalArgumentException.class, () ->
                service.reprogramarConSobreturno(turno.getId(), BLOQUEO,
                        "fixture-motivo", "recepcionista"));

        assertEquals(INICIO, turnos.findById(turno.getId()).orElseThrow().getInicio());
        assertEquals(2, ocupaciones.countByActivoTrue());
        assertEquals(0, recorder.eventos().size());
        assertEquals(1, bloqueos.count());
    }

    private Long[] crearContexto() {
        Paciente paciente = pacientes.saveAndFlush(new Paciente(DocumentoTipo.DNI,
                "PAC-SOB-INT", "Paciente", "Fixture", "555"));
        Long profesional = profesionales.saveAndFlush(
                new com.turnosodontologia.domain.agenda.Profesional(
                        "MP-SOB-INT", "Profesional", "Fixture")).getId();
        Sillon sillon = sillones.saveAndFlush(new Sillon("S-SOB-INT",
                consultorios.findById(1L).orElseThrow()));
        tratamientos.saveAndFlush(new com.turnosodontologia.domain.agenda.Tratamiento(
                "TRAT-SOB-INT-1", "Fixture", 30));
        tratamientos.saveAndFlush(new com.turnosodontologia.domain.agenda.Tratamiento(
                "TRAT-SOB-INT-2", "Fixture", 30));
        return new Long[]{paciente.getId(), profesional, sillon.getId()};
    }

    private static Instant local(int hora, int minuto) {
        return FECHA.atTime(hora, minuto).atZone(ZONA).toInstant();
    }
}
