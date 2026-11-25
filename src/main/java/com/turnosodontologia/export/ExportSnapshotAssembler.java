package com.turnosodontologia.export;

import com.turnosodontologia.domain.agenda.AgendaReference;
import com.turnosodontologia.domain.auditoria.AuditoriaReference;
import com.turnosodontologia.domain.identidad.IdentidadReference;

import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ExportSnapshotAssembler {

    private static final List<String> DOMINIOS = List.of(
            "identidad", "agenda", "clinico", "financial", "auditoria");

    private final Collection<IdentidadReference> identidad;
    private final Collection<AgendaReference> agenda;
    private final ClinicalExportPort clinico;
    private final FinancialExportPort financiero;
    private final Collection<AuditoriaReference> auditoria;
    private final ZoneId zona;

    public ExportSnapshotAssembler(Collection<IdentidadReference> identidad,
                                   Collection<AgendaReference> agenda,
                                   ClinicalExportPort clinico,
                                   FinancialExportPort financiero,
                                   Collection<AuditoriaReference> auditoria,
                                   ZoneId zona) {
        this.identidad = identidad;
        this.agenda = agenda;
        this.clinico = clinico;
        this.financiero = financiero;
        this.auditoria = auditoria;
        this.zona = zona;
    }

    public ExportSnapshot ensamblar(String alcance) {
        Map<String, Integer> conteos = new LinkedHashMap<>();
        conteos.put("identidad", identidad.size());
        conteos.put("agenda", agenda.size());
        conteos.put("clinico", clinico.historias().size());
        conteos.put("financial", financiero.movimientos().size());
        conteos.put("auditoria", auditoria.size());

        List<String> noIntegrados = new ArrayList<>();
        if (!clinico.integrado()) {
            noIntegrados.add("clinical");
        }
        if (!financiero.integrado()) {
            noIntegrados.add("financial");
        }
        ExportResultado resultado = noIntegrados.isEmpty()
                ? ExportResultado.COMPLETA : ExportResultado.PARCIAL;
        return new ExportSnapshot(Instant.now(), zona.getId(), alcance, conteos, DOMINIOS,
                !noIntegrados.isEmpty(), noIntegrados, resultado);
    }
}
