package com.turnosodontologia.application.metricas;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.util.Map;

/**
 * Adopción online: turnos creados con token público (link/QR) sobre el total creado en
 * el período {@code [desde, hasta)}. Se cuentan todos los turnos creados, incluidos los
 * que luego se desactivaron, porque la métrica mide el canal de creación.
 */
@Component
public class AdopcionOnlineFuente implements FuenteMetrica {

    private static final String CONTEO = """
            SELECT COUNT(*) AS total,
                   COUNT(*) FILTER (WHERE token_publico IS NOT NULL) AS online
            FROM turnos
            WHERE created_at >= ? AND created_at < ?
            """;

    private final JdbcTemplate jdbcTemplate;

    public AdopcionOnlineFuente(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public String codigo() {
        return CatalogoMetricas.ADOPCION_ONLINE;
    }

    @Override
    public ResultadoMetrica calcular(Periodo periodo, MetodologiaMetrica metodologia) {
        Map<String, Object> fila = jdbcTemplate.queryForMap(CONTEO,
                Timestamp.from(periodo.desde()), Timestamp.from(periodo.hasta()));
        long total = ((Number) fila.get("total")).longValue();
        long online = ((Number) fila.get("online")).longValue();
        return ResultadoMetrica.desdeConteo(codigo(), online, total, periodo, metodologia);
    }
}
