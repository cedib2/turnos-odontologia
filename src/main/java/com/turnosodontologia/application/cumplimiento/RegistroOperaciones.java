package com.turnosodontologia.application.cumplimiento;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Acceso JDBC al registro de operaciones de cumplimiento (V030). */
@Repository
public class RegistroOperaciones {

    private static final RowMapper<OperacionCumplimiento> MAPPER = RegistroOperaciones::mapear;

    private final JdbcTemplate jdbc;

    public RegistroOperaciones(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public long solicitar(String tipo, String alcance, Long pacienteId, String formato, String motivo,
                          String solicitadoPor, Instant solicitadoAt, Instant plazoVenceAt,
                          Long respaldoOrigenId, String entornoRestore) {
        return jdbc.queryForObject("""
                INSERT INTO cumplimiento_operaciones
                    (tipo, alcance, paciente_id, estado, formato, motivo, solicitado_por, solicitado_at,
                     plazo_vence_at, respaldo_origen_id, entorno_restore)
                VALUES (?, ?, ?, 'SOLICITADA', ?, ?, ?, ?, ?, ?, ?)
                RETURNING id
                """, Long.class, tipo, alcance, pacienteId, formato, motivo, solicitadoPor,
                Timestamp.from(solicitadoAt), ts(plazoVenceAt), respaldoOrigenId, entornoRestore);
    }

    public void completar(long id, String destino, boolean cifrado, String checksum, int tablas, long filas,
                          Instant completadoAt, Instant retenerHasta) {
        int actualizadas = jdbc.update("""
                UPDATE cumplimiento_operaciones
                SET estado = 'COMPLETADA', destino = ?, cifrado = ?, checksum_sha256 = ?, tablas = ?,
                    filas = ?, completado_at = ?, retener_hasta = ?
                WHERE id = ? AND estado = 'SOLICITADA'
                """, destino, cifrado, checksum, tablas, filas, Timestamp.from(completadoAt),
                ts(retenerHasta), id);
        exigirUna(actualizadas, id);
    }

    public void fallar(long id, String error, Instant fecha) {
        String detalle = error == null || error.isBlank() ? "error sin detalle" : error;
        int actualizadas = jdbc.update("""
                UPDATE cumplimiento_operaciones SET estado = 'FALLIDA', error = ?, completado_at = ?
                WHERE id = ? AND estado = 'SOLICITADA'
                """, detalle.length() > 500 ? detalle.substring(0, 500) : detalle, Timestamp.from(fecha), id);
        exigirUna(actualizadas, id);
    }

    public Optional<OperacionCumplimiento> buscar(long id) {
        return jdbc.query("SELECT * FROM cumplimiento_operaciones WHERE id = ?", MAPPER, id)
                .stream().findFirst();
    }

    public List<OperacionCumplimiento> listar(String tipo) {
        return jdbc.query("SELECT * FROM cumplimiento_operaciones WHERE tipo = ? ORDER BY id", MAPPER, tipo);
    }

    /** Solicitudes cuyo plazo (si está configurado) venció sin completarse. */
    public List<OperacionCumplimiento> vencidas(Instant ahora) {
        return jdbc.query("""
                SELECT * FROM cumplimiento_operaciones
                WHERE estado = 'SOLICITADA' AND plazo_vence_at IS NOT NULL AND plazo_vence_at < ?
                ORDER BY plazo_vence_at
                """, MAPPER, Timestamp.from(ahora));
    }

    public Optional<OperacionCumplimiento> ultimaCompletada(String tipo, String alcance) {
        return jdbc.query("""
                SELECT * FROM cumplimiento_operaciones
                WHERE tipo = ? AND alcance = ? AND estado = 'COMPLETADA'
                ORDER BY completado_at DESC, id DESC LIMIT 1
                """, MAPPER, tipo, alcance).stream().findFirst();
    }

    private static void exigirUna(int actualizadas, long id) {
        if (actualizadas != 1) {
            throw new IllegalStateException("La operación " + id + " no está pendiente");
        }
    }

    private static Timestamp ts(Instant instante) {
        return instante == null ? null : Timestamp.from(instante);
    }

    private static Instant instante(ResultSet rs, String columna) throws SQLException {
        Timestamp valor = rs.getTimestamp(columna);
        return valor == null ? null : valor.toInstant();
    }

    private static OperacionCumplimiento mapear(ResultSet rs, int fila) throws SQLException {
        return new OperacionCumplimiento(rs.getLong("id"), rs.getString("tipo"), rs.getString("alcance"),
                (Long) rs.getObject("paciente_id"), rs.getString("estado"), rs.getString("formato"),
                rs.getString("destino"), rs.getBoolean("cifrado"), (Long) rs.getObject("respaldo_origen_id"),
                rs.getString("entorno_restore"), rs.getString("motivo"), rs.getString("solicitado_por"),
                instante(rs, "solicitado_at"), instante(rs, "plazo_vence_at"), instante(rs, "completado_at"),
                instante(rs, "retener_hasta"), rs.getString("checksum_sha256"), (Integer) rs.getObject("tablas"),
                (Long) rs.getObject("filas"), rs.getString("error"));
    }
}
