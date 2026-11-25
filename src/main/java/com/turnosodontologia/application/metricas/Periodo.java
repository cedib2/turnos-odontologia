package com.turnosodontologia.application.metricas;

import java.time.Instant;

/**
 * Período semiabierto {@code [desde, hasta)} obligatorio para toda métrica (C-01 §E, DD-06).
 */
public record Periodo(Instant desde, Instant hasta) {

    public Periodo {
        if (desde == null || hasta == null) {
            throw new IllegalArgumentException("El período es obligatorio: desde y hasta");
        }
        if (!hasta.isAfter(desde)) {
            throw new IllegalArgumentException("El período debe cumplir desde < hasta");
        }
    }
}
