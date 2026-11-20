package com.turnosodontologia.application;

import com.turnosodontologia.domain.agenda.Sillon;
import com.turnosodontologia.domain.agenda.Turno;
import com.turnosodontologia.domain.identidad.DocumentoTipo;
import com.turnosodontologia.domain.identidad.Paciente;
import com.turnosodontologia.infrastructure.persistence.ConsultorioRepository;
import com.turnosodontologia.infrastructure.persistence.PacienteRepository;
import com.turnosodontologia.infrastructure.persistence.ProfesionalRepository;
import com.turnosodontologia.infrastructure.persistence.SillonRepository;
import com.turnosodontologia.infrastructure.persistence.TratamientoRepository;
import com.turnosodontologia.support.PostgresFlywayTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgendaVistaIntegrationTest extends PostgresFlywayTest {

    private static final ZoneId ZONA = ZoneId.of("America/Argentina/Buenos_Aires");
    private static final LocalDate LUNES = LocalDate.of(2026, 12, 28);
    private static final LocalDate MARTES = LUNES.plusDays(1);

    @Autowired
    private AgendaConsultaService consulta;

    @Autowired
    private CreacionTurnoService creacionTurnoService;

    @Autowired
    private BloqueoService bloqueoService;

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

    private Long profesionalUno;
    private Long profesionalDos;
    private Long sillonUno;
    private Long sillonDos;

    @BeforeEach
    void sembrar() {
        profesionalUno = crearProfesional("MP-VISTA-1");
        profesionalDos = crearProfesional("MP-VISTA-2");
        sillonUno = crearSillon("S-VISTA-1");
        sillonDos = crearSillon("S-VISTA-2");
        tratamientos.saveAndFlush(new com.turnosodontologia.domain.agenda.Tratamiento(
                "TRAT-VISTA", "Fixture", 30));
    }

    @Test
    void vistaDiariaFiltraPorProfesionalYSillonEnZonaArgentina() {
        crearTurno(profesionalUno, sillonUno, LUNES, 13, 0);
        crearTurno(profesionalDos, sillonDos, LUNES, 13, 30);
        crearTurno(profesionalUno, sillonUno, MARTES, 13, 0);
        bloqueoService.crearBloqueo(profesionalUno, sillonUno,
                local(LUNES, 14, 0), local(LUNES, 14, 30), "fixture-bloqueo");

        List<EntradaAgenda> porProfesional = consulta.porDia(profesionalUno, null, LUNES);
        assertEquals(2, porProfesional.size());
        assertTrue(porProfesional.stream().allMatch(entrada ->
                profesionalUno.equals(entrada.profesionalId())));

        List<EntradaAgenda> porSillon = consulta.porDia(null, sillonUno, LUNES);
        assertEquals(2, porSillon.size());
        assertTrue(porSillon.stream().allMatch(entrada -> sillonUno.equals(entrada.sillonId())));
    }

    @Test
    void vistaSemanalIncluyeSoloLosSieteDiasDeLaZona() {
        crearTurno(profesionalUno, sillonUno, LUNES, 13, 0);
        crearTurno(profesionalUno, sillonUno, LUNES.plusDays(6), 13, 0);
        crearTurno(profesionalUno, sillonUno, LUNES.plusDays(7), 13, 0);

        List<EntradaAgenda> entradas = consulta.porSemana(profesionalUno, null, LUNES);

        assertEquals(2, entradas.size());
        assertTrue(entradas.stream().noneMatch(entrada ->
                entrada.inicio().equals(local(LUNES.plusDays(7), 13, 0))));
    }

    private void crearTurno(Long profesional, Long sillon, LocalDate fecha, int hora, int minuto) {
        Paciente paciente = pacientes.saveAndFlush(new Paciente(DocumentoTipo.DNI,
                "PAC-VISTA-" + System.nanoTime(), "Paciente", "Fixture", "555"));
        creacionTurnoService.crearTurno(paciente.getId(), profesional, sillon,
                "TRAT-VISTA", local(fecha, hora, minuto), "fixture-estado", "fixture-origen");
    }

    private Long crearProfesional(String matricula) {
        return profesionales.saveAndFlush(new com.turnosodontologia.domain.agenda.Profesional(
                matricula, "Profesional", "Fixture")).getId();
    }

    private Long crearSillon(String numero) {
        Sillon sillon = sillones.saveAndFlush(new Sillon(numero,
                consultorios.findById(1L).orElseThrow()));
        return sillon.getId();
    }

    private static Instant local(LocalDate fecha, int hora, int minuto) {
        return fecha.atTime(hora, minuto).atZone(ZONA).toInstant();
    }
}
