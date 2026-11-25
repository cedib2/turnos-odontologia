package com.turnosodontologia.export;

import com.turnosodontologia.domain.agenda.AgendaReference;
import com.turnosodontologia.domain.auditoria.AuditoriaReference;
import com.turnosodontologia.domain.identidad.IdentidadReference;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExportSnapshotAssemblerTest {

    private static final ZoneId ZONA = ZoneId.of("America/Argentina/Buenos_Aires");

    @Test
    void snapshotConCincoDominiosYMetadatos() {
        ClinicalExportPort clinico = new ClinicalExportPort() {
            @Override
            public List<HistoriaClinicaExportable> historias() {
                return List.of(new HistoriaClinicaExportable("ficha-1", new IdentidadReference("pac-1"), 2,
                        "anamnesis minima", List.of("11=temporal"), List.of(
                                new EvolucionExportable("evolucion", "firma-1", "mat-1")),
                        List.of("consentimiento-1"), Instant.parse("2026-11-24T10:00:00Z")));
            }

            @Override
            public boolean integrado() {
                return true;
            }
        };
        FinancialExportPort financiero = new FinancialExportPort() {
            @Override
            public List<MovimientoFinancieroExportable> movimientos() {
                return List.of(new MovimientoFinancieroExportable("mov-1", "turno-1",
                        new BigDecimal("100.00"), "ARS", "APROBADO", "corr-1"));
            }

            @Override
            public boolean integrado() {
                return true;
            }
        };

        ExportSnapshot snapshot = ensamblador(clinico, financiero).ensamblar("titular");

        assertAll(
                () -> assertEquals(List.of("identidad", "agenda", "clinico", "financial", "auditoria"),
                        snapshot.dominios()),
                () -> assertNotNull(snapshot.generadoEn()),
                () -> assertEquals(ZONA.getId(), snapshot.zona()),
                () -> assertEquals("titular", snapshot.alcance()),
                () -> assertEquals(5, snapshot.conteos().size()),
                () -> assertEquals(1, snapshot.conteos().get("clinico")),
                () -> assertEquals(ExportResultado.COMPLETA, snapshot.resultado()),
                () -> assertFalse(snapshot.partialExport())
        );
    }

    @Test
    void adaptadoresPorDefectoSinExcepcion() {
        ExportSnapshotAssembler assembler = ensamblador(
                new DefaultClinicalExportPort(), new DefaultFinancialExportPort());

        ExportSnapshot snapshot = assertDoesNotThrow(() -> assembler.ensamblar("titular"));

        assertAll(
                () -> assertTrue(new DefaultClinicalExportPort().historias().isEmpty()),
                () -> assertTrue(new DefaultFinancialExportPort().movimientos().isEmpty()),
                () -> assertEquals(0, snapshot.conteos().get("clinico")),
                () -> assertEquals(0, snapshot.conteos().get("financial")),
                () -> assertEquals(ExportResultado.PARCIAL, snapshot.resultado())
        );
    }

    @Test
    void exportacionPartialMarcada() {
        ExportSnapshot snapshot = ensamblador(
                new DefaultClinicalExportPort(), new DefaultFinancialExportPort()).ensamblar("titular");

        assertAll(
                () -> assertTrue(snapshot.partialExport()),
                () -> assertEquals(List.of("clinical", "financial"), snapshot.unintegratedModules()),
                () -> assertEquals(ExportResultado.PARCIAL, snapshot.resultado()),
                () -> assertFalse(snapshot.resultado() == ExportResultado.COMPLETA)
        );
    }

    @Test
    void artefactoSinSecretosNiFirmas() {
        ExportSnapshot snapshot = ensamblador(
                new DefaultClinicalExportPort(), new DefaultFinancialExportPort()).ensamblar("titular");
        ExportFormatService formatos = new ExportFormatService();

        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> formatos.json(snapshot,
                        Map.of("access_token", "valor-prohibido"))),
                () -> assertThrows(IllegalArgumentException.class, () -> formatos.json(snapshot,
                        Map.of("webhook_signature", "firma-prohibida"))),
                () -> assertFalse(new String(formatos.json(snapshot, Map.of())).contains("firma-prohibida"))
        );
    }

    private ExportSnapshotAssembler ensamblador(ClinicalExportPort clinico, FinancialExportPort financiero) {
        return new ExportSnapshotAssembler(
                List.of(new IdentidadReference("identidad-1")),
                List.of(new AgendaReference("agenda-1")),
                clinico,
                financiero,
                List.of(new AuditoriaReference("auditoria-1")),
                ZONA);
    }
}
