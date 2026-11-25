package com.turnosodontologia.export;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExportFormatTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Test
    void jsonConSchemaVersion() throws Exception {
        byte[] artefacto = new ExportFormatService().json(snapshot(), Map.of());

        JsonNode raiz = JSON.readTree(artefacto);
        assertAll(
                () -> assertEquals(ExportFormatService.SCHEMA_VERSION, raiz.get("schemaVersion").asText()),
                () -> assertEquals("titular", raiz.get("alcance").asText()),
                () -> assertEquals(ZoneId.of("America/Argentina/Buenos_Aires").getId(), raiz.get("zona").asText()),
                () -> assertEquals(1, raiz.get("conteos").get("identidad").asInt())
        );
    }

    @Test
    void zipConManifestYChecksum() throws Exception {
        byte[] paquete = new ExportFormatService().zip(snapshot(), Map.of("origen", "test"));

        Map<String, byte[]> entradas = new java.util.HashMap<>();
        try (ZipInputStream zip = new ZipInputStream(new java.io.ByteArrayInputStream(paquete))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                entradas.put(entry.getName(), zip.readAllBytes());
            }
        }

        JsonNode manifest = JSON.readTree(entradas.get("manifest.json"));
        String checksum = HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(entradas.get("snapshot.json")));
        assertAll(
                () -> assertTrue(entradas.containsKey("manifest.json")),
                () -> assertTrue(entradas.containsKey("snapshot.json")),
                () -> assertTrue(entradas.containsKey("resumen.csv")),
                () -> assertEquals(ExportFormatService.SCHEMA_VERSION, manifest.get("schemaVersion").asText()),
                () -> assertEquals(checksum, manifest.get("checksumSha256").asText()),
                () -> assertTrue(new String(entradas.get("resumen.csv"), StandardCharsets.UTF_8)
                        .contains("dominio,conteo"))
        );
    }

    private ExportSnapshot snapshot() {
        return new ExportSnapshot(Instant.parse("2026-11-24T10:00:00Z"),
                ZoneId.of("America/Argentina/Buenos_Aires").getId(), "titular",
                Map.of("identidad", 1, "agenda", 1, "clinico", 0, "financial", 0, "auditoria", 1),
                List.of("identidad", "agenda", "clinico", "financial", "auditoria"),
                true, List.of("clinical", "financial"), ExportResultado.PARCIAL);
    }
}
