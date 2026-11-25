package com.turnosodontologia.invoicing;

/** Resultado de la conciliación: nunca modifica la seña (AR-10). */
public record ConciliacionResponse(Long comprobanteId, boolean conciliado, String motivo) {
}
