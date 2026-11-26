package com.turnosodontologia.publicbooking;

import com.turnosodontologia.application.notificaciones.NotificacionService;
import com.turnosodontologia.application.notificaciones.NotificacionStore;
import com.turnosodontologia.application.notificaciones.Proposito;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * Conecta la reserva pública (C-09) con el outbox de WhatsApp (C-10). Corre dentro de la transacción de la
 * operación del turno: si la reserva se revierte, los avisos también (patrón outbox).
 *
 * <ul>
 *   <li>{@code turno.crear}: confirmación inmediata y recordatorio {@code anticipacion} antes del turno.</li>
 *   <li>{@code turno.reprogramar}: anula los avisos pendientes y programa los del nuevo horario.</li>
 *   <li>{@code turno.cancelar}: anula los avisos pendientes.</li>
 *   <li>{@code turno.confirmar}: sin aviso nuevo; el recordatorio programado se mantiene.</li>
 * </ul>
 *
 * La anticipación del recordatorio sigue {@code (TBD)}: sin {@code notificaciones.recordatorio.anticipacion-horas}
 * no se programa recordatorio (no se inventa un valor, RN-GL-03), solo la confirmación.
 */
public class NotificacionWhatsAppReserva implements NotificacionPublicaPort {

    private final NotificacionService notificaciones;
    private final NotificacionStore store;
    private final Clock reloj;
    private final Duration anticipacion;

    public NotificacionWhatsAppReserva(NotificacionService notificaciones, NotificacionStore store, Clock reloj,
                                       Duration anticipacion) {
        this.notificaciones = notificaciones;
        this.store = store;
        this.reloj = reloj;
        this.anticipacion = anticipacion;
    }

    @Override
    public void solicitar(Long turnoId, String operacion) {
        if (turnoId == null || operacion == null) {
            return;
        }
        switch (operacion) {
            case "turno.crear" -> programarAvisos(turnoId);
            case "turno.reprogramar" -> {
                notificaciones.anularPendientes(turnoId, "turno-reprogramado");
                programarAvisos(turnoId);
            }
            case "turno.cancelar" -> notificaciones.anularPendientes(turnoId, "turno-cancelado");
            default -> {
                // turno.confirmar u otras operaciones: no generan avisos nuevos.
            }
        }
    }

    private void programarAvisos(long turnoId) {
        NotificacionStore.DatosTurno turno = store.datosTurno(turnoId)
                .orElseThrow(() -> new IllegalStateException("Turno inexistente para notificar: " + turnoId));
        Instant ahora = reloj.instant();
        notificaciones.programar(turnoId, Proposito.CONFIRMACION, turno.telefono(), ahora);
        if (anticipacion != null) {
            Instant recordatorio = turno.inicio().minus(anticipacion);
            // Si el turno es antes de la anticipación, la confirmación ya cumple la función del recordatorio.
            if (recordatorio.isAfter(ahora)) {
                notificaciones.programar(turnoId, Proposito.RECORDATORIO, turno.telefono(), recordatorio);
            }
        }
    }
}
