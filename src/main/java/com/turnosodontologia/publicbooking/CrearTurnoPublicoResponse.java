package com.turnosodontologia.publicbooking;

import java.time.Instant;

public record CrearTurnoPublicoResponse(Long turnoId, Instant inicio, Instant fin,
                                        String estado, String origen, String tokenGestionar) {
}
