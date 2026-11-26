package com.turnosodontologia.cumplimiento;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guardas estáticas de C-17 sobre el código fuente: configuración solo por nombre (sin valores inventados,
 * RN-GL-03), sin literales de seguridad fuera de {@code auth/} y sin superficie HTTP productiva de
 * auditoría, respaldo o restore.
 */
class CumplimientoSuperficieTest {

    private static final List<String> PROPIEDADES = List.of(
            "BACKUP_CRON", "EXPORT_RETENTION_YEARS", "BACKUP_ENCRYPTION_KEY");

    @Test
    void configuracionSoloPorNombreSinValoresInventados() throws IOException {
        List<String> plantilla = Files.readAllLines(Path.of(".env.example"));
        String codigo = codigo(ruta -> true);

        assertAll(
                () -> assertTrue(plantilla.containsAll(PROPIEDADES)),
                () -> assertTrue(PROPIEDADES.stream().allMatch(v -> v.matches("[A-Z][A-Z0-9_]*"))),
                // Frecuencia, retención y clave siguen (TBD): ningún default concreto en el código.
                () -> assertFalse(codigo.contains("0 0 3 * * *")),
                () -> assertFalse(codigo.contains("EXPORT_RETENTION_YEARS=")),
                () -> assertFalse(codigo.contains("BACKUP_ENCRYPTION_KEY=")),
                () -> assertTrue(codigo.contains("${BACKUP_CRON:}")),
                () -> assertTrue(codigo.contains("${BACKUP_ENCRYPTION_KEY:}"))
        );
    }

    @Test
    void sinLiteralesDeSeguridadFueraDeAuth() throws IOException {
        String superficie = codigo(ruta -> !ruta.contains("/auth/")
                && (ruta.toLowerCase(Locale.ROOT).contains("agenda") || ruta.contains("/cumplimiento/")));

        assertAll(
                () -> assertFalse(superficie.contains("Authorization")),
                () -> assertFalse(superficie.contains("Bearer")),
                () -> assertFalse(superficie.contains("SecurityContext")),
                () -> assertFalse(superficie.contains("@RequestHeader"))
        );
    }

    @Test
    void sinSuperficieHttpProductivaDeAuditoriaRespaldoNiRestore() throws IOException {
        // Los controladores @Profile({"dev", "test"}) son superficie indicativa, no productiva.
        String productivos = controladoresProductivos();
        String servicios = codigo(ruta -> ruta.contains("/application/cumplimiento/"));

        assertAll(
                () -> assertFalse(productivos.isEmpty()),
                () -> assertFalse(productivos.contains("/audit")),
                () -> assertFalse(productivos.contains("/auditoria")),
                () -> assertFalse(productivos.contains("/backup")),
                () -> assertFalse(productivos.contains("/restore")),
                () -> assertFalse(servicios.contains("@RestController")),
                () -> assertFalse(servicios.contains("@Controller"))
        );
    }

    private static String controladoresProductivos() throws IOException {
        try (var caminos = Files.walk(Path.of("src/main"))) {
            return String.join("\n", caminos.filter(Files::isRegularFile)
                    .filter(ruta -> ruta.toString().endsWith("Controller.java"))
                    .map(CumplimientoSuperficieTest::leer)
                    .filter(fuente -> !fuente.contains("@Profile({\"dev\", \"test\"})"))
                    .toList()).toLowerCase(Locale.ROOT);
        }
    }

    /** Concatena el código de {@code src/main} cuyas rutas (con '/') cumplen el filtro. */
    private static String codigo(Predicate<String> filtro) throws IOException {
        try (var caminos = Files.walk(Path.of("src/main"))) {
            return String.join("\n", caminos.filter(Files::isRegularFile)
                    .filter(ruta -> ruta.toString().endsWith(".java")
                            || ruta.toString().endsWith(".yml")
                            || ruta.toString().endsWith(".properties"))
                    .filter(ruta -> filtro.test(ruta.toString().replace('\\', '/')))
                    .map(CumplimientoSuperficieTest::leer)
                    .toList());
        }
    }

    private static String leer(Path ruta) {
        try {
            return Files.readString(ruta);
        } catch (IOException excepcion) {
            throw new IllegalStateException(excepcion);
        }
    }
}
