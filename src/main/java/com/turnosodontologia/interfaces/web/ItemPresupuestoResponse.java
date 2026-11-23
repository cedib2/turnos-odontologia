package com.turnosodontologia.interfaces.web;

import com.turnosodontologia.domain.financiero.ItemPresupuesto;

import java.math.BigDecimal;

public record ItemPresupuestoResponse(Long id, Long tratamientoId, int cantidad,
                                      BigDecimal precio, String descripcion) {

    public static ItemPresupuestoResponse desde(ItemPresupuesto item) {
        return new ItemPresupuestoResponse(item.getId(), item.getTratamiento().getId(),
                item.getCantidad(), item.getPrecio(), item.getDescripcion());
    }
}
