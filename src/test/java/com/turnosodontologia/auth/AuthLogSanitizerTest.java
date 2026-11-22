package com.turnosodontologia.auth;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthLogSanitizerTest {

    @Test
    void enmascaraContrasenasHashesJwtYSemillasDeFirma() {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("password", "clave-del-usuario");
        datos.put("hash_password", "$2a$10$hashDelUsuario");
        datos.put("jwt", "access.token.valido");
        datos.put("refresh_hash", "hashDelRefresh");
        datos.put("auth_jwt_secret", "semillaDeFirma");
        datos.put("usuario", "ana@example.com");

        Map<String, Object> sanitizado = AuthLogSanitizer.mask(datos);

        assertEquals("***", sanitizado.get("password"));
        assertEquals("***", sanitizado.get("hash_password"));
        assertEquals("***", sanitizado.get("jwt"));
        assertEquals("***", sanitizado.get("refresh_hash"));
        assertEquals("***", sanitizado.get("auth_jwt_secret"));
        assertEquals("ana@example.com", sanitizado.get("usuario"));
    }

    @Test
    void elCodigoFuenteNoRegistraDatosSensiblesEnLogs() throws IOException {
        Path raiz = Path.of("src", "main", "java", "com", "turnosodontologia", "auth");
        List<String> hallazgos = new ArrayList<>();
        if (Files.exists(raiz)) {
            try (Stream<Path> archivos = Files.walk(raiz)) {
                for (Path archivo : archivos.filter(Files::isRegularFile).filter(p -> p.toString().endsWith(".java")).toList()) {
                    String contenido = Files.readString(archivo);
                    for (String linea : contenido.split("\\R")) {
                        String normalizada = linea.toLowerCase(java.util.Locale.ROOT);
                        if (normalizada.contains("log.") && (normalizada.contains("password")
                                || normalizada.contains("hash_password") || normalizada.contains("jwt")
                                || normalizada.contains("secret") || normalizada.contains("refresh"))) {
                            hallazgos.add(archivo + " -> " + linea.strip());
                        }
                    }
                }
            }
        }

        assertEquals(List.of(), hallazgos);
    }

    @Test
    void noHayLiteralesDeClaveEnElCodigoFuente() throws IOException {
        List<String> hallazgos = new ArrayList<>();
        try (Stream<Path> archivos = Files.walk(Path.of("src", "main"))) {
            for (Path archivo : archivos.filter(Files::isRegularFile).toList()) {
                String contenido = Files.readString(archivo);
                if (contenido.matches("(?s).*(?i)(secret|password|token|clave)[a-z0-9_]*\\s*[:=]\\s*[\"'][A-Za-z0-9+/=_-]{16,}[\"'].*")) {
                    hallazgos.add(archivo.toString());
                }
            }
        }

        assertEquals(List.of(), hallazgos);
        assertTrue(Files.readString(Path.of(".env.example")).lines().noneMatch(linea -> linea.contains("=")));
    }
}
