package com.turnosodontologia.export;

import java.util.List;

public final class DefaultFinancialExportPort implements FinancialExportPort {

    @Override
    public List<MovimientoFinancieroExportable> movimientos() {
        return List.of();
    }

    @Override
    public boolean integrado() {
        return false;
    }
}
