package com.turnosodontologia.publicbooking;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.Map;

public final class PublicAuditMetadata {

    private PublicAuditMetadata() {
    }

    public static Map<String, Object> de(Long turnoId, String idempotencyKey, String motivo) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("turnoId", turnoId);
        metadata.put("canal", "link");
        metadata.put("origen", "link");
        metadata.put("idempotencyKeyHash", hash(idempotencyKey));
        if (motivo != null && !motivo.isBlank()) {
            metadata.put("motivo", motivo);
        }
        return metadata;
    }

    private static String hash(String clave) {
        try {
            byte[] resumen = MessageDigest.getInstance("SHA-256")
                    .digest(String.valueOf(clave).getBytes(StandardCharsets.UTF_8));
            StringBuilder texto = new StringBuilder();
            for (byte parte : resumen) {
                texto.append(String.format("%02x", parte));
            }
            return texto.toString();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
