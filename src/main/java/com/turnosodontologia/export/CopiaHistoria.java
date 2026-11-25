package com.turnosodontologia.export;

import java.time.Instant;

public record CopiaHistoria(HistoriaClinicaExportable historia, Instant generadaEn,
                            boolean completa, String correlationId) {
}
