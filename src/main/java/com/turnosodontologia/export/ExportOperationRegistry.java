package com.turnosodontologia.export;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;

public final class ExportOperationRegistry {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final JdbcTemplate jdbc;

    public ExportOperationRegistry(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void registrar(OperacionRegistrada operacion) {
        Map<String, Object> metadata = operacion.metadata() == null ? Map.of() : operacion.metadata();
        jdbc.update("INSERT INTO export_backup_restore_operacion "
                        + "(correlation_id, operacion, resultado, motivo, metadata, tenant_id, actor_nombre) "
                        + "VALUES (?, ?, ?, ?, CAST(? AS jsonb), ?, ?)",
                operacion.correlationId(), operacion.operacion(), operacion.resultado(),
                operacion.motivo(), escribir(metadata), operacion.tenantId(), operacion.actorNombre());
    }

    private String escribir(Map<String, Object> metadata) {
        try {
            return JSON.writeValueAsString(metadata);
        } catch (Exception excepcion) {
            throw new IllegalArgumentException("No se pudo serializar metadata de operación", excepcion);
        }
    }
}
