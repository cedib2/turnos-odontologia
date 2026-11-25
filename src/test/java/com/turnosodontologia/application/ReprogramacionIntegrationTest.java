package com.turnosodontologia.application;

import com.turnosodontologia.domain.agenda.HorarioAtencion;
import com.turnosodontologia.domain.agenda.Profesional;
import com.turnosodontologia.domain.agenda.Sillon;
import com.turnosodontologia.domain.agenda.Turno;
import com.turnosodontologia.domain.identidad.DocumentoTipo;
import com.turnosodontologia.domain.identidad.Paciente;
import com.turnosodontologia.infrastructure.audit.InMemoryAuditEventRecorder;
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
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReprogramacionIntegrationTest extends PostgresFlywayTest {

    private static final ZoneId ZONA = ZoneId.of("America/Argentina/Buenos_Aires");
    private static final LocalDate FECHA = LocalDate.of(2026, 12, 25);
    private static final Instant TRECE = local(13, 0);
    private static final Instant TRECE_MEDIA = local(13, 30);
    private static final Instant CATORCE = local(14, 0);

    @Autowired
    private ReprogramacionService service;

    @Autowired
    private CreacionTurnoService creacionTurnoService;

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
    private InMemoryAuditEventRecorder recorder;

    @BeforeEach
    void sembrarHorario() {
        horarios.saveAndFlush(new HorarioAtencion(null, FECHA.getDayOfWeek().getValue(),
                LocalTime.of(13, 0), LocalTime.of(15, 0)));
        tratamientos.saveAndFlush(new com.turnosodontologia.domain.agenda.Tratamiento(
                "TRAT-REPRO-INT", "Fixture", 30));
        recorder.clear();
    }

    @Test
    void reprogramaTurnoYDejaElEstadoNuevoEnLaAgenda() {
        Turno turno = crearTurno("S-REPRO-INT-1", TRECE);
        recorder.clear();

        Turno resultado = service.reprogramar(turno.getId(), TRECE_MEDIA, "recepcionista");

        assertEquals(TRECE_MEDIA, turnos.findById(resultado.getId()).orElseThrow().getInicio());
        assertEquals(1, ocupaciones.countByActivoTrue());
        assertEquals("turno.reprogramado", recorder.eventos().get(0).tipo());
        // Contrato con C-22: la ventana de 48 hs hábiles se mide contra el inicio original.
        assertEquals(TRECE.toString(), recorder.eventos().get(0).metadata().get("inicioAnterior"));
    }

    @Test
    void destinoOcupadoDejaElTurnoOriginalIntacto() {
        Long profesional = crearProfesional();
        Turno turno = crearTurnoConProfesional("S-REPRO-INT-2", profesional, TRECE);
        crearTurnoConProfesional("S-REPRO-INT-3", profesional, TRECE_MEDIA);
        recorder.clear();

        assertThrows(IllegalArgumentException.class, () ->
                service.reprogramar(turno.getId(), TRECE_MEDIA, "recepcionista"));

        assertEquals(TRECE, turnos.findById(turno.getId()).orElseThrow().getInicio());
        assertEquals(2, ocupaciones.countByActivoTrue());
        assertEquals(0, recorder.eventos().size());
    }

    @Test
    void dosOperacionesSimultaneasNoGeneranDobleOcupacion() throws Exception {
        Long profesional = crearProfesional();
        Turno uno = crearTurnoConProfesional("S-REPRO-CONC-1", profesional, TRECE);
        Turno dos = crearTurnoConProfesional("S-REPRO-CONC-2", profesional, TRECE_MEDIA);
        CyclicBarrier barrera = new CyclicBarrier(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Callable<Boolean> operacionUno = () -> ejecutar(barrera, uno.getId(), CATORCE);
            Callable<Boolean> operacionDos = () -> ejecutar(barrera, dos.getId(), CATORCE);
            Future<Boolean> futuroUno = executor.submit(operacionUno);
            Future<Boolean> futuroDos = executor.submit(operacionDos);

            int exitos = (futuroUno.get(30, TimeUnit.SECONDS) ? 1 : 0)
                    + (futuroDos.get(30, TimeUnit.SECONDS) ? 1 : 0);

            assertEquals(1, exitos);
            assertEquals(1, ocupaciones.findActivosQueSolapan(profesional, null,
                    CATORCE, CATORCE.plusSeconds(1800)).size());
        } finally {
            executor.shutdownNow();
        }
    }

    private boolean ejecutar(CyclicBarrier barrera, Long turnoId, Instant destino) throws Exception {
        barrera.await();
        try {
            service.reprogramar(turnoId, destino, "recepcionista");
            return true;
        } catch (IllegalArgumentException | DataIntegrityViolationException ex) {
            return false;
        }
    }

    private Turno crearTurno(String numeroSillon, Instant inicio) {
        return crearTurnoConProfesional(numeroSillon, crearProfesional(), inicio);
    }

    private Turno crearTurnoConProfesional(String numeroSillon, Long profesional, Instant inicio) {
        Paciente paciente = pacientes.saveAndFlush(new Paciente(DocumentoTipo.DNI,
                "PAC-REPRO-" + numeroSillon, "Paciente", "Fixture", "555"));
        Sillon sillon = sillones.saveAndFlush(new Sillon(numeroSillon,
                consultorios.findById(1L).orElseThrow()));
        return creacionTurnoService.crearTurno(paciente.getId(), profesional, sillon.getId(),
                "TRAT-REPRO-INT", inicio, "fixture-estado", "fixture-origen");
    }

    private Long crearProfesional() {
        return profesionales.saveAndFlush(new Profesional("MP-REPRO-" + System.nanoTime(),
                "Profesional", "Fixture")).getId();
    }

    private static Instant local(int hora, int minuto) {
        return FECHA.atTime(hora, minuto).atZone(ZONA).toInstant();
    }
}
