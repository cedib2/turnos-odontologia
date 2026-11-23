package com.turnosodontologia.depositpayments;

public record WebhookResult(boolean duplicado, boolean procesado, String estado,
                            boolean conciliado) {
}
