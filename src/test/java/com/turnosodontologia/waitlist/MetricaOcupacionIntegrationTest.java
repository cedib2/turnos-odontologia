package com.turnosodontologia.waitlist;

import com.turnosodontologia.domain.auditoria.AuditEvent;
import com.turnosodontologia.domain.waitlist.MetricaOcupacion;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Grupo 5: ocupación, ausentismo, metodología y período, sin meta inventada. */
class MetricaOcupacionIntegrationTest extends WaitlistFixture {

    private static final String PERIODO = "2026-12";
    private static final LocalDate DIA_HABIL = LocalDate.of(2026, 12, 5);
    private static final Instant DIA_INICIO = Instant.parse("2026-12-05T10:00:00Z");

    @Test
    void ocupacionIncluyeBloqueosYDa70ConMetodologiaYPeriodo() {
        Long sillon = crearSillon();
        Long profesional = crearProfesional();
        crearHorarioUnicoDia(DIA_HABIL);
        marcarFeriadosExcepto(DIA_HABIL);
        crearTratamiento("METRICA-1", 300);
        creacion.crearTurno(crearPaciente(), profesional, sillon, "METRICA-1",
                DIA_INICIO, "confirmado", "fixture");
        insertarBloqueo(sillon, DIA_INICIO.plus(300, ChronoUnit.MINUTES), 120);
        recorder.clear();

        MetricaOcupacion metrica = metricaService.publicar(sillon, PERIODO);

        assertNotNull(metrica);
        assertEquals(PERIODO, metrica.getPeriodo());
        assertEquals(sillon, metrica.getSillonId());
        assertEquals("70.0000", metrica.getOcupacion().toPlainString());
        assertTrue(metrica.getMetodologia().contains("ocupacion="));
        assertTrue(metrica.getMetodologia().contains("fuente="));
        assertTrue(metrica.getMetodologia().contains("America/Argentina/Buenos_Aires"));
        assertEquals(WaitlistTestConfig.AHORA, metrica.getCalculadaEn());
        assertTipo("metrica.publicada", "SYSTEM");
    }

    @Test
    void sinMinutosDisponiblesNoPublicaCifraNiEvento() {
        Long sillon = crearSillon();

        assertNull(metricaService.publicar(sillon, PERIODO));

        assertEquals(0, metricas.count());
        assertEquals(0, recorder.eventos().size());
    }

    @Test
    void ausentismoConTresInasistenciasSobreVeinteDa15() {
        Long sillon = crearSillon();
        crearHorarioTodosLosDias();
        insertarTurnos(sillon, 20, 3, 0);

        MetricaOcupacion metrica = metricaService.publicar(sillon, PERIODO);

        assertEquals("15.0000", metrica.getAusentismo().toPlainString());
        assertEquals(3, contarEventos("turno.inasistencia"));
    }

    @Test
    void canceladosQuedanFueraDelNumeradorYDelDenominador() {
        Long sillon = crearSillon();
        crearHorarioTodosLosDias();
        insertarTurnos(sillon, 22, 1, 2);

        MetricaOcupacion metrica = metricaService.publicar(sillon, PERIODO);

        assertEquals("5.0000", metrica.getAusentismo().toPlainString());
        assertEquals(1, contarEventos("turno.inasistencia"));
        assertEquals(2, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM turnos WHERE estado = 'cancelado'", Integer.class));
    }

    @Test
    void inasistenciaSeRegistraExplicitamenteConActorDeRecepcion() {
        Long sillon = crearSillon();
        crearHorarioTodosLosDias();
        List<Long> ids = insertarTurnos(sillon, 1, 0, 0);

        metricaService.registrarInasistencia(ids.get(0));

        assertEquals("inasistente", turnos.findById(ids.get(0)).orElseThrow().getEstado());
        AuditEvent evento = recorder.eventos().stream()
                .filter(e -> "turno.inasistencia".equals(e.tipo())).findFirst().orElseThrow();
        assertEquals("turno", evento.entidad());
        assertEquals("SIMULADO", evento.actor().modo());
    }

    @Test
    void publicacionSinMetodologiaValidaOPeriodoSeRechazaSinPersistir() {
        Long sillon = crearSillon();
        crearHorarioTodosLosDias();

        assertThrows(IllegalArgumentException.class, () -> metricaService.publicar(sillon, "2026-13"));
        assertThrows(IllegalArgumentException.class, () -> metricaService.publicar(null, PERIODO));

        assertEquals(0, metricas.count());
        assertEquals(0, contarEventos("metrica.publicada"));
    }

    @Test
    void consultaDevuelveCifraConMetodologiaYPeriodoSinMeta() {
        Long sillon = crearSillon();
        crearHorarioTodosLosDias();
        insertarTurnos(sillon, 1, 0, 0);
        metricaService.publicar(sillon, PERIODO);
        recorder.clear();

        MetricaOcupacion consultada = metricaService.consultar(sillon, PERIODO);

        assertNotNull(consultada);
        assertNotNull(consultada.getOcupacion());
        assertNotNull(consultada.getMetodologia());
        assertTrue(consultada.getMetodologia().contains("(TBD) -> forma de obtencion"));
        assertEquals(PERIODO, consultada.getPeriodo());
        assertEquals(1, metricas.count());
        assertEquals(0, recorder.eventos().size(), "la consulta no debe publicar de nuevo");
    }

    private void crearHorarioUnicoDia(LocalDate fecha) {
        jdbcTemplate.update("INSERT INTO horarios (dia_semana, desde, hasta) VALUES (?, '10:00', '20:00')",
                fecha.getDayOfWeek().getValue());
    }

    private void crearHorarioTodosLosDias() {
        for (DayOfWeek dia : DayOfWeek.values()) {
            jdbcTemplate.update("INSERT INTO horarios (dia_semana, desde, hasta) "
                    + "VALUES (?, '10:00', '20:00')", dia.getValue());
        }
    }

    private void marcarFeriadosExcepto(LocalDate excepto) {
        LocalDate fecha = LocalDate.of(2026, 12, 1);
        while (fecha.getMonthValue() == 12) {
            if (!fecha.equals(excepto)) {
                jdbcTemplate.update("INSERT INTO feriados (fecha, descripcion) VALUES (?, 'fixture')",
                        java.sql.Date.valueOf(fecha));
            }
            fecha = fecha.plusDays(1);
        }
    }

    private void insertarBloqueo(Long sillon, Instant desde, int minutos) {
        jdbcTemplate.update("INSERT INTO bloqueos (sillon_id, desde, hasta, motivo) "
                        + "VALUES (?, ?, ?, 'fixture-metrica')",
                sillon, java.sql.Timestamp.from(desde),
                java.sql.Timestamp.from(desde.plus(minutos, ChronoUnit.MINUTES)));
    }

    /** Devuelve los ids de los turnos activos (los primeros se marcan como inasistencia). */
    private List<Long> insertarTurnos(Long sillon, int total, int inasistencias, int cancelados) {
        Long paciente = crearPaciente();
        Long profesional = crearProfesional();
        crearTratamiento("METRICA-FIXTURE", 60);
        Long tratamiento = tratamientos.findByCodigo("METRICA-FIXTURE").orElseThrow().getId();
        List<Long> ids = new ArrayList<>();
        for (int i = 0; i < total; i++) {
            Instant inicio = Instant.parse("2026-12-02T00:00:00Z").plus(i * 2L, ChronoUnit.HOURS);
            boolean cancelado = i >= inasistencias && i < inasistencias + cancelados;
            ids.add(jdbcTemplate.queryForObject(
                    "INSERT INTO turnos (paciente_id, profesional_id, sillon_id, tipo_tratamiento_id, "
                            + "inicio, fin, duracion_minutos, estado, origen, activo) "
                            + "VALUES (?, ?, ?, ?, ?, ?, 60, ?, 'fixture', ?) RETURNING id",
                    Long.class, paciente, profesional, sillon, tratamiento,
                    java.sql.Timestamp.from(inicio),
                    java.sql.Timestamp.from(inicio.plus(60, ChronoUnit.MINUTES)),
                    cancelado ? "cancelado" : "confirmado", !cancelado));
        }
        for (int i = 0; i < inasistencias; i++) {
            metricaService.registrarInasistencia(ids.get(i));
        }
        return ids;
    }

    private int contarEventos(String tipo) {
        return (int) recorder.eventos().stream().filter(e -> tipo.equals(e.tipo())).count();
    }

    private void assertTipo(String tipo, String modo) {
        AuditEvent evento = recorder.eventos().stream()
                .filter(e -> tipo.equals(e.tipo())).findFirst().orElseThrow();
        assertEquals(modo, evento.actor().modo());
    }
}
