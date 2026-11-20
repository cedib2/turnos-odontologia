package com.turnosodontologia.application;

import com.turnosodontologia.domain.agenda.Bloqueo;
import com.turnosodontologia.domain.agenda.Consultorio;
import com.turnosodontologia.domain.agenda.Profesional;
import com.turnosodontologia.domain.agenda.Sillon;
import com.turnosodontologia.domain.agenda.Tratamiento;
import com.turnosodontologia.domain.identidad.DocumentoTipo;
import com.turnosodontologia.domain.identidad.Paciente;
import com.turnosodontologia.infrastructure.audit.InMemoryAuditEventRecorder;
import com.turnosodontologia.infrastructure.persistence.BloqueoRepository;
import com.turnosodontologia.infrastructure.persistence.ConsultorioRepository;
import com.turnosodontologia.infrastructure.persistence.OcupacionRepository;
import com.turnosodontologia.infrastructure.persistence.PacienteRepository;
import com.turnosodontologia.infrastructure.persistence.ProfesionalRepository;
import com.turnosodontologia.infrastructure.persistence.SillonRepository;
import com.turnosodontologia.infrastructure.persistence.TratamientoRepository;
import com.turnosodontologia.support.PostgresFlywayTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BloqueoIntegrationTest extends PostgresFlywayTest {

    private static final Instant DESDE = Instant.parse("2026-12-20T13:00:00Z");
    private static final Instant HASTA = Instant.parse("2026-12-20T14:00:00Z");

    @Autowired
    private BloqueoService bloqueoService;

    @Autowired
    private CreacionTurnoService creacionTurnoService;

    @Autowired
    private BloqueoRepository bloqueos;

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
    void limpiar() {
        recorder.clear();
    }

    @Test
    void creaBloqueoDeSillonYOcupacion() {
        Long sillon = crearSillon("S-BLOQ-INT-1");

        Bloqueo bloqueo = bloqueoService.crearBloqueo(null, sillon, DESDE, HASTA, "fixture-motivo");

        assertEquals(1, bloqueos.count());
        assertEquals(1, ocupaciones.countByActivoTrue());
        assertEquals(DESDE, bloqueos.findById(bloqueo.getId()).orElseThrow().getDesde());
        assertEquals(1, recorder.eventos().size());
        assertEquals("bloqueo.creado", recorder.eventos().get(0).tipo());
    }

    @Test
    void bloqueoQueSolapaTurnoEsRechazadoSinEvento() {
        Paciente paciente = pacientes.saveAndFlush(new Paciente(DocumentoTipo.DNI,
                "PAC-BLOQ-INT-2", "Paciente", "Fixture", "555"));
        Profesional profesional = profesionales.saveAndFlush(new Profesional(
                "MP-BLOQ-INT-2", "Profesional", "Fixture"));
        Sillon sillon = sillones.saveAndFlush(new Sillon("S-BLOQ-INT-2",
                consultorios.findById(1L).orElseThrow()));
        tratamientos.saveAndFlush(new Tratamiento("TRAT-BLOQ-INT-2", "Fixture", 30));
        creacionTurnoService.crearTurno(paciente.getId(), profesional.getId(), sillon.getId(),
                "TRAT-BLOQ-INT-2", DESDE, "fixture-estado", "fixture-origen");
        recorder.clear();

        assertThrows(DataIntegrityViolationException.class, () ->
                bloqueoService.crearBloqueo(profesional.getId(), sillon.getId(),
                        DESDE.plusSeconds(900), HASTA, "fixture-motivo"));

        assertEquals(0, bloqueos.count());
        assertEquals(1, ocupaciones.countByActivoTrue());
        assertEquals(0, recorder.eventos().size());
    }

    private Long crearSillon(String numero) {
        return sillones.saveAndFlush(new Sillon(numero,
                consultorios.findById(1L).orElseThrow())).getId();
    }
}
