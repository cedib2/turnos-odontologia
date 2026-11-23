package com.turnosodontologia.interfaces.web;

import java.math.BigDecimal;

public record ItemPresupuestoRequest(Long tratamientoId, int cantidad, BigDecimal precio, String descripcion) {
}
