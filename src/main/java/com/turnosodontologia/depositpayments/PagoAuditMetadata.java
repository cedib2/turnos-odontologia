package com.turnosodontologia.depositpayments;

import com.turnosodontologia.domain.financiero.Senia;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Metadata acotada de los eventos de dinero (MP-6). */
public final class PagoAuditMetadata {

    private PagoAuditMetadata() {
    }

    public static Map<String, Object> de(Senia senia, String motivo, String correlationId) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("seniaId", senia.getId());
        if (senia.getTurnoId() != null) {
            metadata.put("turnoId", senia.getTurnoId());
        }
        if (senia.getPresupuestoId() != null) {
            metadata.put("presupuestoId", senia.getPresupuestoId());
        }
        metadata.put("monto", senia.getMonto().toPlainString());
        metadata.put("moneda", senia.getMoneda());
        metadata.put("estado", senia.getEstado());
        metadata.put("externalReference", senia.getExternalReference());
        if (motivo != null && !motivo.isBlank()) {
            metadata.put("motivo", motivo);
        }
        metadata.put("correlationId",
                correlationId == null || correlationId.isBlank()
                        ? UUID.randomUUID().toString() : correlationId);
        return Map.copyOf(metadata);
    }
}
