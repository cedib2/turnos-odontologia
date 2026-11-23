package com.turnosodontologia.depositpayments;

public record WebhookSenia(String externalReference, String status, String paymentId, String ts) {
}
