package com.turnosodontologia.waitlist;

import java.time.Instant;

public record AltaWaitlist(Long candidatoId, Long profesionalId, Long sillonId,
                           Instant inicioDeseado, Instant finDeseado, boolean depositoRequerido) {
}
