package com.turnosodontologia.application.notificaciones;

public record PlantillaWhatsApp(long id, String codigoMeta, Proposito proposito, String idioma,
                                String estadoAprobacion, String motivoRechazo, boolean activo) {

    public boolean aprobada() {
        return activo && "APROBADA".equals(estadoAprobacion);
    }
}
