package com.turnosodontologia.domain.financiero;

public class PresupuestoNoModificableException extends RuntimeException {

    public PresupuestoNoModificableException(String mensaje) {
        super(mensaje);
    }
}
