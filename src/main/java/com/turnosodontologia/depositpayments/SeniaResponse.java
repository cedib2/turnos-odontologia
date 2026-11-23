package com.turnosodontologia.depositpayments;

public record SeniaResponse(Long id, String externalReference, String estado, String moneda,
                            String monto, String preferenceId) {
}
