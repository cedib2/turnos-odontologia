package com.turnosodontologia.application;

import com.turnosodontologia.domain.agenda.HorarioAtencion;
import com.turnosodontologia.domain.agenda.Sillon;
import com.turnosodontologia.domain.agenda.Turno;
import com.turnosodontologia.domain.identidad.DocumentoTipo;
import com.turnosodontologia.domain.identidad.Paciente;
import com.turnosodontologia.infrastructure.persistence.ConsultorioRepository;
import com.turnosodontologia.infrastructure.persistence.HorarioAtencionRepository;
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
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgendaConsultaIntegrationTest extends PostgresFlywayTest {

    private static final ZoneId ZONA = ZoneId.of("America/Argentina/Buenos_Aires");
    private static final LocalDate FECHA = LocalDate.of(2026, 12, 26);
    private static final Instant TRECE = local(13, 0);
    private static final Instant TRECE_MEDIA = local(13, 30);
    private static final Instant CATORCE = local(14, 0);

    @Autowired
    private AgendaConsultaService consulta;

    @Autowired
    private CreacionTurnoService creacionTurnoService;

    @Autowired
    private ReprogramacionService reprogramacionService;

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

    @Autowired
    private HorarioAtencionRepository horarios;

    @BeforeEach
    void sembrar() {
        horarios.saveAndFlush(new HorarioAtencion(null, FECHA.getDayOfWeek().getValue(),
                LocalTime.of(13, 0), LocalTime.of(15, 0)));
        tratamientos.saveAndFlush(new com.turnosodontologia.domain.agenda.Tratamiento(
                "TRAT-WRITEBACK", "Fixture", 30));
    }

    @Test
    void laConsultaReflejaCreacionYReprogramacionSinReplicaExterna() {
        Long sillon = crearSillon("S-WB-1");
        Turno turno = crearTurno(sillon, TRECE);

        List<EntradaAgenda> antes = consulta.consultar(null, sillon, TRECE, CATORCE);
        assertTrue(antes.stream().anyMatch(entrada ->
                "turno".equals(entrada.tipo()) && entrada.inicio().equals(TRECE)));

        reprogramacionService.reprogramar(turno.getId(), TRECE_MEDIA, "recepcionista");

        List<EntradaAgenda> despues = consulta.consultar(null, sillon, TRECE, CATORCE);
        assertFalse(despues.stream().anyMatch(entrada -> entrada.inicio().equals(TRECE)));
        assertTrue(despues.stream().anyMatch(entrada ->
                "turno".equals(entrada.tipo()) && entrada.inicio().equals(TRECE_MEDIA)));
    }

    @Test
    void laConsultaReflejaElBloqueoCreado() {
        Long sillon = crearSillon("S-WB-2");
        bloqueoService.crearBloqueo(null, sillon, TRECE, TRECE_MEDIA, "fixture-motivo");

        List<EntradaAgenda> entradas = consulta.consultar(null, sillon, TRECE, CATORCE);

        assertEquals(1, entradas.size());
        assertEquals("bloqueo", entradas.get(0).tipo());
        assertEquals("fixture-motivo", entradas.get(0).detalle());
        assertEquals(TRECE, entradas.get(0).inicio());
    }

    private Turno crearTurno(Long sillon, Instant inicio) {
        Paciente paciente = pacientes.saveAndFlush(new Paciente(DocumentoTipo.DNI,
                "PAC-WB-" + sillon, "Paciente", "Fixture", "555"));
        Long profesional = profesionales.saveAndFlush(
                new com.turnosodontologia.domain.agenda.Profesional(
                        "MP-WB-" + sillon, "Profesional", "Fixture")).getId();
        return creacionTurnoService.crearTurno(paciente.getId(), profesional, sillon,
                "TRAT-WRITEBACK", inicio, "fixture-estado", "fixture-origen");
    }

    private Long crearSillon(String numero) {
        Sillon sillon = sillones.saveAndFlush(new Sillon(numero,
                consultorios.findById(1L).orElseThrow()));
        return sillon.getId();
    }

    private static Instant local(int hora, int minuto) {
        return FECHA.atTime(hora, minuto).atZone(ZONA).toInstant();
    }
}
