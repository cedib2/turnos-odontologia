package com.turnosodontologia.depositpayments;

/** Aviso al paciente detrás de un puerto; sin envío real en este change (MP-9). */
public interface NotificacionPagoPort {

    void solicitar(Long seniaId, String motivo);
}
