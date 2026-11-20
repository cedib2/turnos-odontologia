package com.turnosodontologia.interfaces.web;

import java.time.Instant;

public record CrearTurnoRequest(Long pacienteId, Long profesionalId, Long sillonId,
                                String codigoTratamiento, Instant inicio,
                                String estado, String origen) {
}
