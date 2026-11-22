package com.turnosodontologia.application.notificaciones;

import java.time.Instant;

public record Notificacion(long id, long turnoId, Long plantillaId, Proposito proposito,
                           String destinatario, EstadoNotificacion estado, int intentos,
                           String proveedorMessageId, Instant programadaPara, String ultimoError) {
}
