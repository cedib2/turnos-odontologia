package com.turnosodontologia.application.notificaciones;

public enum EstadoNotificacion {
    PENDIENTE,
    ENVIADA,
    ENTREGADA,
    LEIDA,
    CONFIRMADA,
    FALLIDA,
    BLOQUEADA;

    public boolean esFinal() {
        return this == CONFIRMADA || this == FALLIDA || this == BLOQUEADA;
    }
}
