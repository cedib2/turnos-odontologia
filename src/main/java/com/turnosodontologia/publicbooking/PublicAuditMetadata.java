package com.turnosodontologia.publicbooking;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public final class PublicAuditMetadata {

    private PublicAuditMetadata() {
    }

    public static Map<String, Object> de(Long turnoId, String idempotencyKey, String motivo) {
        return de(turnoId, idempotencyKey, motivo, null);
    }

    /**
     * Con {@code inicioAnterior} (ISO-8601): inicio del turno antes de cancelarlo o reprogramarlo. Es el
     * contrato que usa C-22 para medir el cumplimiento de la ventana de 48 hs hábiles.
     */
    public static Map<String, Object> de(Long turnoId, String idempotencyKey, String motivo,
                                         Instant inicioAnterior) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("turnoId", turnoId);
        metadata.put("canal", "link");
        metadata.put("origen", "link");
        metadata.put("idempotencyKeyHash", hash(idempotencyKey));
        if (motivo != null && !motivo.isBlank()) {
            metadata.put("motivo", motivo);
        }
        if (inicioAnterior != null) {
            metadata.put("inicioAnterior", inicioAnterior.toString());
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
