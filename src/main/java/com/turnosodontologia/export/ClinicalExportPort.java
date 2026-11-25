package com.turnosodontologia.export;

import java.util.List;

public interface ClinicalExportPort {

    List<HistoriaClinicaExportable> historias();

    default boolean integrado() {
        return true;
    }
}
