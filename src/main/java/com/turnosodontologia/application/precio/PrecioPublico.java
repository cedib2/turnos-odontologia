package com.turnosodontologia.application.precio;

import java.math.BigDecimal;
import java.util.List;

/**
 * Superficie pública del precio (C-21, RN-SE-06, US-016). Modelo aprobado en C-01 A5=2: suscripción plana
 * todo incluido, sin desglose de costos de API. Solo expone datos comerciales: ningún campo de configuración
 * de integraciones (C-10/C-15) forma parte de este contrato.
 */
public record PrecioPublico(String modelo, String estado, String plan, BigDecimal monto, String moneda,
                            String periodicidad, List<String> incluye, boolean todoIncluido,
                            boolean desgloseCostosApi, boolean packsAdicionales, String vigenteDesde,
                            String nota) {

    public static final String MODELO = "SUSCRIPCION_PLANA_TODO_INCLUIDO";
    public static final String PUBLICADO = "PUBLICADO";
    public static final String PENDIENTE = "PENDIENTE";

    public PrecioPublico {
        incluye = incluye == null ? List.of() : List.copyOf(incluye);
    }
}
