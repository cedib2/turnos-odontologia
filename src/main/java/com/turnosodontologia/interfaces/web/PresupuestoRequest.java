package com.turnosodontologia.interfaces.web;

import java.util.List;

public record PresupuestoRequest(Long pacienteId, Long turnoId, List<ItemPresupuestoRequest> items) {
}
