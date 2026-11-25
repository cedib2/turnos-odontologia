package com.turnosodontologia.application.metricas;

import com.turnosodontologia.application.AuditEventPublisher;
import com.turnosodontologia.application.ConfiguracionSchedulingService;
import com.turnosodontologia.application.CurrentActorProvider;
import com.turnosodontologia.application.cumplimiento.Dossier;
import com.turnosodontologia.application.cumplimiento.DossierService;
import com.turnosodontologia.domain.auditoria.ActorSimulado;
import com.turnosodontologia.domain.auditoria.AuditEvent;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Tablero de adopción y cumplimiento (C-22). Cada métrica sale con metodología y período; cada consulta
 * queda en el log append-only. Las confirmaciones salen del outbox de WhatsApp (C-10); la ocupación y el
 * ausentismo se toman de las métricas que C-19 publica por sillón y mes, con su propia metodología: C-22 no
 * las recalcula ni las agrega, para no inventar una fórmula.
 */
@Service
public class MetricasService {

    public static final String ADOPCION = "ADOPCION_ONLINE";
    public static final String VENTANA = "CUMPLIMIENTO_VENTANA_48H";
    public static final String CONFIRMACIONES = "CONFIRMACIONES";
    public static final String OCUPACION = "OCUPACION_POR_SILLON";
    public static final String AUSENTISMO = "AUSENTISMO";
    public static final String DOSSIER = "EVIDENCIA_CUMPLIMIENTO";

    /**
     * Contrato con C-08/C-09: eventos que mueven un turno y llevan {@code inicioAnterior} (ISO-8601) en metadata.
     * La reprogramación del staff (C-08) y la cancelación y reprogramación por link (C-09) lo informan.
     */
    static final List<String> EVENTOS_VENTANA = List.of("turno.reprogramado", "turno.cancelado");
    static final long HORAS_VENTANA = 48;

    private final JdbcTemplate jdbc;
    private final CalculadoraHorasHabiles horasHabiles;
    private final DossierService dossier;
    private final CurrentActorProvider actores;
    private final AuditEventPublisher publisher;
    private final TransactionTemplate tx;

    public MetricasService(JdbcTemplate jdbc, ConfiguracionSchedulingService configuracion,
                           DossierService dossier, CurrentActorProvider actores, AuditEventPublisher publisher,
                           PlatformTransactionManager transactionManager) {
        this.jdbc = jdbc;
        this.horasHabiles = new CalculadoraHorasHabiles(configuracion::esFeriado);
        this.dossier = dossier;
        this.actores = actores;
        this.publisher = publisher;
        this.tx = new TransactionTemplate(transactionManager);
    }

    public Metrica adopcionOnline(Instant desde, Instant hasta, String motivo) {
        Metrica metrica = calcularAdopcion(desde, hasta);
        auditar(List.of(metrica), desde, hasta, motivo);
        return metrica;
    }

    public Metrica cumplimientoVentana(Instant desde, Instant hasta, String motivo) {
        Metrica metrica = calcularVentana(desde, hasta);
        auditar(List.of(metrica), desde, hasta, motivo);
        return metrica;
    }

    /** Las cinco métricas de C-01 E más confirmaciones, en una sola consulta auditada. */
    public List<Metrica> tablero(Instant desde, Instant hasta, String motivo) {
        validarPeriodo(desde, hasta);
        List<Metrica> metricas = new ArrayList<>();
        metricas.add(calcularAdopcion(desde, hasta));
        metricas.add(calcularVentana(desde, hasta));
        metricas.add(calcularConfirmaciones(desde, hasta));
        metricas.add(publicadaPorC19(OCUPACION, "ocupacion",
                "% de agenda ocupada por sillón/profesional con metodología publicable", desde, hasta));
        metricas.add(publicadaPorC19(AUSENTISMO, "ausentismo",
                "% de no-shows sobre turnos confirmados", desde, hasta));
        metricas.add(calcularDossier(desde, hasta));
        auditar(metricas, desde, hasta, motivo);
        return metricas;
    }

    private Metrica calcularAdopcion(Instant desde, Instant hasta) {
        validarPeriodo(desde, hasta);
        Map<String, Long> porOrigen = new LinkedHashMap<>();
        long[] totales = new long[2];
        jdbc.query("""
                SELECT origen, count(*) AS total, count(*) FILTER (WHERE token_publico IS NOT NULL) AS online
                FROM turnos
                WHERE created_at >= ? AND created_at < ?
                GROUP BY origen ORDER BY origen
                """, rs -> {
            porOrigen.put(rs.getString("origen"), rs.getLong("total"));
            totales[0] += rs.getLong("online");
            totales[1] += rs.getLong("total");
        }, Timestamp.from(desde), Timestamp.from(hasta));
        Map<String, Object> detalle = new LinkedHashMap<>();
        detalle.put("turnosPorOrigen", porOrigen);
        detalle.put("criterioOnline", "turno creado con token público de link/QR (turnos.token_publico no nulo)");
        return new Metrica(ADOPCION,
                "% de turnos creados por el paciente vía link/QR sobre el total de turnos creados",
                "100 × turnos con canal link/QR / total de turnos creados en el período",
                "Entidad Turno (created_at, token_publico, origen)", desde, hasta,
                CalculadoraHorasHabiles.ZONA.getId(), totales[1] == 0 ? Metrica.SIN_DATOS : Metrica.CALCULADA,
                totales[0], totales[1], Metrica.porcentaje(totales[0], totales[1]), null, detalle);
    }

    private Metrica calcularVentana(Instant desde, Instant hasta) {
        validarPeriodo(desde, hasta);
        long[] conteo = new long[3];
        Map<String, Long> porEvento = new LinkedHashMap<>();
        jdbc.query("""
                SELECT accion, fecha, metadata->>'inicioAnterior' AS inicio_anterior
                FROM auditoria_log
                WHERE accion IN (?, ?) AND fecha >= ? AND fecha < ?
                ORDER BY id
                """, rs -> {
            porEvento.merge(rs.getString("accion"), 1L, Long::sum);
            Instant inicioAnterior = parsear(rs.getString("inicio_anterior"));
            if (inicioAnterior == null) {
                conteo[2]++;
                return;
            }
            conteo[1]++;
            if (horasHabiles.conAnticipacion(rs.getTimestamp("fecha").toInstant(), inicioAnterior, HORAS_VENTANA)) {
                conteo[0]++;
            }
        }, EVENTOS_VENTANA.get(0), EVENTOS_VENTANA.get(1), Timestamp.from(desde), Timestamp.from(hasta));
        Map<String, Object> detalle = new LinkedHashMap<>();
        detalle.put("eventosPorTipo", porEvento);
        detalle.put("excluidosSinInicioAnterior", conteo[2]);
        detalle.put("criterio", "anticipación ≥ " + HORAS_VENTANA + " hs hábiles entre el evento y el inicio "
                + "original del turno; lunes a viernes sin feriados (C-01 C2)");
        return new Metrica(VENTANA,
                "% de cancelaciones/reprogramaciones hechas con al menos 48 hs hábiles de anticipación",
                "100 × cancelaciones/reprogramaciones dentro de la ventana / total con inicio original registrado",
                "auditoria_log (turno.reprogramado, turno.cancelado) y calendario de feriados (C-07)",
                desde, hasta, CalculadoraHorasHabiles.ZONA.getId(),
                conteo[1] == 0 ? Metrica.SIN_DATOS : Metrica.CALCULADA,
                conteo[0], conteo[1], Metrica.porcentaje(conteo[0], conteo[1]), null, detalle);
    }

    private Metrica calcularConfirmaciones(Instant desde, Instant hasta) {
        validarPeriodo(desde, hasta);
        // Solicitudes de confirmación aceptadas por el proveedor y programadas dentro del período.
        String enviadas = """
                FROM notificaciones
                WHERE proposito = 'CONFIRMACION' AND proveedor_message_id IS NOT NULL
                  AND programada_para >= ? AND programada_para < ?
                """;
        Map<String, Long> porEstado = new LinkedHashMap<>();
        jdbc.query("SELECT estado, count(DISTINCT turno_id) AS turnos " + enviadas + " GROUP BY estado ORDER BY estado",
                rs -> {
                    porEstado.put(rs.getString("estado"), rs.getLong("turnos"));
                }, Timestamp.from(desde), Timestamp.from(hasta));
        long[] totales = jdbc.queryForObject("SELECT count(DISTINCT turno_id) FILTER (WHERE estado = 'CONFIRMADA'), "
                        + "count(DISTINCT turno_id) " + enviadas,
                (rs, fila) -> new long[]{rs.getLong(1), rs.getLong(2)}, Timestamp.from(desde), Timestamp.from(hasta));
        Map<String, Object> detalle = new LinkedHashMap<>();
        detalle.put("turnosPorEstado", porEstado);
        detalle.put("criterioEnviada", "notificación de propósito CONFIRMACION aceptada por el proveedor "
                + "(proveedor_message_id no nulo), programada dentro del período");
        detalle.put("criterioConfirmada", "el paciente respondió con el botón de confirmación (estado CONFIRMADA)");
        return new Metrica(CONFIRMACIONES,
                "% de turnos confirmados por el paciente vía WhatsApp sobre las solicitudes de confirmación enviadas",
                "100 × turnos con confirmación respondida / turnos con solicitud de confirmación enviada",
                "Outbox y webhooks de WhatsApp (C-10): tabla notificaciones", desde, hasta,
                CalculadoraHorasHabiles.ZONA.getId(), totales[1] == 0 ? Metrica.SIN_DATOS : Metrica.CALCULADA,
                totales[0], totales[1], Metrica.porcentaje(totales[0], totales[1]), null, detalle);
    }

    /**
     * Ocupación o ausentismo tal como los publica C-19: un valor por sillón y mes, cada uno con su metodología.
     * Se incluyen los meses (zona AR) que se superponen con el período. No se agregan entre sillones: el valor
     * del tablero queda nulo y las cifras van en el detalle.
     */
    private Metrica publicadaPorC19(String codigo, String columna, String definicion, Instant desde, Instant hasta) {
        validarPeriodo(desde, hasta);
        List<Map<String, Object>> porSillon = new ArrayList<>();
        jdbc.query("""
                SELECT sillon_id, periodo, %s AS valor, metodologia, calculada_en
                FROM metricas_ocupacion
                WHERE activo AND %s IS NOT NULL
                ORDER BY periodo, sillon_id
                """.formatted(columna, columna), rs -> {
            YearMonth mes = YearMonth.parse(rs.getString("periodo"));
            Instant inicioMes = mes.atDay(1).atStartOfDay(CalculadoraHorasHabiles.ZONA).toInstant();
            Instant finMes = mes.plusMonths(1).atDay(1).atStartOfDay(CalculadoraHorasHabiles.ZONA).toInstant();
            if (inicioMes.isBefore(hasta) && finMes.isAfter(desde)) {
                Map<String, Object> fila = new LinkedHashMap<>();
                fila.put("sillonId", rs.getLong("sillon_id"));
                fila.put("periodo", mes.toString());
                fila.put("valor", rs.getBigDecimal("valor"));
                fila.put("metodologia", rs.getString("metodologia"));
                fila.put("calculadaEn", rs.getTimestamp("calculada_en").toInstant().toString());
                porSillon.add(fila);
            }
        });
        Map<String, Object> detalle = new LinkedHashMap<>();
        detalle.put("porSillonYMes", porSillon);
        detalle.put("nota", "Valores publicados por C-19 con su metodología; C-22 no los agrega entre sillones");
        return new Metrica(codigo, definicion,
                "La publicada por C-19 para cada sillón y mes (ver metodologia en el detalle)",
                "Métricas publicadas por C-19 (tabla metricas_ocupacion)", desde, hasta,
                CalculadoraHorasHabiles.ZONA.getId(), porSillon.isEmpty() ? Metrica.SIN_DATOS : Metrica.CALCULADA,
                null, null, null, null, detalle);
    }

    private Metrica calcularDossier(Instant desde, Instant hasta) {
        Dossier.Completitud completitud = dossier.generar().completitud();
        return new Metrica(DOSSIER,
                "Dossier descargable completo (DPA, AAIP, RNBD, logs, exportación)",
                completitud.formula(), "Dossier de cumplimiento (C-18), logs append-only, exportación y respaldo (C-17)",
                desde, hasta, CalculadoraHorasHabiles.ZONA.getId(), Metrica.CALCULADA,
                (long) completitud.presentes(), (long) completitud.exigidos(), completitud.porcentaje(), null,
                Map.of("metodologia", completitud.metodologia(),
                        "nota", "Estado del dossier al momento de la consulta"));
    }

    private void auditar(List<Metrica> metricas, Instant desde, Instant hasta, String motivo) {
        ActorSimulado actor = actores.current();
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("metricas", metricas.stream().map(Metrica::codigo).toList());
        metadata.put("desde", desde.toString());
        metadata.put("hasta", hasta.toString());
        tx.executeWithoutResult(estado -> publisher.publish(AuditEvent.de("metricas.consultadas", "metrica",
                null, actor, motivo == null ? "" : motivo, metadata, UUID.randomUUID().toString())));
    }

    private static void validarPeriodo(Instant desde, Instant hasta) {
        if (desde == null || hasta == null || !hasta.isAfter(desde)) {
            throw new IllegalArgumentException("El período (desde < hasta) es obligatorio para toda métrica");
        }
    }

    private static Instant parsear(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(valor);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
