package com.turnosodontologia.export;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record ExportSnapshot(Instant generadoEn, String zona, String alcance,
                             Map<String, Integer> conteos, List<String> dominios,
                             boolean partialExport, List<String> unintegratedModules,
                             ExportResultado resultado) {

    public ExportSnapshot {
        conteos = Map.copyOf(conteos);
        dominios = List.copyOf(dominios);
        unintegratedModules = List.copyOf(unintegratedModules);
    }
}
