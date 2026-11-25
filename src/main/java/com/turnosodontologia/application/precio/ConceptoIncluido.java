package com.turnosodontologia.application.precio;

import java.math.BigDecimal;

/** Capacidad incluida en el plan. Por decisión C-01 A5 nunca tiene cargo adicional. */
public record ConceptoIncluido(String codigo, String descripcion, BigDecimal cargoAdicional) {

    public ConceptoIncluido {
        if (cargoAdicional == null || cargoAdicional.signum() != 0) {
            throw new IllegalArgumentException("Un concepto incluido no puede tener cargo adicional: " + codigo);
        }
    }

    static ConceptoIncluido incluido(String codigo, String descripcion) {
        return new ConceptoIncluido(codigo, descripcion, BigDecimal.ZERO);
    }
}
