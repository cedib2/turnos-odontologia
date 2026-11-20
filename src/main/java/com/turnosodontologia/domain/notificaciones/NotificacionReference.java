package com.turnosodontologia.domain.notificaciones;

public record NotificacionReference(String id) {
    public NotificacionReference {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id es obligatorio");
        }
    }
}
