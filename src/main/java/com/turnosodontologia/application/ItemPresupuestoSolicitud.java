package com.turnosodontologia.application;

import java.math.BigDecimal;

public record ItemPresupuestoSolicitud(Long tratamientoId, int cantidad, BigDecimal precio, String descripcion) {
}
