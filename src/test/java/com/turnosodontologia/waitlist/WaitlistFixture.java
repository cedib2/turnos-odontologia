package com.turnosodontologia.waitlist;

import com.turnosodontologia.application.CreacionTurnoService;
import com.turnosodontologia.depositpayments.PagoTestConfig;
import com.turnosodontologia.depositpayments.SeniaService;
import com.turnosodontologia.domain.agenda.Ocupacion;
import com.turnosodontologia.domain.agenda.Profesional;
import com.turnosodontologia.domain.agenda.Sillon;
import com.turnosodontologia.domain.agenda.Tratamiento;
import com.turnosodontologia.domain.agenda.Turno;
import com.turnosodontologia.domain.identidad.DocumentoTipo;
import com.turnosodontologia.domain.identidad.Paciente;
import com.turnosodontologia.infrastructure.audit.InMemoryAuditEventRecorder;
import com.turnosodontologia.infrastructure.persistence.ConsultorioRepository;
import com.turnosodontologia.infrastructure.persistence.EntradaWaitlistRepository;
import com.turnosodontologia.infrastructure.persistence.MetricaOcupacionRepository;
import com.turnosodontologia.infrastructure.persistence.OcupacionRepository;
import com.turnosodontologia.infrastructure.persistence.PacienteRepository;
import com.turnosodontologia.infrastructure.persistence.ProfesionalRepository;
import com.turnosodontologia.infrastructure.persistence.SillonRepository;
import com.turnosodontologia.infrastructure.persistence.TratamientoRepository;
import com.turnosodontologia.infrastructure.persistence.TurnoRepository;
import com.turnosodontologia.support.PostgresFlywayTest;
import com.turnosodontologia.TestJpaConfig;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

/** Fixtures compartidas de C-19 sobre Testcontainers/PostgreSQL. */
@SpringBootTest(classes = TestJpaConfig.class)
@ActiveProfiles("test")
@Import({PagoTestConfig.class, WaitlistTestConfig.class})
public abstract class WaitlistFixture extends PostgresFlywayTest {

    private static final AtomicLong SECUENCIA = new AtomicLong();

    @Autowired protected CreacionTurnoService creacion;
    @Autowired protected TurnoRepository turnos;
    @Autowired protected OcupacionRepository ocupaciones;
    @Autowired protected PacienteRepository pacientes;
    @Autowired protected ProfesionalRepository profesionales;
    @Autowired protected SillonRepository sillones;
    @Autowired protected ConsultorioRepository consultorios;
    @Autowired protected TratamientoRepository tratamientos;
    @Autowired protected EntradaWaitlistRepository entradas;
    @Autowired protected MetricaOcupacionRepository metricas;
    @Autowired protected InMemoryAuditEventRecorder recorder;
    @Autowired protected NotificacionWaitlistSimulada notificaciones;
    @Autowired protected SeniaService senias;
    @Autowired protected ListaEsperaService listaEspera;
    @Autowired protected RellenoWaitlistService relleno;
    @Autowired protected MetricaOcupacionService metricaService;

    @BeforeEach
    void limpiarObservadores() {
        recorder.clear();
        notificaciones.clear();
    }

    protected String id(String prefijo) {
        return prefijo + "-" + SECUENCIA.incrementAndGet();
    }

    protected Long crearPaciente() {
        return pacientes.saveAndFlush(new Paciente(DocumentoTipo.DNI, id("PAC"),
                "Paciente", "Fixture", "555")).getId();
    }

    protected Long crearProfesional() {
        return profesionales.saveAndFlush(new Profesional(id("MP"), "Profesional", "Fixture")).getId();
    }

    protected Long crearSillon() {
        return sillones.saveAndFlush(new Sillon(id("S"),
                consultorios.findById(1L).orElseThrow())).getId();
    }

    protected void crearTratamiento(String codigo, int duracion) {
        tratamientos.saveAndFlush(new Tratamiento(codigo, "Fixture", duracion));
    }

    /** Crea un turno confirmado, desactiva su ocupación y lo cancela: hueco ya liberado (W-4). */
    protected Turno crearHuecoLiberado(Long profesional, Long sillon, String codigoTratamiento,
                                       Instant inicio) {
        Turno turno = creacion.crearTurno(crearPaciente(), profesional, sillon,
                codigoTratamiento, inicio, "confirmado", "fixture");
        Ocupacion ocupacion = ocupaciones.findByTurnoIdAndActivoTrue(turno.getId()).orElseThrow();
        ocupacion.desactivar();
        ocupaciones.saveAndFlush(ocupacion);
        turno.cancelar("fixture");
        turnos.saveAndFlush(turno);
        recorder.clear();
        return turno;
    }
}
