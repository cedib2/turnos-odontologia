package com.turnosodontologia.invoicing;

import com.turnosodontologia.domain.financiero.Comprobante;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Metadata cerrada de los eventos de facturación (AR-12): sin CAE ni datos personales. */
public final class FacturacionAuditMetadata {

    private FacturacionAuditMetadata() {
    }

    public static Map<String, Object> de(Comprobante comprobante, String motivo,
                                         String correlationId) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("comprobanteId", comprobante.getId());
        metadata.put("seniaId", comprobante.getSeniaId());
        if (comprobante.getPresupuestoId() != null) {
            metadata.put("presupuestoId", comprobante.getPresupuestoId());
        }
        if (comprobante.getTurnoId() != null) {
            metadata.put("turnoId", comprobante.getTurnoId());
        }
        metadata.put("monto", comprobante.getMonto().toPlainString());
        metadata.put("moneda", comprobante.getMoneda());
        metadata.put("tipo", comprobante.getTipo());
        metadata.put("estado", comprobante.getEstado());
        if (motivo != null && !motivo.isBlank()) {
            metadata.put("motivo", motivo);
        }
        metadata.put("correlationId",
                correlationId == null || correlationId.isBlank()
                        ? UUID.randomUUID().toString() : correlationId);
        return Map.copyOf(metadata);
    }
}
