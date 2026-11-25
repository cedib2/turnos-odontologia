package com.turnosodontologia.application.precio;

import java.math.BigDecimal;
import java.util.List;

/**
 * Precio único, público y predecible (US-016). Modelo plano todo incluido, sin desglose de
 * costos de API para el consultorio (C-01 A5, enmienda a RN-SE-06).
 */
public record PrecioPublico(EstadoPrecio estado, String modelo, BigDecimal monto, String moneda,
                            String periodicidad, BigDecimal total, List<ConceptoIncluido> incluye,
                            List<ConceptoIncluido> cargosAdicionales, boolean desgloseCostosApi,
                            List<String> pendientes, String decision) {

    public PrecioPublico {
        incluye = List.copyOf(incluye);
        cargosAdicionales = List.copyOf(cargosAdicionales);
        pendientes = List.copyOf(pendientes);
        if (!cargosAdicionales.isEmpty()) {
            throw new IllegalArgumentException("El plan no admite cargos adicionales ni packs (RN-NT-04)");
        }
        if (desgloseCostosApi) {
            throw new IllegalArgumentException("C-01 A5: sin desglose de costos de API");
        }
        if (estado == EstadoPrecio.PUBLICADO && (monto == null || total == null || total.compareTo(monto) != 0)) {
            throw new IllegalArgumentException("El total publicado debe coincidir con el precio del plan");
        }
        if (estado == EstadoPrecio.PENDIENTE_DEFINICION && (monto != null || total != null)) {
            throw new IllegalArgumentException("Sin definición del PO no se publica una cifra");
        }
    }
}
