package com.turnosodontologia.depositpayments;

import java.math.BigDecimal;

/** Frente de cobro de Mercado Pago (MP-2), sin SDK ni dependencias nuevas. */
public interface MercadoPagoPort {

    String crearPreferencia(String externalReference, BigDecimal monto, String idempotencyKey);

    void reembolsar(String paymentId, String externalReference, BigDecimal monto);
}
