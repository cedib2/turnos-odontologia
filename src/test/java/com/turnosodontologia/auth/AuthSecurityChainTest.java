package com.turnosodontologia.auth;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthSecurityChainTest {

    @Test
    void dependenciaYCadenaDeSeguridadVivenSoloEnAuth() throws IOException {
        assertTrue(Files.readString(Path.of("pom.xml")).contains("spring-boot-starter-security"));

        Path configuracion = Path.of("src", "main", "java", "com", "turnosodontologia",
                "auth", "AuthSecurityConfig.java");
        assertTrue(Files.exists(configuracion));
        String contenido = Files.readString(configuracion);
        assertTrue(contenido.contains("SecurityFilterChain"));
        assertTrue(contenido.contains("securityMatcher(\"/auth/**\")"));

        Path paqueteAuth = Path.of("src", "main", "java", "com", "turnosodontologia", "auth")
                .toAbsolutePath();
        List<String> fueraDeAuth = new java.util.ArrayList<>();
        try (Stream<Path> archivos = Files.walk(Path.of("src", "main", "java"))) {
            for (Path archivo : archivos.filter(Files::isRegularFile).toList()) {
                if (archivo.toAbsolutePath().startsWith(paqueteAuth)) {
                    continue;
                }
                String codigo = Files.readString(archivo);
                if (codigo.contains("SecurityFilterChain") || codigo.contains("@EnableWebSecurity")) {
                    fueraDeAuth.add(archivo.toString());
                }
            }
        }
        assertEquals(List.of(), fueraDeAuth);
    }

    @Test
    void losMappingsAuthSoloAparecenEnElPaqueteAuth() throws IOException {
        Path paqueteAuth = Path.of("src", "main", "java", "com", "turnosodontologia", "auth")
                .toAbsolutePath();
        List<String> controladoresAuth = new java.util.ArrayList<>();
        List<String> fueraDeAuth = new java.util.ArrayList<>();
        try (Stream<Path> archivos = Files.walk(Path.of("src", "main", "java"))) {
            for (Path archivo : archivos.filter(Files::isRegularFile).toList()) {
                String codigo = Files.readString(archivo);
                if (!codigo.contains("@RequestMapping(\"/auth\")")) {
                    continue;
                }
                if (archivo.toAbsolutePath().startsWith(paqueteAuth)) {
                    controladoresAuth.add(archivo.toString());
                } else {
                    fueraDeAuth.add(archivo.toString());
                }
            }
        }
        assertFalse(controladoresAuth.isEmpty());
        assertEquals(List.of(), fueraDeAuth);
    }
}
