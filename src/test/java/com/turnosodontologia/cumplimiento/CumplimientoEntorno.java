package com.turnosodontologia.cumplimiento;

import com.turnosodontologia.support.PostgresFixture;
import org.springframework.test.context.DynamicPropertyRegistry;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.util.Base64;

/** Entorno de C-17: clave de cifrado de prueba, directorio temporal y base aislada para restores. */
public final class CumplimientoEntorno {

    public static final String BASE_AISLADA = "restore_aislado_test";
    public static final String CLAVE = clave();
    public static final Path DIRECTORIO = directorio();

    static {
        try {
            PostgresFixture.execute("postgres", "DROP DATABASE IF EXISTS " + BASE_AISLADA);
            PostgresFixture.execute("postgres", "CREATE DATABASE " + BASE_AISLADA);
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    private CumplimientoEntorno() {
    }

    public static void registrar(DynamicPropertyRegistry registry) {
        registry.add("cumplimiento.cifrado.clave", () -> CLAVE);
        registry.add("cumplimiento.almacen.directorio", DIRECTORIO::toString);
        registry.add("cumplimiento.exportacion.plazo-horas", () -> "48");
        registry.add("cumplimiento.respaldo.retencion-anios", () -> "10");
        registry.add("cumplimiento.restore.aislado.url", () -> PostgresFixture.jdbcUrl(BASE_AISLADA));
        registry.add("cumplimiento.restore.aislado.usuario", PostgresFixture::user);
        registry.add("cumplimiento.restore.aislado.clave", PostgresFixture::password);
    }

    private static String clave() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    private static Path directorio() {
        try {
            return Files.createTempDirectory("turnos-respaldos-");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
