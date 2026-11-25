package com.turnosodontologia.export;

import java.math.BigDecimal;
import java.util.List;

public record MovimientoFinancieroExportable(String id, String turnoRef, BigDecimal monto,
                                             String moneda, String estado, String correlationId) {
}
