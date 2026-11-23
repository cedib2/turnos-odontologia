package com.turnosodontologia.depositpayments;

import java.math.BigDecimal;

public record RefundResponse(Long seniaId, String estado, BigDecimal montoReembolsado) {
}
