package com.turnosodontologia.publictoken;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class PublicIdempotencyStore {

    private final JdbcTemplate jdbc;

    public PublicIdempotencyStore(DataSource dataSource) {
        this.jdbc = new JdbcTemplate(dataSource);
    }

    public boolean registrar(String operacion, String clave) {
        try {
            jdbc.update("INSERT INTO public_idempotencia (operacion, idempotency_key_hash) VALUES (?, ?)",
                    operacion, hash(clave));
            return true;
        } catch (DuplicateKeyException e) {
            return false;
        }
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
            StringBuilder texto = new StringBuilder();
            for (byte parte : resumen) {
                texto.append(String.format("%02x", parte));
            }
            return texto.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
