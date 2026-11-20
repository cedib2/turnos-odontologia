package com.turnosodontologia.infrastructure.persistence;

import com.turnosodontologia.support.PostgresFixture;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BtreeGistExtensionTest {

    @Test
    void laBaseDePruebaTieneHabilitadaLaExtensionBtreeGist() throws SQLException {
        try (Connection connection = PostgresFixture.open();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(
                     "SELECT extversion FROM pg_extension WHERE extname = 'btree_gist'")) {
            assertTrue(result.next(), "btree_gist no está habilitada en PostgreSQL real");
            assertNotNull(result.getString(1));
        }
    }

    @Test
    void createExtensionFuncionaEnUnaBaseLimpia() throws SQLException {
        PostgresFixture.execute("DROP DATABASE IF EXISTS turnos_extension_probe");
        PostgresFixture.execute("CREATE DATABASE turnos_extension_probe");
        try {
            PostgresFixture.execute("turnos_extension_probe", "CREATE EXTENSION btree_gist");
            String version = versionEn("turnos_extension_probe");
            assertNotNull(version);
            assertEquals(versionEn("turnos_test"), version);
        } finally {
            PostgresFixture.execute("DROP DATABASE IF EXISTS turnos_extension_probe");
        }
    }

    private String versionEn(String database) throws SQLException {
        try (Connection connection = PostgresFixture.open(database);
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(
                     "SELECT extversion FROM pg_extension WHERE extname = 'btree_gist'")) {
            assertTrue(result.next(), "CREATE EXTENSION no registró btree_gist en " + database);
            return result.getString(1);
        }
    }
}
