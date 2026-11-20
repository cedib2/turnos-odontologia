package com.turnosodontologia.application;

import java.time.Instant;

public record EntradaAgenda(String tipo, Long id, Instant inicio, Instant fin,
                            Long profesionalId, Long sillonId, String detalle) {
}
