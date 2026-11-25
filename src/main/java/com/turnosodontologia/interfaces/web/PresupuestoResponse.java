package com.turnosodontologia.interfaces.web;

import com.turnosodontologia.domain.financiero.ItemPresupuesto;
import com.turnosodontologia.domain.financiero.Presupuesto;

import java.math.BigDecimal;
import java.util.List;

public record PresupuestoResponse(Long id, Long pacienteId, Long turnoId, String estado, String moneda,
                                  BigDecimal total, boolean activo,
                                  List<ItemPresupuestoResponse> items) {

    public static PresupuestoResponse desde(Presupuesto presupuesto) {
        List<ItemPresupuestoResponse> items = presupuesto.getItems().stream()
                .map(ItemPresupuestoResponse::desde)
                .toList();
        Long turnoId = presupuesto.getTurno() == null ? null : presupuesto.getTurno().getId();
        return new PresupuestoResponse(presupuesto.getId(), presupuesto.getPaciente().getId(), turnoId,
                presupuesto.getEstado(), presupuesto.getMoneda(), presupuesto.getTotal(),
                presupuesto.isActivo(), items);
    }
}
