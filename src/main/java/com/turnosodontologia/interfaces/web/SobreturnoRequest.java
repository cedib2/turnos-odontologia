package com.turnosodontologia.interfaces.web;

import java.time.Instant;

public record SobreturnoRequest(Long turnoId, Instant nuevoInicio, String motivo, String actor) {
}
