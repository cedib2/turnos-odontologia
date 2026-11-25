package com.turnosodontologia.publicbooking;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GestionarRequest(Long turnoId,
                               @JsonProperty("idempotency_key") String idempotencyKey) {
}
