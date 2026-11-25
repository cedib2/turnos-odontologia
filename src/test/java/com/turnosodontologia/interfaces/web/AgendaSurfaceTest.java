package com.turnosodontologia.interfaces.web;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgendaSurfaceTest {

    @Test
    void losControladoresEstanRestringidosAProfileDevTest() {
        for (Class<?> controller : List.of(AgendaController.class, TurnoController.class,
                BloqueoController.class, SobreturnoController.class,
                TratamientoController.class, HorarioController.class, FeriadoController.class,
                PresupuestoController.class)) {
            assertNotNull(controller.getAnnotation(RestController.class));
            Profile profile = controller.getAnnotation(Profile.class);
            assertNotNull(profile, controller.getSimpleName() + " sin @Profile");
            assertTrue(Arrays.asList(profile.value()).containsAll(List.of("dev", "test")),
                    controller.getSimpleName() + " debe estar en dev y test");
        }
    }

    @Test
    void laSuperficieNoLeeIdentidadNiExigeAutenticacion() throws Exception {
        Path raiz = Path.of("src", "main", "java").toAbsolutePath();
        String codigo;
        try (Stream<Path> archivos = Files.walk(raiz)) {
            codigo = archivos.filter(Files::isRegularFile)
                    .filter(ruta -> ruta.toString().endsWith(".java"))
                    .filter(ruta -> !ruta.toString().replace('\\', '/')
                            .contains("/com/turnosodontologia/auth/"))
                    // C-16/AR-7: cabeceras salientes al proveedor ARCA, no identidad de request.
                    .filter(ruta -> !ruta.toString().replace('\\', '/')
                            .contains("/com/turnosodontologia/invoicing/FacturacionCredentialHeaders.java"))
                    .map(ruta -> {
                        try {
                            return Files.readString(ruta);
                        } catch (Exception e) {
                            throw new IllegalStateException(e);
                        }
                    })
                    .collect(Collectors.joining("\n"));
        }
        for (String prohibido : List.of("SecurityContext", "@RequestHeader", "@CookieValue",
                "HttpServletRequest", "getHeader(", "Authorization", "Bearer ",
                "@AuthenticationPrincipal", "UserDetails", "JwtDecoder", "\"/auth")) {
            assertFalse(codigo.contains(prohibido), "Token prohibido en superficie: " + prohibido);
        }
    }

    @Test
    void laSuperficieNoExponePacientePdfCajaNiCobro() throws Exception {
        Path raiz = Path.of("src", "main", "java", "com", "turnosodontologia",
                "interfaces", "web").toAbsolutePath();
        String codigo;
        try (Stream<Path> archivos = Files.walk(raiz)) {
            codigo = archivos.filter(Files::isRegularFile)
                    .filter(ruta -> ruta.toString().endsWith(".java"))
                    .map(ruta -> {
                        try {
                            return Files.readString(ruta);
                        } catch (Exception e) {
                            throw new IllegalStateException(e);
                        }
                    })
                    .collect(Collectors.joining("\n"));
        }
        for (String prohibido : List.of("\"/pacientes", "\"/paciente", "\"/pdf", "\"/caja",
                "\"/cobros", "\"/cobro")) {
            assertFalse(codigo.contains(prohibido), "Superficie no prevista: " + prohibido);
        }
    }
}
