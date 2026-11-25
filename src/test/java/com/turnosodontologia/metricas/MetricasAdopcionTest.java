package com.turnosodontologia.metricas;

import com.turnosodontologia.application.AuditEventPublisher;
import com.turnosodontologia.application.metricas.Metrica;
import com.turnosodontologia.application.metricas.MetricasService;
import com.turnosodontologia.cumplimiento.CumplimientoFixtures;
import com.turnosodontologia.cumplimiento.CumplimientoPostgresTest;
import com.turnosodontologia.domain.agenda.Feriado;
import com.turnosodontologia.domain.agenda.Turno;
import com.turnosodontologia.domain.auditoria.ActorSimulado;
import com.turnosodontologia.domain.auditoria.AuditEvent;
import com.turnosodontologia.domain.identidad.Paciente;
import com.turnosodontologia.infrastructure.persistence.FeriadoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** C-22: adopción online, ventana de 48 hs hábiles, metodología obligatoria y auditoría de consulta. */
class MetricasAdopcionTest extends CumplimientoPostgresTest {

    private static final Instant DESDE = Instant.now().minusSeconds(3600);
    private static final Instant HASTA = Instant.now().plusSeconds(3600 * 24);

    @Autowired
    private MetricasService metricas;

    @Autowired
    private CumplimientoFixtures fixtures;

    @Autowired
    private AuditEventPublisher publisher;

    @Autowired
    private FeriadoRepository feriados;

    @BeforeEach
    void sembrar() {
        fixtures.sembrarCatalogo();
    }

    @Test
    void adopcionCuentaTurnosConTokenPublicoSobreElTotalDelPeriodo() {
        Paciente paciente = fixtures.paciente("DOC-METRICAS", "Mara");
        Turno online = fixtures.turno(paciente, "S-M-1", 9, "link-publico");
        fixtures.turno(paciente, "S-M-2", 10, "recepcion");
        fixtures.turno(paciente, "S-M-3", 11, "recepcion");
        Turno qr = fixtures.turno(paciente, "S-M-4", 12, "qr");
        jdbcTemplate.update("UPDATE turnos SET token_publico = 'jti-x' WHERE id IN (?, ?)", online.getId(), qr.getId());

        Metrica adopcion = metricas.adopcionOnline(DESDE, HASTA, "tablero semanal");

        assertEquals(Metrica.CALCULADA, adopcion.estado());
        assertEquals(2L, adopcion.numerador());
        assertEquals(4L, adopcion.denominador());
        assertEquals(new BigDecimal("50.00"), adopcion.valor());
        assertEquals(Map.of("link-publico", 1L, "qr", 1L, "recepcion", 2L), adopcion.detalle().get("turnosPorOrigen"));
        assertEquals(Metrica.META_TBD, adopcion.meta());
        assertTrue(adopcion.formula().startsWith("100 ×"));
        assertEquals(DESDE, adopcion.periodoDesde());
        assertEquals("America/Argentina/Buenos_Aires", adopcion.zonaHoraria());

        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT count(*) FROM auditoria_log WHERE accion = 'metricas.consultadas' "
                        + "AND metadata->'metricas' ? 'ADOPCION_ONLINE'", Integer.class));
    }

    @Test
    void sinTurnosEnElPeriodoNoSePublicaCifra() {
        Metrica adopcion = metricas.adopcionOnline(Instant.parse("2020-01-01T00:00:00Z"),
                Instant.parse("2020-02-01T00:00:00Z"), null);

        assertEquals(Metrica.SIN_DATOS, adopcion.estado());
        assertNull(adopcion.valor());
    }

    @Test
    void elPeriodoYLaMetodologiaSonObligatorios() {
        assertThrows(IllegalArgumentException.class, () -> metricas.adopcionOnline(null, HASTA, null));
        assertThrows(IllegalArgumentException.class, () -> metricas.tablero(HASTA, DESDE, null));
        assertThrows(IllegalArgumentException.class, () -> new Metrica("X", "def", " ", "fuente", DESDE, HASTA,
                "UTC", Metrica.CALCULADA, 1L, 1L, null, null, null));
        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT count(*) FROM auditoria_log WHERE accion = 'metricas.consultadas'", Integer.class));
    }

    @Test
    void ventanaDe48HorasHabilesUsaElInicioOriginalYLosFeriados() {
        LocalDate viernes = LocalDate.of(2026, 12, 18);
        feriados.saveAndFlush(new Feriado(viernes.plusDays(3), "Feriado de prueba"));
        Instant evento = CumplimientoFixtures.local(viernes, 10);
        // Con el lunes feriado: hasta el miércoles 10:00 hay 14 + 24 + 10 = 48 hs hábiles → dentro.
        reprogramacion(evento, CumplimientoFixtures.local(viernes.plusDays(5), 10));
        // Hasta el martes 10:00 hay 14 + 10 = 24 hs hábiles → fuera.
        reprogramacion(evento, CumplimientoFixtures.local(viernes.plusDays(4), 10));
        reprogramacion(evento, null);

        Metrica ventana = metricas.cumplimientoVentana(evento.minusSeconds(60), evento.plusSeconds(60), null);

        assertEquals(1L, ventana.numerador());
        assertEquals(2L, ventana.denominador());
        assertEquals(new BigDecimal("50.00"), ventana.valor());
        assertEquals(1L, ventana.detalle().get("excluidosSinInicioAnterior"));
    }

    @Test
    void tableroDeclaraLoNoDisponibleEnLugarDeInventarCifras() {
        List<Metrica> tablero = metricas.tablero(DESDE, HASTA, "tablero");

        assertEquals(List.of(MetricasService.ADOPCION, MetricasService.VENTANA, MetricasService.CONFIRMACIONES,
                MetricasService.OCUPACION, MetricasService.AUSENTISMO, MetricasService.DOSSIER),
                tablero.stream().map(Metrica::codigo).toList());
        assertEquals(Metrica.NO_DISPONIBLE, tablero.get(2).estado());
        assertEquals(Metrica.DIFERIDA, tablero.get(3).estado());
        assertEquals(Metrica.DIFERIDA, tablero.get(4).estado());
        tablero.subList(2, 5).forEach(m -> assertNull(m.valor()));
        assertEquals(12L, tablero.get(5).denominador());
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT count(*) FROM auditoria_log WHERE accion = 'metricas.consultadas'", Integer.class));
    }

    private void reprogramacion(Instant fecha, Instant inicioAnterior) {
        Map<String, Object> metadata = new java.util.HashMap<>();
        if (inicioAnterior != null) {
            metadata.put("inicioAnterior", inicioAnterior.toString());
        }
        publisher.publish(new AuditEvent("turno.reprogramado", "turno", 1L, ActorSimulado.recepcionista(), fecha,
                "reprogramación", metadata, UUID.randomUUID().toString()));
    }
}
