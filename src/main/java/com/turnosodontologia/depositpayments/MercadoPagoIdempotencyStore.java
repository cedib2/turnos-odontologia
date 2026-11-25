package com.turnosodontologia.depositpayments;

import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * Deduplicación de webhooks sobre {@code public_idempotencia} (V004), operación
 * {@code pago.webhook}. Usa INSERT ... ON CONFLICT DO NOTHING para que un reenvío
 * no aborte la transacción que contiene el cambio de estado (MP-7).
 */
public final class MercadoPagoIdempotencyStore {

    public static final String OPERACION = "pago.webhook";

    private final JdbcTemplate jdbc;

    public MercadoPagoIdempotencyStore(DataSource dataSource) {
        this.jdbc = new JdbcTemplate(dataSource);
    }

    public boolean registrar(String clave) {
        return jdbc.update("INSERT INTO public_idempotencia (operacion, idempotency_key_hash) "
                        + "VALUES (?, ?) ON CONFLICT (operacion, idempotency_key_hash) DO NOTHING",
                OPERACION, hash(clave)) > 0;
    }

    public boolean esRepeticion(String clave) {
        Boolean existe = jdbc.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM public_idempotencia "
                        + "WHERE operacion = ? AND idempotency_key_hash = ?)",
                Boolean.class, OPERACION, hash(clave));
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
