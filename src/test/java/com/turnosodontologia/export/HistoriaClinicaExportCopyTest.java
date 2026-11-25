package com.turnosodontologia.export;

import com.turnosodontologia.domain.identidad.IdentidadReference;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HistoriaClinicaExportCopyTest {

    private static final Instant SOLICITUD = Instant.parse("2026-11-27T10:00:00Z");

    @Test
    void copiaFielDeLaHistoria() {
        InMemoryOpsAlertPort alertas = new InMemoryOpsAlertPort();
        HistoriaClinicaExportable ficha = ficha(
                new EvolucionExportable("evolucion firmada", "firma-1", "mat-1"));
        String original = ficha.toString();

        CopiaHistoria copia = new HistoriaClinicaCopyService(alertas)
                .copiar(ficha, SOLICITUD, "corr-copia");

        assertAll(
                () -> assertEquals(3, copia.historia().version()),
                () -> assertEquals("anamnesis versionada", copia.historia().anamnesis()),
                () -> assertEquals(List.of("11=temporal;51=resto"), copia.historia().odontogramaFdi()),
                () -> assertEquals("firma-1", copia.historia().evoluciones().get(0).firma()),
                () -> assertEquals("mat-1", copia.historia().evoluciones().get(0).matricula()),
                () -> assertEquals(List.of("consentimiento-1"), copia.historia().consentimientos()),
                () -> assertTrue(copia.completa()),
                () -> assertEquals("corr-copia", copia.correlationId()),
                () -> assertEquals(original, ficha.toString())
        );
    }

    @Test
    void copiaDentroDe48HorasCorridas() {
        HistoriaClinicaCopyService servicio = new HistoriaClinicaCopyService(new InMemoryOpsAlertPort());

        assertAll(
                () -> assertTrue(servicio.dentroDePlazo(SOLICITUD,
                        SOLICITUD.plus(Duration.ofHours(48)))),
                () -> assertFalse(servicio.dentroDePlazo(SOLICITUD,
                        SOLICITUD.plus(Duration.ofHours(48)).plusSeconds(1))),
                () -> assertTrue(servicio.dentroDePlazo(SOLICITUD,
                        SOLICITUD.plus(Duration.ofHours(36))))
        );
    }

    @Test
    void alertaPreventivaALas36Horas() {
        InMemoryOpsAlertPort alertas = new InMemoryOpsAlertPort();
        HistoriaClinicaCopyService servicio = new HistoriaClinicaCopyService(alertas);

        servicio.evaluarSla(SOLICITUD, SOLICITUD.plus(Duration.ofHours(36)), "corr-sla");

        assertAll(
                () -> assertEquals(1, alertas.alertas().size()),
                () -> assertEquals("corr-sla", alertas.alertas().get(0).correlationId()),
                () -> assertTrue(alertas.alertas().get(0).tipo().contains("36")),
                () -> assertTrue(alertas.alertas().get(0).ocurridoEn().isBefore(
                        SOLICITUD.plus(Duration.ofHours(48))))
        );

        alertas.limpiar();
        servicio.evaluarSla(SOLICITUD, SOLICITUD.plus(Duration.ofHours(35)), "corr-sla");
        assertTrue(alertas.alertas().isEmpty());
    }

    @Test
    void evolucionSinFirmaOMatriculaSeRechaza() {
        InMemoryOpsAlertPort alertas = new InMemoryOpsAlertPort();
        HistoriaClinicaCopyService servicio = new HistoriaClinicaCopyService(alertas);

        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> servicio.copiar(
                        ficha(new EvolucionExportable("evolucion", "", "mat-1")),
                        SOLICITUD, "corr-sin-firma")),
                () -> assertThrows(IllegalArgumentException.class, () -> servicio.copiar(
                        ficha(new EvolucionExportable("evolucion", "firma-1", null)),
                        SOLICITUD, "corr-sin-matricula")),
                () -> assertTrue(alertas.alertas().isEmpty())
        );
    }

    private HistoriaClinicaExportable ficha(EvolucionExportable evolucion) {
        return new HistoriaClinicaExportable("ficha-1", new IdentidadReference("paciente-1"), 3,
                "anamnesis versionada", List.of("11=temporal;51=resto"), List.of(evolucion),
                List.of("consentimiento-1"), SOLICITUD);
    }
}
