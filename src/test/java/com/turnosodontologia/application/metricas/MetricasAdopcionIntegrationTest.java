package com.turnosodontologia.application.metricas;

import com.turnosodontologia.domain.auditoria.AuditEvent;
import com.turnosodontologia.infrastructure.audit.InMemoryAuditEventRecorder;
import com.turnosodontologia.support.PostgresFlywayTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MetricasAdopcionIntegrationTest extends PostgresFlywayTest {

    private static final Instant DESDE = Instant.parse("2026-12-01T03:00:00Z");
    private static final Instant HASTA = Instant.parse("2027-01-01T03:00:00Z");

    @Autowired
    private MetricasAdopcionService servicio;

    @Autowired
    private InMemoryAuditEventRecorder recorder;

    private long paciente;
    private long profesional;
    private long sillon;
    private long tratamiento;

    @BeforeEach
    void fixtures() {
        recorder.clear();
        paciente = jdbcTemplate.queryForObject(
                "INSERT INTO pacientes (documento_tipo, documento_numero, nombre, apellido, telefono) "
                        + "VALUES ('DNI', '30111222', 'Fixture', 'Paciente', '555') RETURNING id", Long.class);
        profesional = jdbcTemplate.queryForObject(
                "INSERT INTO profesionales (matricula, nombre, apellido) VALUES ('MP-C22', 'Fixture', 'Prof') "
                        + "RETURNING id", Long.class);
        sillon = jdbcTemplate.queryForObject(
                "INSERT INTO sillones (numero, consultorio_id) VALUES ('C22', 1) RETURNING id", Long.class);
        tratamiento = jdbcTemplate.queryForObject(
                "INSERT INTO tratamientos (codigo, nombre, duracion_minutos) VALUES ('C22', 'Fixture', 30) "
                        + "RETURNING id", Long.class);
    }

    @Test
    void cuentaTurnosPorCanalDeCreacionDentroDelPeriodo() {
        Instant base = Instant.parse("2026-12-10T13:00:00Z");
        crearTurno(base, "tok-1", DESDE.plusSeconds(60), true);
        crearTurno(base.plusSeconds(3600), "tok-2", DESDE.plusSeconds(120), false);
        crearTurno(base.plusSeconds(7200), null, DESDE.plusSeconds(180), true);
        crearTurno(base.plusSeconds(10800), null, HASTA.minusSeconds(1), true);
        // fuera del período: límite superior excluido y anterior a desde
        crearTurno(base.plusSeconds(14400), "tok-3", HASTA, true);
        crearTurno(base.plusSeconds(18000), "tok-4", DESDE.minusSeconds(1), true);

        ResultadoMetrica adopcion = buscar(servicio.consultar(DESDE, HASTA), CatalogoMetricas.ADOPCION_ONLINE);

        assertEquals(EstadoMetrica.CALCULADA, adopcion.estado());
        assertEquals(2L, adopcion.numerador());
        assertEquals(4L, adopcion.denominador());
        assertEquals(new BigDecimal("50.00"), adopcion.porcentaje());
        assertEquals(DESDE, adopcion.desde());
        assertEquals(HASTA, adopcion.hasta());
        assertTrue(adopcion.metodologia().formula().contains("token público"));
    }

    @Test
    void periodoSinTurnosNoPublicaCifra() {
        ResultadoMetrica adopcion = buscar(servicio.consultar(DESDE, HASTA), CatalogoMetricas.ADOPCION_ONLINE);

        assertEquals(EstadoMetrica.SIN_DATOS, adopcion.estado());
        assertNull(adopcion.porcentaje());
    }

    @Test
    void metricasSinFuenteSeInformanNoDisponiblesConSuDependencia() {
        List<ResultadoMetrica> resultados = servicio.consultar(DESDE, HASTA);

        assertEquals(CatalogoMetricas.DEFINICIONES.size(), resultados.size());
        assertEquals("C-09", dependencia(resultados, CatalogoMetricas.CUMPLIMIENTO_VENTANA));
        assertEquals("C-10", dependencia(resultados, CatalogoMetricas.CONFIRMACIONES));
        assertEquals("C-18", dependencia(resultados, CatalogoMetricas.EVIDENCIA_CUMPLIMIENTO));
    }

    @Test
    void cadaConsultaQuedaAuditadaConActorYPeriodo() {
        servicio.consultar(DESDE, HASTA);

        List<AuditEvent> eventos = recorder.eventos();
        assertEquals(1, eventos.size());
        AuditEvent evento = eventos.get(0);
        assertEquals(MetricasAdopcionService.EVENTO_CONSULTA, evento.tipo());
        assertEquals("metricas", evento.entidad());
        assertEquals("recepcionista", evento.actor().nombre());
        assertEquals(DESDE.toString(), evento.metadata().get("desde"));
        assertEquals(HASTA.toString(), evento.metadata().get("hasta"));
        assertEquals("SIN_DATOS", ((Map<?, ?>) evento.metadata().get("metricas"))
                .get(CatalogoMetricas.ADOPCION_ONLINE));
    }

    @Test
    void periodoInvalidoNoConsultaNiAudita() {
        assertThrows(IllegalArgumentException.class, () -> servicio.consultar(HASTA, DESDE));
        assertThrows(IllegalArgumentException.class, () -> servicio.consultar(null, HASTA));
        assertTrue(recorder.eventos().isEmpty());
    }

    private void crearTurno(Instant inicio, String tokenPublico, Instant creado, boolean activo) {
        jdbcTemplate.update("INSERT INTO turnos (paciente_id, profesional_id, sillon_id, tipo_tratamiento_id, "
                        + "inicio, fin, duracion_minutos, estado, origen, token_publico, created_at, activo) "
                        + "VALUES (?, ?, ?, ?, ?, ?, 30, 'fixture-estado', 'fixture-origen', ?, ?, ?)",
                paciente, profesional, sillon, tratamiento, Timestamp.from(inicio),
                Timestamp.from(inicio.plusSeconds(1800)), tokenPublico, Timestamp.from(creado), activo);
    }

    private static ResultadoMetrica buscar(List<ResultadoMetrica> resultados, String codigo) {
        return resultados.stream().filter(r -> r.codigo().equals(codigo)).findFirst().orElseThrow();
    }

    private static String dependencia(List<ResultadoMetrica> resultados, String codigo) {
        ResultadoMetrica r = buscar(resultados, codigo);
        assertEquals(EstadoMetrica.NO_DISPONIBLE, r.estado());
        assertNull(r.porcentaje());
        return r.dependenciaPendiente().substring(0, 4);
    }
}
