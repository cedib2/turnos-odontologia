package com.turnosodontologia.invoicing;

import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * Deduplicación sobre {@code public_idempotencia} (V004), reutilizada sin cambios
 * de esquema. Operaciones {@code factura.emitir} y {@code nota_credito.emitir}.
 * La inserción ocurre en la misma transacción que el cambio de estado y la auditoría.
 */
public final class FacturacionIdempotencyStore {

    public static final String OPERACION_FACTURA = "factura.emitir";
    public static final String OPERACION_NOTA = "nota_credito.emitir";

    private final JdbcTemplate jdbc;

    public FacturacionIdempotencyStore(DataSource dataSource) {
        this.jdbc = new JdbcTemplate(dataSource);
    }

    public boolean registrar(String operacion, String clave) {
        return jdbc.update("INSERT INTO public_idempotencia (operacion, idempotency_key_hash) "
                        + "VALUES (?, ?) ON CONFLICT (operacion, idempotency_key_hash) DO NOTHING",
                operacion, hash(clave)) > 0;
    }

    public boolean esRepeticion(String operacion, String clave) {
        Boolean existe = jdbc.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM public_idempotencia "
                        + "WHERE operacion = ? AND idempotency_key_hash = ?)",
                Boolean.class, operacion, hash(clave));
        return Boolean.TRUE.equals(existe);
    }

    private String hash(String valor) {
        try {
            byte[] resumen = MessageDigest.getInstance("SHA-256")
                    .digest(valor.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(resumen);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
