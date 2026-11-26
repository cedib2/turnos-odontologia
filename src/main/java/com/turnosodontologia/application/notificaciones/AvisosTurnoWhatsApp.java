package com.turnosodontologia.application.notificaciones;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * Programa y anula avisos de WhatsApp en el outbox según el ciclo de vida del turno.
 * La anticipación del recordatorio sigue {@code (TBD)}: sin {@code notificaciones.recordatorio.anticipacion-horas}
 * no se programa recordatorio (no se inventa un valor, RN-GL-03), solo la confirmación.
 */
public class AvisosTurnoWhatsApp implements AvisosTurno {

    private final NotificacionService notificaciones;
    private final NotificacionStore store;
    private final Clock reloj;
    private final Duration anticipacion;

    public AvisosTurnoWhatsApp(NotificacionService notificaciones, NotificacionStore store, Clock reloj,
                               Duration anticipacion) {
        this.notificaciones = notificaciones;
        this.store = store;
        this.reloj = reloj;
        this.anticipacion = anticipacion;
    }

    @Override
    public void creado(Long turnoId) {
        if (turnoId != null) {
            programar(turnoId);
        }
    }

    @Override
    public void reprogramado(Long turnoId) {
        if (turnoId != null) {
            notificaciones.anularPendientes(turnoId, "turno-reprogramado");
            programar(turnoId);
        }
    }

    @Override
    public void cancelado(Long turnoId) {
        if (turnoId != null) {
            notificaciones.anularPendientes(turnoId, "turno-cancelado");
        }
    }

    private void programar(long turnoId) {
        NotificacionStore.DatosTurno turno = store.datosTurno(turnoId)
                .orElseThrow(() -> new IllegalStateException("Turno inexistente para notificar: " + turnoId));
        if (!turno.activo()) {
            return;
        }
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
