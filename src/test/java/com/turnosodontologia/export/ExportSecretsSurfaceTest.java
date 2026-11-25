package com.turnosodontologia.export;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExportSecretsSurfaceTest {

    private static final List<String> PROPIEDADES = List.of(
            ExportOperationalConfig.CRON_PROPERTY,
            ExportOperationalConfig.RETENCION_PROPERTY,
            ExportOperationalConfig.CLAVE_PROPERTY);

    @Test
    void configuracionSoloConNombres() throws IOException {
        List<String> lineas = Files.readAllLines(Path.of(".env.example"));
        String codigo = codigoMain();

        assertAll(
                () -> assertTrue(lineas.containsAll(PROPIEDADES)),
                () -> assertTrue(PROPIEDADES.stream().allMatch(v -> v.matches("[A-Z][A-Z0-9_]*"))),
                () -> assertFalse(codigo.contains("0 0 3 * * *")),
                () -> assertFalse(codigo.contains("EXPORT_RETENTION_YEARS=10")),
                () -> assertFalse(codigo.contains("BACKUP_ENCRYPTION_KEY"))
        );
    }

    @Test
    void sinLiteralesDeSeguridadFueraDeAuth() throws IOException {
        String superficie = codigoFuente().stream()
                .filter(ruta -> !ruta.toString().replace('\\', '/').contains("/auth/"))
                .filter(ruta -> ruta.toString().toLowerCase(Locale.ROOT).contains("agenda")
                        || ruta.toString().contains("export"))
                .map(this::leer)
                .reduce("", String::concat);

        assertAll(
                () -> assertFalse(superficie.contains("Authorization")),
                () -> assertFalse(superficie.contains("Bearer")),
                () -> assertFalse(superficie.contains("SecurityContext")),
                () -> assertFalse(superficie.contains("@RequestHeader"))
        );
    }

    @Test
    void sinSuperficieHttpNueva() throws IOException {
        String controladores = codigoFuente().stream()
                .filter(ruta -> ruta.getFileName().toString().endsWith("Controller.java"))
                .map(this::leer)
                .filter(fuente -> !fuente.contains("@Profile({\"dev\", \"test\"})"))
                .reduce("", String::concat);
        String export = codigoMain();

        assertAll(
                () -> assertFalse(controladores.toLowerCase(Locale.ROOT).contains("/audit")),
                () -> assertFalse(controladores.toLowerCase(Locale.ROOT).contains("/auditoria")),
                () -> assertFalse(controladores.toLowerCase(Locale.ROOT).contains("/backup")),
                () -> assertFalse(controladores.toLowerCase(Locale.ROOT).contains("/restore")),
                () -> assertFalse(export.contains("@RestController")),
                () -> assertFalse(export.contains("@Controller"))
        );
    }

    private String codigoMain() throws IOException {
        return codigoFuente().stream()
                .filter(ruta -> ruta.toString().replace('\\', '/').contains("/src/main/"))
                .map(this::leer)
                .reduce("", String::concat);
    }

    private List<Path> codigoFuente() throws IOException {
        try (var caminos = Files.walk(Path.of("src/main"))) {
            return caminos.filter(Files::isRegularFile)
                    .filter(ruta -> ruta.toString().endsWith(".java")
                            || ruta.toString().endsWith(".yml")
                            || ruta.toString().endsWith(".properties"))
                    .toList();
        }
    }

    private String leer(Path ruta) {
        try {
            return Files.readString(ruta);
        } catch (IOException excepcion) {
            throw new IllegalStateException(excepcion);
        }
    }
}
