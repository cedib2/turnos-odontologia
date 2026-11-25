package com.turnosodontologia.export;

import java.time.Instant;

public record ExportAlert(String correlationId, String tipo, String mensaje, Instant ocurridoEn) {
}
