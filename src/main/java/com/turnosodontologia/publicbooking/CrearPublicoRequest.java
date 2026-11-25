package com.turnosodontologia.publicbooking;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public record CrearPublicoRequest(Long pacienteId, Long profesionalId, Long sillonId,
                                  String codigoTratamiento, Instant inicio,
                                  @JsonProperty("idempotency_key") String idempotencyKey) {
}
