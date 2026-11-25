package com.turnosodontologia.export;

import java.util.ArrayList;
import java.util.List;

public final class InMemoryOpsAlertPort implements OpsAlertPort {

    private final List<ExportAlert> alertas = new ArrayList<>();

    @Override
    public void alertar(ExportAlert alerta) {
        alertas.add(alerta);
    }

    public List<ExportAlert> alertas() {
        return List.copyOf(alertas);
    }

    public void limpiar() {
        alertas.clear();
    }
}
