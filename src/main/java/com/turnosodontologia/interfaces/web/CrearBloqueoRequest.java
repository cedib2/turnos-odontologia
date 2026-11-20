package com.turnosodontologia.interfaces.web;

import java.time.Instant;

public record CrearBloqueoRequest(Long profesionalId, Long sillonId,
                                  Instant desde, Instant hasta, String motivo) {
}
