package com.turnosodontologia.depositpayments;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Implementación NO-OP/simulada, equivalente a C-09 D8: registra, nunca envía. */
public final class NotificacionPagoSimulada implements NotificacionPagoPort {

    private final List<Solicitud> solicitudes = new CopyOnWriteArrayList<>();

    @Override
    public void solicitar(Long seniaId, String motivo) {
        solicitudes.add(new Solicitud(seniaId, motivo));
    }

    public List<Solicitud> solicitudes() {
        return List.copyOf(solicitudes);
    }

    public void clear() {
        solicitudes.clear();
    }

    public record Solicitud(Long seniaId, String motivo) {
    }
}
