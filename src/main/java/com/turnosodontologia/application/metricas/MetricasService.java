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
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Tablero de adopción y cumplimiento (C-22). Cada métrica sale con metodología y período; cada consulta
 * queda en el log append-only. Ocupación y ausentismo dependen de C-19 (diferido a v0.2) y confirmaciones
 * de C-10: se informan como DIFERIDA / NO_DISPONIBLE en lugar de inventar cifras.
 */
@Service
public class MetricasService {

    public static final String ADOPCION = "ADOPCION_ONLINE";
    public static final String VENTANA = "CUMPLIMIENTO_VENTANA_48H";
    public static final String CONFIRMACIONES = "CONFIRMACIONES";
    public static final String OCUPACION = "OCUPACION_POR_SILLON";
    public static final String AUSENTISMO = "AUSENTISMO";
    public static final String DOSSIER = "EVIDENCIA_CUMPLIMIENTO";

    /** Contrato con C-08/C-09: eventos que mueven un turno y llevan {@code inicioAnterior} en metadata. */
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
        metricas.add(new Metrica(CONFIRMACIONES,
                "% de turnos confirmados por el paciente vía WhatsApp sobre los recordatorios enviados",
                "100 × turnos confirmados / turnos con recordatorio enviado",
                "Outbox y webhooks de WhatsApp (C-10)", desde, hasta, CalculadoraHorasHabiles.ZONA.getId(),
                Metrica.NO_DISPONIBLE, null, null, null, null,
                Map.of("motivo", "C-10 whatsapp-notifications-outbox no implementado")));
        metricas.add(new Metrica(OCUPACION,
                "% de agenda ocupada por sillón/profesional con metodología publicable",
                "100 × minutos ocupados / minutos disponibles, por sillón y profesional",
                "Agenda, turnos y bloqueos (C-08); motor de ocupación C-19", desde, hasta,
                CalculadoraHorasHabiles.ZONA.getId(), Metrica.DIFERIDA, null, null, null, null,
                Map.of("motivo", "C-19 diferido a v0.2 (C-01 Gap C-19); bloqueos como ocupación (TBD)")));
        metricas.add(new Metrica(AUSENTISMO,
                "% de no-shows sobre turnos confirmados",
                "100 × turnos con evento de inasistencia / total de turnos confirmados",
                "Eventos de asistencia del turno y auditoría append-only (C-10/C-19)", desde, hasta,
                CalculadoraHorasHabiles.ZONA.getId(), Metrica.DIFERIDA, null, null, null, null,
                Map.of("motivo", "Sin evento de inasistencia hasta C-10/C-19; definición del numerador (TBD)")));
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
