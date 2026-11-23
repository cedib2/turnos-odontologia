package com.turnosodontologia.auth;

import com.turnosodontologia.support.PostgresFlywayTest;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import javax.sql.DataSource;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthMigrationV002Test extends PostgresFlywayTest {

    @Autowired
    private DataSource dataSource;

    @Test
    void rechazaJtiDuplicadoEnSesiones() {
        long usuario = crearUsuario("jti-duplicado@example.com");
        crearSesion(usuario, "jti-unico-002", Instant.now().plusSeconds(900));

        assertThrows(DataIntegrityViolationException.class,
                () -> crearSesion(usuario, "jti-unico-002", Instant.now().plusSeconds(900)));
    }

    @Test
    void rechazaHashTokenDuplicadoEnRefresh() {
        long usuario = crearUsuario("hash-duplicado@example.com");
        long sesion = crearSesion(usuario, "jti-sesion-hash", Instant.now().plusSeconds(900));
        crearRefresh(sesion, "jti-refresh-original", "hash-refresh-unico-002");

        assertThrows(DataIntegrityViolationException.class,
                () -> crearRefresh(sesion, "jti-refresh-duplicado", "hash-refresh-unico-002"));
    }

    @Test
    void aplicaV002EnBaseConV005YV006RegistradasCuandoOutOfOrderEstaHabilitado() {
        Integer versionesPrevias = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success = true "
                        + "AND CAST(version AS INTEGER) IN (1, 5, 6)", Integer.class);
        assertEquals(3, versionesPrevias);

        jdbcTemplate.update("DELETE FROM flyway_schema_history WHERE version = '002'");
        jdbcTemplate.update("DROP TABLE IF EXISTS auth_jti_revocados");
        jdbcTemplate.update("DROP TABLE IF EXISTS auth_tokens_refresh");
        jdbcTemplate.update("DROP TABLE IF EXISTS auth_sesiones");
        jdbcTemplate.update("DROP SEQUENCE IF EXISTS auth_jti_revocados_id_seq");
        jdbcTemplate.update("DROP SEQUENCE IF EXISTS auth_tokens_refresh_id_seq");
        jdbcTemplate.update("DROP SEQUENCE IF EXISTS auth_sesiones_id_seq");

        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .outOfOrder(true)
                .load()
                .migrate();

        Integer aplicada = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success = true AND version = '002'",
                Integer.class);
        assertEquals(1, aplicada);
        assertNotNull(jdbcTemplate.queryForObject(
                "SELECT to_regclass('public.auth_sesiones')", String.class));
    }

    @Test
    void baseNuevaAplicaV002EnOrdenJuntoALasDemasMigraciones() {
        List<Integer> versiones = jdbcTemplate.queryForList(
                "SELECT CAST(version AS INTEGER) FROM flyway_schema_history "
                        + "WHERE success = true ORDER BY installed_rank", Integer.class);

        assertEquals(List.of(1, 2, 3, 4, 5, 6, 20, 21, 22, 23), versiones);
    }

    @Test
    void revocacionRegistraSesionesYJtiSinBorrarFilas() {
        long usuario = crearUsuario("revocacion@example.com");
        long sesion = crearSesion(usuario, "jti-sesion-revocada", Instant.now().plusSeconds(900));
        crearRefresh(sesion, "jti-refresh-revocado", "hash-refresh-revocacion");

        jdbcTemplate.update("UPDATE auth_sesiones SET estado = 'REVOCADA', revoked_at = now() WHERE id = ?", sesion);
        jdbcTemplate.update("INSERT INTO auth_jti_revocados (jti, sesion_id, revoked_at) VALUES (?, ?, now())",
                "jti-sesion-revocada", sesion);

        Integer activas = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM auth_sesiones WHERE id = ? AND revoked_at IS NOT NULL", Integer.class, sesion);
        Integer jti = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM auth_jti_revocados WHERE jti = ?", Integer.class, "jti-sesion-revocada");
        Integer refresh = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM auth_tokens_refresh WHERE sesion_id = ?", Integer.class, sesion);
        assertEquals(1, activas);
        assertEquals(1, jti);
        assertEquals(1, refresh);
        assertTrue(refresh > 0);
    }

    private long crearUsuario(String email) {
        Long rol = jdbcTemplate.queryForObject("SELECT id FROM roles WHERE codigo = 'recepcionista'", Long.class);
        return jdbcTemplate.queryForObject(
                "INSERT INTO usuarios (rol_id, email, proveedor) VALUES (?, ?, 'LOCAL') RETURNING id",
                Long.class, rol, email);
    }

    private long crearSesion(long usuario, String jti, Instant expira) {
        return jdbcTemplate.queryForObject(
                "INSERT INTO auth_sesiones (usuario_id, jti, estado, ip_origen, expires_at) "
                        + "VALUES (?, ?, 'ACTIVA', '127.0.0.1', ?) RETURNING id",
                Long.class, usuario, jti, Timestamp.from(expira));
    }

    private void crearRefresh(long sesion, String jti, String hash) {
        jdbcTemplate.update(
                "INSERT INTO auth_tokens_refresh (sesion_id, hash_token, jti, familia_id, expires_at) "
                        + "VALUES (?, ?, ?, ?, ?)",
                sesion, hash, jti, "familia-" + sesion, Timestamp.from(Instant.now().plusSeconds(604800)));
    }
}
