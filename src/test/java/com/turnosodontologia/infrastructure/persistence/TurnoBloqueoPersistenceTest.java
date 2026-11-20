package com.turnosodontologia.infrastructure.persistence;

import com.turnosodontologia.domain.agenda.Bloqueo;
import com.turnosodontologia.domain.agenda.Consultorio;
import com.turnosodontologia.domain.agenda.Ocupacion;
import com.turnosodontologia.domain.agenda.Profesional;
import com.turnosodontologia.domain.agenda.Sillon;
import com.turnosodontologia.domain.agenda.Tratamiento;
import com.turnosodontologia.domain.agenda.Turno;
import com.turnosodontologia.domain.identidad.DocumentoTipo;
import com.turnosodontologia.domain.identidad.Paciente;
import com.turnosodontologia.support.PostgresFlywayTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TurnoBloqueoPersistenceTest extends PostgresFlywayTest {

    private static final Instant INICIO = Instant.parse("2026-12-10T13:00:00Z");

    @Autowired
    private TurnoRepository turnos;

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

    @Test
    void persisteTurnoConMapeoSnakeCaseAuditoriaYSoftDelete() {
        Paciente paciente = pacientes.saveAndFlush(
                new Paciente(DocumentoTipo.DNI, "TURNO-1", "Paciente", "Fixture", "555"));
        Profesional profesional = profesionales.saveAndFlush(
                new Profesional("MP-TURNO-1", "Profesional", "Fixture"));
        Sillon sillon = sillones.saveAndFlush(new Sillon("S-TURNO-1",
                consultorios.findById(1L).orElseThrow()));
        Tratamiento tratamiento = tratamientos.saveAndFlush(
                new Tratamiento("TRAT-TURNO-1", "Fixture", 30));

        Turno turno = new Turno(paciente, profesional, sillon, tratamiento,
                INICIO, INICIO.plusSeconds(1800), 30,
                "fixture-estado", "fixture-origen", null);
        turno.registrarCreacion("recepcionista");
        turnos.saveAndFlush(turno);

        Turno cargado = turnos.findById(turno.getId()).orElseThrow();
        assertNotNull(cargado.getId());
        assertEquals(INICIO, cargado.getInicio());
        assertEquals(INICIO.plusSeconds(1800), cargado.getFin());
        assertEquals(30, cargado.getDuracionMinutos());
        assertEquals("recepcionista", cargado.getCreatedBy());
        assertTrue(cargado.isActivo());

        cargado.desactivar("recepcionista");
        turnos.saveAndFlush(cargado);
        assertFalse(turnos.findById(turno.getId()).orElseThrow().isActivo());
        assertEquals(1, turnos.count());
    }

    @Test
    void persisteBloqueoYOcupacionConSoftDelete() {
        Sillon sillon = sillones.saveAndFlush(new Sillon("S-BLOQ-1",
                consultorios.findById(1L).orElseThrow()));
        Bloqueo bloqueo = new Bloqueo(null, sillon.getId(), INICIO, INICIO.plusSeconds(3600),
                "fixture-motivo");
        bloqueo.registrarCreacion("system");
        bloqueos.saveAndFlush(bloqueo);

        Ocupacion ocupacion = Ocupacion.paraBloqueo(bloqueo.getId(), null, sillon.getId(),
                INICIO, INICIO.plusSeconds(3600));
        ocupaciones.saveAndFlush(ocupacion);

        assertEquals(1, ocupaciones.countByActivoTrue());
        ocupacion.desactivar();
        ocupaciones.saveAndFlush(ocupacion);
        assertEquals(0, ocupaciones.countByActivoTrue());
        assertEquals(1, ocupaciones.count());
    }
}
