package com.turnosodontologia.export;

import java.util.Map;

public record OperacionRegistrada(String correlationId, String operacion, String resultado,
                                  String motivo, Map<String, Object> metadata,
                                  String tenantId, String actorNombre) {
}
