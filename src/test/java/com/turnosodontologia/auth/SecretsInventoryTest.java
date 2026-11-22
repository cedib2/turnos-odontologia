package com.turnosodontologia.auth;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecretsInventoryTest {

    @Test
    void envExampleExisteYContieneSoloNombresDeVariables() throws IOException {
        Path ejemplo = Path.of(".env.example");

        assertTrue(Files.exists(ejemplo), ".env.example debe existir");
        List<String> lineas = Files.readAllLines(ejemplo).stream()
                .filter(linea -> !linea.isBlank())
                .toList();
        assertFalse(lineas.isEmpty(), ".env.example no puede estar vacío");
        assertTrue(lineas.containsAll(AuthEnvironment.VARIABLES));
        for (String linea : lineas) {
            assertTrue(linea.matches("[A-Z][A-Z0-9_]*"),
                    "Solo nombres de variables sin valores: " + linea);
        }
    }

    @Test
    void gitignoreIgnoraEnvYConservaElEjemplo() throws IOException {
        List<String> lineas = Files.readAllLines(Path.of(".gitignore"));

        assertTrue(lineas.contains(".env"));
        assertTrue(lineas.contains(".env.*"));
        assertTrue(lineas.contains("!.env.example"));
    }
}
