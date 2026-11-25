package com.turnosodontologia.export;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class ExportFormatService {

    public static final String SCHEMA_VERSION = "export-backup-restore";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ExportArtifactSanitizer sanitizer = new ExportArtifactSanitizer();

    public byte[] json(ExportSnapshot snapshot, Map<String, Object> metadata) {
        ObjectNode raiz = base(snapshot, metadata);
        try {
            return objectMapper.writeValueAsBytes(raiz);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo serializar la exportación JSON", e);
        }
    }

    public byte[] zip(ExportSnapshot snapshot, Map<String, Object> metadata) {
        byte[] contenido = json(snapshot, metadata);
        byte[] resumen = resumen(snapshot);
        byte[] manifest = manifest(snapshot, contenido);
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(salida)) {
            escribir(zip, "manifest.json", manifest);
            escribir(zip, "snapshot.json", contenido);
            escribir(zip, "resumen.csv", resumen);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo serializar la exportación ZIP", e);
        }
        return salida.toByteArray();
    }

    private ObjectNode base(ExportSnapshot snapshot, Map<String, Object> metadata) {
        sanitizer.validar(metadata == null ? Map.of() : metadata);
        ObjectNode raiz = objectMapper.createObjectNode();
        raiz.put("schemaVersion", SCHEMA_VERSION);
        raiz.put("generadoEn", snapshot.generadoEn().toString());
        raiz.put("zona", snapshot.zona());
        raiz.put("alcance", snapshot.alcance());
        raiz.put("resultado", snapshot.resultado().name());
        raiz.put("partialExport", snapshot.partialExport());
        ObjectNode conteos = raiz.putObject("conteos");
        snapshot.conteos().forEach(conteos::put);
        raiz.putArray("dominios").addAll(snapshot.dominios().stream()
                .map(objectMapper.getNodeFactory()::textNode).toList());
        raiz.putArray("unintegratedModules").addAll(snapshot.unintegratedModules().stream()
                .map(objectMapper.getNodeFactory()::textNode).toList());
        ObjectNode datos = raiz.putObject("metadata");
        (metadata == null ? Map.<String, Object>of() : metadata).forEach(
                (clave, valor) -> datos.set(clave, objectMapper.valueToTree(valor)));
        return raiz;
    }

    private byte[] manifest(ExportSnapshot snapshot, byte[] contenido) {
        try {
            ObjectNode raiz = objectMapper.createObjectNode();
            raiz.put("schemaVersion", SCHEMA_VERSION);
            raiz.put("generadoEn", snapshot.generadoEn().toString());
            raiz.put("resultado", snapshot.resultado().name());
            raiz.put("checksumSha256", HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(contenido)));
            raiz.putArray("archivos").add("snapshot.json").add("resumen.csv");
            return objectMapper.writeValueAsBytes(raiz);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo serializar el manifest", e);
        }
    }

    private byte[] resumen(ExportSnapshot snapshot) {
        StringBuilder csv = new StringBuilder("dominio,conteo\n");
        snapshot.conteos().forEach((dominio, conteo) ->
                csv.append(dominio).append(',').append(conteo).append('\n'));
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private void escribir(ZipOutputStream zip, String nombre, byte[] contenido) throws Exception {
        zip.putNextEntry(new ZipEntry(nombre));
        zip.write(contenido);
        zip.closeEntry();
    }
}
