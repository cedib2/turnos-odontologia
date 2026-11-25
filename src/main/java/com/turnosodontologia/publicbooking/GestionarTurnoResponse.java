package com.turnosodontologia.publicbooking;

import java.time.Instant;

public record GestionarTurnoResponse(Long turnoId, String estado, boolean activo,
                                     Instant inicio, Instant fin) {
}
