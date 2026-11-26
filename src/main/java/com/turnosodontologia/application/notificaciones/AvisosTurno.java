package com.turnosodontologia.application.notificaciones;

/**
 * Avisos de WhatsApp ligados al ciclo de vida de un turno (C-10). Lo invocan la reserva pública (C-09) y las
 * operaciones de staff (C-08) dentro de su transacción, así que los avisos se confirman o revierten con el turno.
 * Un id nulo se ignora. Con {@code notificaciones.turnos.whatsapp=true} se usa {@link AvisosTurnoWhatsApp}; si no, {@link #NINGUNO}.
 */
public interface AvisosTurno {

    /** No genera avisos: comportamiento de test y de los perfiles sin WhatsApp habilitado. */
    AvisosTurno NINGUNO = new AvisosTurno() {
        @Override
        public void creado(Long turnoId) {
        }

        @Override
        public void reprogramado(Long turnoId) {
        }

        @Override
        public void cancelado(Long turnoId) {
        }
    };

    /** Confirmación inmediata y, si hay anticipación configurada, recordatorio antes del turno. */
    void creado(Long turnoId);

    /** Anula los avisos pendientes y programa los del nuevo horario. */
    void reprogramado(Long turnoId);

    /** Anula los avisos pendientes. */
    void cancelado(Long turnoId);
}
