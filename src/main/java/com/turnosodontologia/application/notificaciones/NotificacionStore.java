package com.turnosodontologia.application.notificaciones;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Persistencia JDBC de plantillas, notificaciones, outbox, intentos y eventos de webhook (V010). */
@Repository
public class NotificacionStore {

    private static final RowMapper<Notificacion> NOTIFICACION = (rs, i) -> new Notificacion(
            rs.getLong("id"), rs.getLong("turno_id"), (Long) rs.getObject("plantilla_id"),
            Proposito.valueOf(rs.getString("proposito")), rs.getString("destinatario"),
            EstadoNotificacion.valueOf(rs.getString("estado")), rs.getInt("intentos"),
            rs.getString("proveedor_message_id"), rs.getTimestamp("programada_para").toInstant(),
            rs.getString("ultimo_error"));

    private static final RowMapper<PlantillaWhatsApp> PLANTILLA = (rs, i) -> new PlantillaWhatsApp(
            rs.getLong("id"), rs.getString("codigo_meta"), Proposito.valueOf(rs.getString("proposito")),
            rs.getString("idioma"), rs.getString("estado_aprobacion"), rs.getString("motivo_rechazo"),
            rs.getBoolean("activo"));

    private final JdbcTemplate jdbc;

    public NotificacionStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public long registrarPlantilla(String codigoMeta, Proposito proposito, String idioma, String estado) {
        return jdbc.queryForObject("INSERT INTO plantillas_whatsapp (codigo_meta, proposito, idioma, "
                + "estado_aprobacion) VALUES (?, ?, ?, ?) RETURNING id", Long.class,
                codigoMeta, proposito.name(), idioma, estado);
    }

    public void actualizarAprobacion(long plantillaId, String estado, String motivo) {
        jdbc.update("UPDATE plantillas_whatsapp SET estado_aprobacion = ?, motivo_rechazo = ?, "
                + "updated_at = now() WHERE id = ?", estado, motivo, plantillaId);
    }

    /** Plantilla vigente más reciente para el propósito, aprobada o no. */
    public Optional<PlantillaWhatsApp> plantillaPara(Proposito proposito) {
        return jdbc.query("SELECT * FROM plantillas_whatsapp WHERE proposito = ? AND activo "
                        + "ORDER BY (estado_aprobacion = 'APROBADA') DESC, id DESC LIMIT 1",
                PLANTILLA, proposito.name()).stream().findFirst();
    }

    public Optional<PlantillaWhatsApp> plantilla(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return jdbc.query("SELECT * FROM plantillas_whatsapp WHERE id = ?", PLANTILLA, id)
                .stream().findFirst();
    }

    public Optional<Notificacion> porIdempotencia(String clave) {
        return jdbc.query("SELECT * FROM notificaciones WHERE idempotency_key = ?", NOTIFICACION, clave)
                .stream().findFirst();
    }

    public Optional<Notificacion> notificacion(long id) {
        return jdbc.query("SELECT * FROM notificaciones WHERE id = ?", NOTIFICACION, id).stream().findFirst();
    }

    public Optional<Notificacion> porProveedorMessageId(String proveedorMessageId) {
        return jdbc.query("SELECT * FROM notificaciones WHERE proveedor_message_id = ? FOR UPDATE",
                NOTIFICACION, proveedorMessageId).stream().findFirst();
    }

    public long crearNotificacion(long turnoId, Long plantillaId, Proposito proposito, String destinatario,
                                  EstadoNotificacion estado, Instant programadaPara, String clave,
                                  String error) {
        return jdbc.queryForObject("INSERT INTO notificaciones (turno_id, plantilla_id, proposito, "
                        + "destinatario, estado, programada_para, idempotency_key, ultimo_error) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING id", Long.class,
                turnoId, plantillaId, proposito.name(), destinatario, estado.name(),
                Timestamp.from(programadaPara), clave, error);
    }

    public void encolar(long notificacionId, Instant proximoIntento) {
        jdbc.update("INSERT INTO notificaciones_outbox (notificacion_id, estado, proximo_intento) "
                + "VALUES (?, 'PENDIENTE', ?)", notificacionId, Timestamp.from(proximoIntento));
    }

    /** Toma un lote de pendientes vencidos con bloqueo de fila; otro despachador los saltea. */
    public List<Long> tomarPendientes(Instant ahora, int lote) {
        return jdbc.queryForList("SELECT notificacion_id FROM notificaciones_outbox "
                        + "WHERE estado = 'PENDIENTE' AND proximo_intento <= ? "
                        + "ORDER BY proximo_intento, id LIMIT ? FOR UPDATE SKIP LOCKED",
                Long.class, Timestamp.from(ahora), lote);
    }

    public void reprogramar(long notificacionId, Instant proximoIntento) {
        jdbc.update("UPDATE notificaciones_outbox SET proximo_intento = ? WHERE notificacion_id = ?",
                Timestamp.from(proximoIntento), notificacionId);
    }

    public void cerrarOutbox(long notificacionId) {
        jdbc.update("UPDATE notificaciones_outbox SET estado = 'PROCESADO', procesado_at = now() "
                + "WHERE notificacion_id = ?", notificacionId);
    }

    public void registrarIntento(long notificacionId, int numero, String resultado,
                                 String proveedorMessageId, String error) {
        jdbc.update("INSERT INTO notificacion_intentos (notificacion_id, numero, resultado, "
                        + "proveedor_message_id, error) VALUES (?, ?, ?, ?, ?)",
                notificacionId, numero, resultado, proveedorMessageId, error);
    }

    public void actualizar(long notificacionId, EstadoNotificacion estado, int intentos,
                           String proveedorMessageId, String error) {
        jdbc.update("UPDATE notificaciones SET estado = ?, intentos = ?, "
                        + "proveedor_message_id = COALESCE(?, proveedor_message_id), ultimo_error = ?, "
                        + "updated_at = now() WHERE id = ?",
                estado.name(), intentos, proveedorMessageId, error, notificacionId);
    }

    public void cambiarEstado(long notificacionId, EstadoNotificacion estado) {
        jdbc.update("UPDATE notificaciones SET estado = ?, updated_at = now() WHERE id = ?",
                estado.name(), notificacionId);
    }

    /** Registra el evento del webhook; devuelve {@code false} si ya había sido procesado. */
    public boolean registrarEventoWebhook(String eventoId, String tipo, String proveedorMessageId) {
        return jdbc.update("INSERT INTO whatsapp_webhook_eventos (evento_id, tipo, proveedor_message_id) "
                + "VALUES (?, ?, ?) ON CONFLICT (evento_id) DO NOTHING",
                eventoId, tipo, proveedorMessageId) == 1;
    }

    public Instant inicioTurno(long turnoId) {
        return jdbc.queryForObject("SELECT inicio FROM turnos WHERE id = ?", Timestamp.class, turnoId)
                .toInstant();
    }
}
