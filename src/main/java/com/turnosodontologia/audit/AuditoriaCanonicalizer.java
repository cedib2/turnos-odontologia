package com.turnosodontologia.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class AuditoriaCanonicalizer {

    public static final String GENESIS = "0000000000000000000000000000000000000000000000000000000000000000";

    private static final String SEPARADOR = "\u001f";
    private final ObjectMapper objectMapper = new ObjectMapper();

    public record Fila(Long id, Instant fecha, Long actorId, String actorNombre, String actorRol,
                       String actorJti, String modo, String accion, String entidad, Long entidadId,
                       String ip, String dispositivo, String motivo, Map<String, Object> metadata,
                       String correlationId, String hashAnterior) {
    }

    public String payload(Fila fila) {
        List<String> campos = new ArrayList<>();
        campos.add(texto(fila.id()));
        campos.add(fila.fecha() == null ? "null" : fila.fecha().toString());
        campos.add(texto(fila.actorId()));
        campos.add(texto(fila.actorNombre()));
        campos.add(texto(fila.actorRol()));
        campos.add(texto(fila.actorJti()));
        campos.add(texto(fila.modo()));
        campos.add(texto(fila.accion()));
        campos.add(texto(fila.entidad()));
        campos.add(texto(fila.entidadId()));
        campos.add(texto(fila.ip()));
        campos.add(texto(fila.dispositivo()));
        campos.add(texto(fila.motivo()));
        campos.add(metadata(fila.metadata()));
        campos.add(texto(fila.correlationId()));
        campos.add(texto(fila.hashAnterior()));
        return String.join(SEPARADOR, campos);
    }

    public String hash(Fila fila) {
        return hash(payload(fila));
    }

    public String hash(String payload) {
        try {
            byte[] resumen = MessageDigest.getInstance("SHA-256")
                    .digest(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder resultado = new StringBuilder(resumen.length * 2);
            for (byte parte : resumen) {
                resultado.append(String.format("%02x", parte));
            }
            return resultado.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }

    public Map<String, Object> leerMetadata(String json) {
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("metadata inválida en auditoria_log", e);
        }
    }

    private String texto(Object valor) {
        return valor == null ? "null" : valor.toString();
    }

    private String metadata(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return "{}";
        }
        return new java.util.TreeMap<>(metadata).entrySet().stream()
                .map(entrada -> escapar(entrada.getKey()) + ":" + valor(entrada.getValue()))
                .reduce((a, b) -> a + "," + b)
                .orElse("{}")
                .transform(texto -> "{" + texto + "}");
    }

    private String valor(Object valor) {
        if (valor == null) {
            return "null";
        }
        if (valor instanceof Map<?, ?> mapa) {
            java.util.TreeMap<String, String> ordenado = new java.util.TreeMap<>();
            mapa.forEach((clave, contenido) -> ordenado.put(String.valueOf(clave), valor(contenido)));
            return "{" + String.join(",", ordenado.entrySet().stream()
                    .map(entrada -> escapar(entrada.getKey()) + ":" + entrada.getValue()).toList()) + "}";
        }
        if (valor instanceof Iterable<?> iterable) {
            List<String> elementos = new ArrayList<>();
            iterable.forEach(elemento -> elementos.add(valor(elemento)));
            return "[" + String.join(",", elementos) + "]";
        }
        if (valor instanceof Boolean) {
            return "b:" + valor;
        }
        if (valor instanceof Number) {
            return "n:" + valor;
        }
        return "s:" + escapar(valor.toString());
    }

    private String escapar(String valor) {
        return valor.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace(SEPARADOR, "\\u001f")
                .replace(",", "\\,")
                .replace(":", "\\:");
    }
}
