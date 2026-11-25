package com.turnosodontologia.publicbooking;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public record ReprogramarRequest(Long turnoId, Instant inicio,
                                 @JsonProperty("idempotency_key") String idempotencyKey) {
}
