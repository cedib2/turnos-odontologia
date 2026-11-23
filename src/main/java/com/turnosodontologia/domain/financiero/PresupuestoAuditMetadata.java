package com.turnosodontologia.domain.financiero;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

public final class PresupuestoAuditMetadata {

    private PresupuestoAuditMetadata() {
    }

    public static Map<String, Object> de(Long presupuestoId, Long turnoId, BigDecimal total,
                                         int cantidadItems, String motivo) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("presupuestoId", presupuestoId);
        if (turnoId != null) {
            metadata.put("turnoId", turnoId);
        }
        metadata.put("total", total.toPlainString());
        metadata.put("moneda", Presupuesto.MONEDA);
        metadata.put("cantidadItems", cantidadItems);
        if (motivo != null && !motivo.isBlank()) {
            metadata.put("motivo", motivo);
        }
        return Map.copyOf(metadata);
    }
}
