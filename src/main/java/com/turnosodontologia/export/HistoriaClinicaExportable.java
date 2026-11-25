package com.turnosodontologia.export;

import com.turnosodontologia.domain.identidad.IdentidadReference;

import java.time.Instant;
import java.util.List;

public record HistoriaClinicaExportable(String fichaId, IdentidadReference pacienteRef, int version,
                                        String anamnesis, List<String> odontogramaFdi,
                                        List<EvolucionExportable> evoluciones,
                                        List<String> consentimientos, Instant generadoEn) {

    public HistoriaClinicaExportable {
        odontogramaFdi = List.copyOf(odontogramaFdi);
        evoluciones = List.copyOf(evoluciones);
        consentimientos = List.copyOf(consentimientos);
    }
}
