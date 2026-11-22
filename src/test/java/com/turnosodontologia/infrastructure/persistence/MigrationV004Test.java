package com.turnosodontologia.infrastructure.persistence;

import com.turnosodontologia.support.PostgresFlywayTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MigrationV004Test extends PostgresFlywayTest {

    @Autowired
    private Environment environment;

    @Test
    void v004SoloMetadatosSeAplicaConV006PreviaYOutOfOrder() throws IOException {
        assertEquals(1, versionesAplicadas("004"));
        assertEquals(1, versionesAplicadas("006"));
        assertTrue(Boolean.TRUE.equals(environment.getProperty("spring.flyway.out-of-order", Boolean.class)));

        String sql = Files.readString(Path.of("src", "main", "resources", "db", "migration",
                "V004__public_access_tokens.sql"));
        String sqlNormalizado = sql.toUpperCase(Locale.ROOT);
        assertFalse(sqlNormalizado.contains("TOKEN_PUBLICO"));
        assertFalse(sqlNormalizado.contains("CREATE TABLE TURNOS"));
        assertFalse(sqlNormalizado.contains("ALTER TABLE TURNOS"));

        assertEquals(1, countColumn("turnos", "token_publico"));
        for (String columna : new String[]{"jti", "token_hash", "scope", "paciente_id",
                "turno_id", "issued_at", "expires_at", "used_at", "revoked_at"}) {
            assertEquals(1, countColumn("public_token_jti", columna), "falta la columna " + columna);
        }
        for (String columna : new String[]{"operacion", "idempotency_key_hash", "creado_en"}) {
            assertEquals(1, countColumn("public_idempotencia", columna), "falta la columna " + columna);
        }

        Integer tokenEnClaro = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns "
                        + "WHERE table_schema = 'public' "
                        + "AND table_name IN ('public_token_jti', 'public_idempotencia') "
                        + "AND column_name LIKE '%token%' AND column_name NOT LIKE '%hash%'",
                Integer.class);
        assertEquals(0, tokenEnClaro);

        Integer claveEnClaro = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns "
                        + "WHERE table_schema = 'public' AND table_name = 'public_idempotencia' "
                        + "AND column_name = 'idempotency_key'", Integer.class);
        assertEquals(0, claveEnClaro);

        for (String indice : new String[]{"uq_public_token_jti_hash",
                "ix_public_token_jti_vigencia", "pk_public_idempotencia"}) {
            assertEquals(1, countIndice(indice), "falta el índice " + indice);
        }
    }

    private int versionesAplicadas(String version) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE version = ? AND success = TRUE",
                Integer.class, version);
    }

    private int countColumn(String tabla, String columna) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns "
                        + "WHERE table_schema = 'public' AND table_name = ? AND column_name = ?",
                Integer.class, tabla, columna);
    }

    private int countIndice(String indice) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pg_indexes WHERE schemaname = 'public' AND indexname = ?",
                Integer.class, indice);
    }
}
