package com.turnosodontologia.audit;

import com.turnosodontologia.support.PostgresFixture;
import com.turnosodontologia.support.PostgresFlywayTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuditoriaLogAppendOnlyTest extends PostgresFlywayTest {

    private static final String ROL_APLICACION = "auditoria_app_test";

    @BeforeEach
    void prepararRolDeAplicacion() {
        jdbcTemplate.execute("DO $$ BEGIN IF NOT EXISTS "
                + "(SELECT FROM pg_roles WHERE rolname = 'auditoria_app_test') THEN "
                + "CREATE ROLE auditoria_app_test LOGIN; END IF; END $$;");
        jdbcTemplate.execute("GRANT turnos_app TO auditoria_app_test");
        jdbcTemplate.execute("GRANT CONNECT ON DATABASE turnos_test TO auditoria_app_test");
        jdbcTemplate.execute("GRANT USAGE ON SCHEMA public TO auditoria_app_test");
        jdbcTemplate.execute("GRANT USAGE ON SEQUENCE auditoria_log_id_seq TO auditoria_app_test");
        jdbcTemplate.execute("GRANT SELECT ON auditoria_log TO auditoria_app_test");
        insertarFila("append-only-insercion");
    }

    @Test
    void insercionConfirmadaYConservada() throws SQLException {
        try (Connection connection = conexionAplicacion();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("INSERT INTO auditoria_log (fecha, actor_nombre, modo, accion, "
                    + "entidad, entidad_id, motivo, metadata, correlation_id, hash_anterior, hash_actual) "
                    + "VALUES (now(), 'recepcionista', 'SIMULADO', 'turno.creado', 'turno', 42, "
                    + "'motivo-insercion', '{}'::jsonb, 'corr-insercion', repeat('0', 64), repeat('1', 64))");
        }

        assertEquals("motivo-insercion", jdbcTemplate.queryForObject(
                "SELECT motivo FROM auditoria_log WHERE correlation_id = 'corr-insercion'", String.class));
        assertEquals("turno.creado", jdbcTemplate.queryForObject(
                "SELECT accion FROM auditoria_log WHERE correlation_id = 'corr-insercion'", String.class));
    }

    @Test
    void intentoDeActualizacionRechazado() throws SQLException {
        assertThrows(SQLException.class, () -> {
            try (Connection connection = conexionAplicacion();
                 Statement statement = connection.createStatement()) {
                statement.executeUpdate("UPDATE auditoria_log SET motivo = 'modificado' "
                        + "WHERE correlation_id = 'append-only-insercion'");
            }
        });

        assertEquals("motivo-original", jdbcTemplate.queryForObject(
                "SELECT motivo FROM auditoria_log WHERE correlation_id = 'append-only-insercion'", String.class));
    }

    @Test
    void intentoDeBorradoRechazado() throws SQLException {
        assertThrows(SQLException.class, () -> {
            try (Connection connection = conexionAplicacion();
                 Statement statement = connection.createStatement()) {
                statement.executeUpdate("DELETE FROM auditoria_log "
                        + "WHERE correlation_id = 'append-only-insercion'");
            }
        });

        assertEquals(1, totalFilas());
    }

    @Test
    void intentoDeVaciadoRechazado() throws SQLException {
        assertThrows(SQLException.class, () -> {
            try (Connection connection = conexionAplicacion();
                 Statement statement = connection.createStatement()) {
                statement.executeUpdate("TRUNCATE TABLE auditoria_log");
            }
        });

        assertEquals(1, totalFilas());
    }

    private void insertarFila(String correlationId) {
        jdbcTemplate.update("INSERT INTO auditoria_log (fecha, actor_nombre, modo, accion, entidad, "
                        + "entidad_id, motivo, metadata, correlation_id, hash_anterior, hash_actual) "
                        + "VALUES (now(), 'recepcionista', 'SIMULADO', 'turno.creado', 'turno', 1, "
                        + "'motivo-original', '{}'::jsonb, ?, repeat('0', 64), repeat('1', 64))",
                correlationId);
    }

    private Connection conexionAplicacion() throws SQLException {
        return DriverManager.getConnection(PostgresFixture.jdbcUrl(), ROL_APLICACION, "");
    }

    private Integer totalFilas() {
        return jdbcTemplate.queryForObject("SELECT count(*)::int FROM auditoria_log", Integer.class);
    }
}
