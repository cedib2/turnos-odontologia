package com.turnosodontologia.publicbooking;

import java.time.Instant;

/**
 * Resultado de la reserva pública. {@code tratamiento}, {@code profesional}, {@code sillon} y {@code consultorio}
 * son nombres para mostrar en la confirmación del paciente (aditivos: pueden faltar).
 */
public record CrearTurnoPublicoResponse(Long turnoId, Instant inicio, Instant fin,
                                        String estado, String origen, String tokenGestionar,
                                        String tratamiento, String profesional, String sillon, String consultorio) {

    public CrearTurnoPublicoResponse(Long turnoId, Instant inicio, Instant fin,
                                     String estado, String origen, String tokenGestionar) {
        this(turnoId, inicio, fin, estado, origen, tokenGestionar, null, null, null, null);
    }
}
