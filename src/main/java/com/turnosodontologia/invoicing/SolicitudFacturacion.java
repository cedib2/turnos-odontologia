package com.turnosodontologia.invoicing;

import java.math.BigDecimal;

/** Datos no secretos enviados al proveedor (AR-4, AR-5, AR-7). */
public record SolicitudFacturacion(String tipo, String referencia, String concepto, BigDecimal monto,
                                   String moneda, String letra, String puntoVenta) {
}
