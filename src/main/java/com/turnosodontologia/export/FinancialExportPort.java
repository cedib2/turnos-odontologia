package com.turnosodontologia.export;

import java.util.List;

public interface FinancialExportPort {

    List<MovimientoFinancieroExportable> movimientos();

    default boolean integrado() {
        return true;
    }
}
