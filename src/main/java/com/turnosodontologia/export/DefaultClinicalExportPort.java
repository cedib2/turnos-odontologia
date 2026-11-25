package com.turnosodontologia.export;

import java.util.List;

public final class DefaultClinicalExportPort implements ClinicalExportPort {

    @Override
    public List<HistoriaClinicaExportable> historias() {
        return List.of();
    }

    @Override
    public boolean integrado() {
        return false;
    }
}
