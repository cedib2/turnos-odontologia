package com.turnosodontologia.invoicing;

import com.turnosodontologia.domain.financiero.Comprobante;

import java.math.BigDecimal;

/** Representación acotada del comprobante; el CAE solo aparece en la superficie de prueba. */
public record ComprobanteResponse(Long id, Long seniaId, Long presupuestoId, Long turnoId,
                                  String tipo, String estado, String moneda, BigDecimal monto,
                                  String concepto, String cae, int intentos, boolean conciliado) {

    public static ComprobanteResponse de(Comprobante comprobante) {
        return new ComprobanteResponse(comprobante.getId(), comprobante.getSeniaId(),
                comprobante.getPresupuestoId(), comprobante.getTurnoId(), comprobante.getTipo(),
                comprobante.getEstado(), comprobante.getMoneda(), comprobante.getMonto(),
                comprobante.getConcepto(), comprobante.getCae(), comprobante.getIntentos(),
                comprobante.isConciliado());
    }
}
