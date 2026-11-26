package com.turnosodontologia;

import com.turnosodontologia.interfaces.HealthCheckResponse;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Smoke test del scaffold base (C-02): estructura de directorios, contrato de la plantilla de entorno,
 * contrato del health check y tipos base del dominio. Originalmente era un harness con {@code main}
 * en {@code tests/}, de antes de que el proyecto usara Maven.
 */
class SmokeTest {

    private static final Path ROOT = Path.of(".").toAbsolutePath().normalize();

    private static final List<String> REQUIRED_DIRS = List.of(
            "src/main/java/com/turnosodontologia/domain",
            "src/main/java/com/turnosodontologia/domain/agenda",
            "src/main/java/com/turnosodontologia/domain/clinico",
            "src/main/java/com/turnosodontologia/domain/financiero",
            "src/main/java/com/turnosodontologia/domain/identidad",
            "src/main/java/com/turnosodontologia/domain/auditoria",
            "src/main/java/com/turnosodontologia/domain/notificaciones",
            "src/main/java/com/turnosodontologia/application",
            "src/main/java/com/turnosodontologia/infrastructure",
            "src/main/java/com/turnosodontologia/interfaces",
            "src/main/resources",
            "src/test/java");

    private static final List<String> REQUIRED_ENV = List.of(
            "APP_BASE_URL", "DATABASE_URL", "TIMEZONE", "CANCELLATION_WINDOW_HOURS",
            "WHATSAPP_ACCESS_TOKEN", "WHATSAPP_PHONE_NUMBER_ID", "WHATSAPP_WEBHOOK_VERIFY_TOKEN",
            "MERCADOPAGO_ACCESS_TOKEN", "MERCADOPAGO_WEBHOOK_SECRET",
            "EXPORT_RETENTION_YEARS", "BACKUP_CRON", "IA_HANDOFF_ENABLED");

    private static final List<String> DOMAIN_TYPES = List.of(
            "com.turnosodontologia.domain.agenda.AgendaReference",
            "com.turnosodontologia.domain.clinico.ClinicoReference",
            "com.turnosodontologia.domain.financiero.FinancieroReference",
            "com.turnosodontologia.domain.identidad.IdentidadReference",
            "com.turnosodontologia.domain.auditoria.AuditoriaReference",
            "com.turnosodontologia.domain.notificaciones.NotificacionReference");

    private static final String NOMBRE_VARIABLE = "[A-Z][A-Z0-9_]*";

    @Test
    void estructuraDeDirectorios() {
        assertNull(firstMissingDirectory(REQUIRED_DIRS));
    }

    @Test
    void detectaDirectorioFaltante() {
        String probe = "src/main/java/com/turnosodontologia/domain/__no_existe__";
        assertEquals(probe, firstMissingDirectory(List.of(probe)));
    }

    @Test
    void plantillaDeEntornoCumpleElContrato() throws Exception {
        assertNull(envProblem(envLines()));
    }

    @Test
    void detectaVariableFaltante() throws Exception {
        List<String> lineas = envLines();
        lineas.remove("MERCADOPAGO_ACCESS_TOKEN");
        assertEquals("variable faltante: MERCADOPAGO_ACCESS_TOKEN", envProblem(lineas));
    }

    @Test
    void rechazaValoresEnLaPlantilla() throws Exception {
        List<String> lineas = envLines();
        lineas.replaceAll(linea -> linea.equals("WHATSAPP_ACCESS_TOKEN")
                ? "WHATSAPP_ACCESS_TOKEN=EAAHfakeToken1234567890" : linea);
        assertEquals("la plantilla solo lista nombres, sin valores: WHATSAPP_ACCESS_TOKEN=EAAHfakeToken1234567890",
                envProblem(lineas));
    }

    @Test
    void contratoDelHealthCheck() throws Exception {
        assertEquals("UP", new HealthCheckResponse("UP").status());
        String decisiones = read("docs/decisiones/C-02-resoluciones-stack.md");
        assertTrue(decisiones.contains("GET /health"), "GET /health no documentado");
        assertTrue(decisiones.contains("200") && decisiones.contains("{\"status\":\"UP\"}"),
                "contrato 200 {\"status\":\"UP\"} no documentado");
    }

    @Test
    void healthCheckConservaElEstadoDown() {
        assertEquals("DOWN", new HealthCheckResponse("DOWN").status());
    }

    @Test
    void tiposBaseDelDominioConservanElId() throws Exception {
        for (String className : DOMAIN_TYPES) {
            assertTrue(newDomainType(className, "ref-1").toString().contains("ref-1"),
                    "tipo base no conserva el id: " + className);
        }
    }

    @Test
    void tipoBaseRechazaIdEnBlanco() {
        InvocationTargetException error = assertThrows(InvocationTargetException.class,
                () -> newDomainType("com.turnosodontologia.domain.agenda.AgendaReference", " "));
        assertInstanceOf(IllegalArgumentException.class, error.getCause());
    }

    private static String read(String relativePath) throws Exception {
        return Files.readString(ROOT.resolve(relativePath), StandardCharsets.UTF_8);
    }

    /** Líneas no vacías ni comentadas de {@code .env.example}. */
    private static List<String> envLines() throws Exception {
        List<String> lineas = new ArrayList<>();
        for (String raw : read(".env.example").split("\\R")) {
            String linea = raw.trim();
            if (!linea.isEmpty() && !linea.startsWith("#")) {
                lineas.add(linea);
            }
        }
        return lineas;
    }

    /** La plantilla lista solo nombres de variables: así ningún valor (ni secreto) puede versionarse. */
    private static String envProblem(List<String> lineas) {
        for (String linea : lineas) {
            if (!linea.matches(NOMBRE_VARIABLE)) {
                return "la plantilla solo lista nombres, sin valores: " + linea;
            }
        }
        for (String clave : REQUIRED_ENV) {
            if (!lineas.contains(clave)) {
                return "variable faltante: " + clave;
            }
        }
        return null;
    }

    private static String firstMissingDirectory(List<String> directories) {
        for (String directory : directories) {
            if (!Files.isDirectory(ROOT.resolve(directory))) {
                return directory;
            }
        }
        return null;
    }

    private static Object newDomainType(String className, String id) throws Exception {
        Constructor<?> constructor = Class.forName(className).getConstructor(String.class);
        return constructor.newInstance(id);
    }
}
