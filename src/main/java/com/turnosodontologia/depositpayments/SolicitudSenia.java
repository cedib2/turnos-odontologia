package com.turnosodontologia.depositpayments;

import java.math.BigDecimal;

public record SolicitudSenia(Long turnoId, Long presupuestoId, BigDecimal monto,
                             String idempotencyKey) {
}
