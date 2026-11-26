package com.turnosodontologia.publicbooking;

import com.turnosodontologia.application.notificaciones.AvisosTurno;

/**
 * Conecta la reserva pública (C-09) con los avisos de WhatsApp (C-10): crear y reprogramar programan avisos,
 * cancelar los anula y confirmar no genera avisos nuevos (el recordatorio programado se mantiene).
 */
public class NotificacionWhatsAppReserva implements NotificacionPublicaPort {

    private final AvisosTurno avisos;

    public NotificacionWhatsAppReserva(AvisosTurno avisos) {
        this.avisos = avisos;
    }

    @Override
    public void solicitar(Long turnoId, String operacion) {
        if (turnoId == null || operacion == null) {
            return;
        }
        switch (operacion) {
            case "turno.crear" -> avisos.creado(turnoId);
            case "turno.reprogramar" -> avisos.reprogramado(turnoId);
            case "turno.cancelar" -> avisos.cancelado(turnoId);
            default -> {
                // turno.confirmar u otras operaciones: no generan avisos nuevos.
            }
        }
    }
}
