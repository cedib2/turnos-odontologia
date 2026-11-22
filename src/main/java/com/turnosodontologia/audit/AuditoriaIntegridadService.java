package com.turnosodontologia.audit;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public final class AuditoriaIntegridadService {

    private static final String SELECT = """
            SELECT id, fecha, actor_id, actor_nombre, actor_rol, actor_jti, modo, accion,
                   entidad, entidad_id, ip, dispositivo, motivo, metadata::text, correlation_id,
                   hash_anterior, hash_actual
            FROM auditoria_log
            ORDER BY id
            """;

    private final JdbcTemplate jdbcTemplate;
    private final AuditoriaCanonicalizer canonicalizer;

    public AuditoriaIntegridadService(JdbcTemplate jdbcTemplate, AuditoriaCanonicalizer canonicalizer) {
        this.jdbcTemplate = jdbcTemplate;
        this.canonicalizer = canonicalizer;
    }

    public Resultado verificar() {
        List<FilaConHash> filas = jdbcTemplate.query(SELECT, (rs, numero) ->
                new FilaConHash(new AuditoriaCanonicalizer.Fila(
                        rs.getLong("id"),
                        rs.getTimestamp("fecha").toInstant(),
                        (Long) rs.getObject("actor_id"),
                        rs.getString("actor_nombre"),
                        rs.getString("actor_rol"),
                        rs.getString("actor_jti"),
                        rs.getString("modo"),
                        rs.getString("accion"),
                        rs.getString("entidad"),
                        (Long) rs.getObject("entidad_id"),
                        rs.getString("ip"),
                        rs.getString("dispositivo"),
                        rs.getString("motivo"),
                        canonicalizer.leerMetadata(rs.getString("metadata")),
                        rs.getString("correlation_id"),
                        rs.getString("hash_anterior")), rs.getString("hash_actual")));

        String esperado = AuditoriaCanonicalizer.GENESIS;
        for (FilaConHash registro : filas) {
            AuditoriaCanonicalizer.Fila fila = registro.fila();
            if (!esperado.equals(fila.hashAnterior().trim())
                    || !canonicalizer.hash(fila).equals(registro.hashActual().trim())) {
                return new Resultado(false, fila.id(),
                        "hash inválido en fila " + fila.id(), filas.size());
            }
            esperado = registro.hashActual().trim();
        }
        return Resultado.valida(filas.size());
    }

    private record FilaConHash(AuditoriaCanonicalizer.Fila fila, String hashActual) {
    }

    public record Resultado(boolean valida, Long idAfectado, String detalle, long filasVerificadas) {

        public static Resultado valida(long filasVerificadas) {
            return new Resultado(true, null, "cadena válida", filasVerificadas);
        }
    }
}
