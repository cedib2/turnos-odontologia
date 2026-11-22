package com.turnosodontologia.publicbooking;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class NotificacionSimulada implements NotificacionPublicaPort {

    private final List<Solicitud> solicitudes = new CopyOnWriteArrayList<>();

    @Override
    public void solicitar(Long turnoId, String operacion) {
        solicitudes.add(new Solicitud(turnoId, operacion));
    }

    public List<Solicitud> solicitudes() {
        return List.copyOf(solicitudes);
    }

    public void clear() {
        solicitudes.clear();
    }

    public record Solicitud(Long turnoId, String operacion) {
    }
}
