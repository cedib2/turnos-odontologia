package com.turnosodontologia.support;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.MountableFile;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.TimeZone;

public final class PostgresFixture {

    private static final String IMAGE = "postgres:17";
    private static final GenericContainer<?> CONTAINER = new GenericContainer<>(IMAGE)
            .withEnv("POSTGRES_HOST_AUTH_METHOD", "trust")
            .withEnv("POSTGRES_DB", "turnos_test")
            .withCopyFileToContainer(MountableFile.forClasspathResource("sql/btree_gist.sql"),
                    "/docker-entrypoint-initdb.d/01-btree_gist.sql")
            // Cada contexto de Spring cacheado conserva su pool; el default de 100 conexiones queda justo.
            .withCommand("postgres", "-c", "max_connections=300")
            .withExposedPorts(5432)
            .waitingFor(Wait.forListeningPort());

    static {
        TimeZone.setDefault(TimeZone.getTimeZone("America/Argentina/Buenos_Aires"));
        CONTAINER.start();
    }

    private PostgresFixture() {
    }

    public static String jdbcUrl() {
        return jdbcUrl("turnos_test");
    }

    public static String jdbcUrl(String database) {
        return "jdbc:postgresql://" + CONTAINER.getHost() + ":" + CONTAINER.getMappedPort(5432) + "/" + database;
    }

    public static String user() {
        return "postgres";
    }

    public static String password() {
        return "";
    }

    public static Connection open() throws SQLException {
        return open("turnos_test");
    }

    public static Connection open(String database) throws SQLException {
        return DriverManager.getConnection(jdbcUrl(database), user(), password());
    }

    public static void execute(String sql, Object... parameters) throws SQLException {
        execute("turnos_test", sql, parameters);
    }

    public static void execute(String database, String sql, Object... parameters) throws SQLException {
        try (Connection connection = open(database);
             var statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < parameters.length; i++) {
                statement.setObject(i + 1, parameters[i]);
            }
            statement.executeUpdate();
        }
    }
}
