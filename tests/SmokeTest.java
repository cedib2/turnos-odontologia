import com.turnosodontologia.interfaces.HealthCheckResponse;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class SmokeTest {
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
            "src/test/java",
            "tests");

    private static final List<String> REQUIRED_ENV = List.of(
            "APP_BASE_URL", "DATABASE_URL", "TIMEZONE", "CANCELLATION_WINDOW_HOURS",
            "WHATSAPP_ACCESS_TOKEN", "WHATSAPP_PHONE_NUMBER_ID", "WHATSAPP_WEBHOOK_VERIFY_TOKEN",
            "MERCADOPAGO_ACCESS_TOKEN", "MERCADOPAGO_WEBHOOK_SECRET",
            "EXPORT_RETENTION_YEARS", "BACKUP_CRON", "IA_HANDOFF_ENABLED");

    private static final Set<String> SENSITIVE_ENV = new LinkedHashSet<>(List.of(
            "DATABASE_URL", "WHATSAPP_ACCESS_TOKEN", "WHATSAPP_PHONE_NUMBER_ID",
            "WHATSAPP_WEBHOOK_VERIFY_TOKEN", "MERCADOPAGO_ACCESS_TOKEN", "MERCADOPAGO_WEBHOOK_SECRET"));

    private static final List<String> DOMAIN_TYPES = List.of(
            "com.turnosodontologia.domain.agenda.AgendaReference",
            "com.turnosodontologia.domain.clinico.ClinicoReference",
            "com.turnosodontologia.domain.financiero.FinancieroReference",
            "com.turnosodontologia.domain.identidad.IdentidadReference",
            "com.turnosodontologia.domain.auditoria.AuditoriaReference",
            "com.turnosodontologia.domain.notificaciones.NotificacionReference");

    private static final String SECRET_PATTERN =
            "(?i)(sk-[A-Za-z0-9]{8,}|pk_live_[A-Za-z0-9]+|EA[A-Z][A-Za-z0-9]{10,})";

    private static int total;
    private static int failures;

    private interface Case {
        void run() throws Exception;
    }

    public static void main(String[] args) {
        run("structure/directories", SmokeTest::requiredDirectories);
        run("env/template-contract", SmokeTest::envTemplateContract);
        run("health/contract", SmokeTest::healthCheckContract);
        run("domain/base-types", SmokeTest::baseDomainTypes);
        run("structure/missing-directory-detected", SmokeTest::missingDirectoryDetected);
        run("env/missing-variable-detected", SmokeTest::missingVariableDetected);
        run("env/real-secret-rejected", SmokeTest::realSecretRejected);
        run("health/down-status-preserved", SmokeTest::downStatusPreserved);
        run("domain/blank-id-rejected", SmokeTest::blankIdRejected);

        System.out.println((total - failures) + "/" + total + " cases passed");
        if (failures > 0) {
            System.err.println("FAILED: " + failures + " case(s)");
            System.exit(1);
        }
    }

    private static void run(String name, Case test) {
        total++;
        try {
            test.run();
            System.out.println("PASS " + name);
        } catch (Throwable failure) {
            failures++;
            System.err.println("FAIL " + name + ": " + failure.getMessage());
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static String read(String relativePath) throws Exception {
        return Files.readString(ROOT.resolve(relativePath), StandardCharsets.UTF_8);
    }

    private static Map<String, String> parseEnv(String content) {
        Map<String, String> env = new LinkedHashMap<>();
        for (String raw : content.split("\\R")) {
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            int separator = line.indexOf('=');
            require(separator > 0, "línea de entorno inválida: " + line);
            env.put(line.substring(0, separator).trim(), line.substring(separator + 1).trim());
        }
        return env;
    }

    private static String envProblem(Map<String, String> env) {
        for (String key : REQUIRED_ENV) {
            if (!env.containsKey(key)) {
                return "variable faltante: " + key;
            }
            if (env.get(key).isBlank()) {
                return "variable vacía: " + key;
            }
        }
        for (String key : SENSITIVE_ENV) {
            if (!"(TBD)".equals(env.get(key))) {
                return "valor sensible no es marcador (TBD): " + key;
            }
        }
        if (!"false".equals(env.get("IA_HANDOFF_ENABLED"))) {
            return "IA_HANDOFF_ENABLED debe ser false (A3)";
        }
        for (Map.Entry<String, String> entry : env.entrySet()) {
            if (entry.getValue().matches(SECRET_PATTERN)) {
                return "posible secreto real en " + entry.getKey();
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

    private static void requiredDirectories() throws Exception {
        String missing = firstMissingDirectory(REQUIRED_DIRS);
        require(missing == null, "directorio faltante: " + missing);
        require(Files.isRegularFile(ROOT.resolve("src/test/java/README.md")), "placeholder JUnit faltante");
    }

    private static void missingDirectoryDetected() {
        String probe = "src/main/java/com/turnosodontologia/domain/__no_existe__";
        require(probe.equals(firstMissingDirectory(List.of(probe))), "validador no detectó directorio faltante");
    }

    private static void envTemplateContract() throws Exception {
        require(envProblem(parseEnv(read(".env.example"))) == null, "plantilla de entorno inválida");
    }

    private static void missingVariableDetected() throws Exception {
        Map<String, String> env = new LinkedHashMap<>(parseEnv(read(".env.example")));
        env.remove("MERCADOPAGO_ACCESS_TOKEN");
        require("variable faltante: MERCADOPAGO_ACCESS_TOKEN".equals(envProblem(env)),
                "no se detectó la variable quitada");
    }

    private static void realSecretRejected() throws Exception {
        Map<String, String> env = new LinkedHashMap<>(parseEnv(read(".env.example")));
        env.put("WHATSAPP_ACCESS_TOKEN", "EAAHfakeToken1234567890");
        require("valor sensible no es marcador (TBD): WHATSAPP_ACCESS_TOKEN".equals(envProblem(env)),
                "no se rechazó el secreto falso");
    }

    private static void healthCheckContract() throws Exception {
        HealthCheckResponse response = new HealthCheckResponse("UP");
        require("UP".equals(response.status()), "health check no informa UP");
        String decisions = read("docs/decisiones/C-02-resoluciones-stack.md");
        require(decisions.contains("GET /health"), "GET /health no documentado");
        require(decisions.contains("200") && decisions.contains("{\"status\":\"UP\"}"),
                "contrato 200 {\"status\":\"UP\"} no documentado");
    }

    private static void downStatusPreserved() {
        require("DOWN".equals(new HealthCheckResponse("DOWN").status()),
                "HealthCheckResponse no conserva el estado recibido");
    }

    private static void baseDomainTypes() throws Exception {
        for (String className : DOMAIN_TYPES) {
            require(newDomainType(className, "ref-1").toString().contains("ref-1"),
                    "tipo base no conserva el id: " + className);
        }
    }

    private static void blankIdRejected() throws Exception {
        try {
            newDomainType("com.turnosodontologia.domain.agenda.AgendaReference", " ");
            throw new AssertionError("id en blanco aceptado");
        } catch (InvocationTargetException expected) {
            require(expected.getCause() instanceof IllegalArgumentException,
                    "se esperaba IllegalArgumentException");
        }
    }
}
