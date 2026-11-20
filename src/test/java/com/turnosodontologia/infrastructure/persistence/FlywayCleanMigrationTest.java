package com.turnosodontologia.infrastructure.persistence;

import com.turnosodontologia.support.PostgresFlywayTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlywayCleanMigrationTest extends PostgresFlywayTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private Environment environment;

    @Test
    void laMigracionLimpiaDejaElHistorialYElSeedDelModeloBase() {
        Integer applied = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success = true "
                        + "AND CAST(version AS INTEGER) = 1", Integer.class);
        assertEquals(1, applied);
        Integer consultorios = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM consultorios WHERE nombre = 'Consultorio principal'", Integer.class);
        assertEquals(1, consultorios);
    }

    @Test
    void elPerfilTestUsaPostgresYNoDejaQueHibernateGenereElEsquema() throws SQLException {
        try (var connection = dataSource.getConnection()) {
            assertTrue(connection.getMetaData().getURL().startsWith("jdbc:postgresql:"));
        }
        assertEquals("validate", environment.getProperty("spring.jpa.hibernate.ddl-auto"));
        assertEquals("org.hibernate.dialect.PostgreSQLDialect",
                environment.getProperty("spring.jpa.properties.hibernate.dialect"));
    }
}
