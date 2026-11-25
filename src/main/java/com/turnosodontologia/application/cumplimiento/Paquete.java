package com.turnosodontologia.application.cumplimiento;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Contenido de una exportación o respaldo (C-17). Cada fila es el {@code to_jsonb} textual de PostgreSQL,
 * de modo que el restore y su verificación comparan exactamente el mismo texto.
 * Formato técnico de trabajo: JSON versionado; el formato oficial de entrega sigue (TBD, RN-SE-03).
 */
public record Paquete(int formatoVersion, String tipo, String alcance, String generadoAt,
                      Map<String, List<String>> tablas, Map<String, Long> secuencias,
                      Map<String, Object> anexos) {

    public static final int VERSION = 1;
    public static final String FORMATO = "JSON-v1";

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);

    public Paquete {
        tablas = tablas == null ? Map.of() : new TreeMap<>(tablas);
        secuencias = secuencias == null ? Map.of() : new TreeMap<>(secuencias);
        anexos = anexos == null ? Map.of() : new LinkedHashMap<>(anexos);
    }

    public long totalFilas() {
        return tablas.values().stream().mapToLong(List::size).sum();
    }

    public byte[] serializar() {
        try {
            return MAPPER.writeValueAsBytes(this);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo serializar el paquete", e);
        }
    }

    public static Paquete deserializar(byte[] contenido) {
        try {
            return MAPPER.readValue(contenido, Paquete.class);
        } catch (IOException e) {
            throw new IllegalStateException("Paquete ilegible", e);
        }
    }

    public static String sha256(byte[] contenido) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(contenido));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public static String sha256(String texto) {
        return sha256(texto.getBytes(StandardCharsets.UTF_8));
    }
}
