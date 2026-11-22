package com.turnosodontologia.governance;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guardia de C-20: C-01 A3 decidió «solo plantillas de WhatsApp, sin IA» con
 * {@code IA_HANDOFF_ENABLED=false}. Mientras esa decisión siga vigente no puede aparecer código de
 * asistente conversacional ni de handoff de IA. Habilitarlo exige una decisión nueva del PO que
 * reemplace A3, y recién entonces se retira esta guardia junto con la implementación de C-20.
 */
class IaHandoffDiferidoTest {

    @Test
    void laDecisionA3SigueVigenteYDocumentada() throws IOException {
        String c01 = Files.readString(Path.of("docs", "decisiones", "C-01-decisiones-habilitantes.md"));
        assertTrue(c01.contains("### A3 — Solo plantillas de WhatsApp, sin IA"));
        assertTrue(c01.contains("`IA_HANDOFF_ENABLED=false`, `C-20` diferido"));
        assertTrue(Files.exists(Path.of("docs", "decisiones", "C-20-ia-conversacional-handoff-diferido.md")));
        assertTrue(Files.readAllLines(Path.of(".env.example")).contains("IA_HANDOFF_ENABLED"),
                "la bandera existe solo como nombre, sin valor");
    }

    @Test
    void noHayCodigoDeAsistenteConversacionalNiHandoffDeIa() throws IOException {
        List<String> hallazgos = new ArrayList<>();
        List<String> prohibidos = List.of("ia_handoff_enabled", "conversacional", "handoff", "chatbot",
                "llm", "openai", "anthropic");
        for (Path raiz : List.of(Path.of("src", "main", "java"), Path.of("src", "main", "resources"))) {
            try (Stream<Path> archivos = Files.walk(raiz)) {
                for (Path archivo : archivos.filter(Files::isRegularFile).toList()) {
                    String contenido = (archivo.getFileName() + "\n" + Files.readString(archivo))
                            .toLowerCase(Locale.ROOT);
                    for (String prohibido : prohibidos) {
                        if (contenido.matches("(?s).*\\b" + prohibido + "\\b.*")) {
                            hallazgos.add(archivo + " -> " + prohibido);
                        }
                    }
                }
            }
        }
        assertEquals(List.of(), hallazgos);
    }
}
