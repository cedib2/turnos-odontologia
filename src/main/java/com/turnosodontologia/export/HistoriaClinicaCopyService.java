package com.turnosodontologia.export;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public final class HistoriaClinicaCopyService {

    public static final Duration PLAZO = Duration.ofHours(48);
    public static final Duration ALERTA_PREVENTIVA = Duration.ofHours(36);
    public static final ZoneId ZONA = ZoneId.of("America/Argentina/Buenos_Aires");

    private final OpsAlertPort alertas;

    public HistoriaClinicaCopyService(OpsAlertPort alertas) {
        this.alertas = alertas;
    }

    public CopiaHistoria copiar(HistoriaClinicaExportable historia, Instant solicitadaEn,
                                String correlationId) {
        validarEvoluciones(historia);
        return new CopiaHistoria(historia, Instant.now(), true, correlationId);
    }

    public boolean dentroDePlazo(Instant solicitadaEn, Instant ahora) {
        ZonedDateTime limite = ZonedDateTime.ofInstant(solicitadaEn, ZONA).plus(PLAZO);
        return !ZonedDateTime.ofInstant(ahora, ZONA).isAfter(limite);
    }

    public void evaluarSla(Instant solicitadaEn, Instant ahora, String correlationId) {
        if (dentroDePlazo(solicitadaEn, ahora)
                && !ahora.isBefore(solicitadaEn.plus(ALERTA_PREVENTIVA))) {
            alertas.alertar(new ExportAlert(correlationId, "COPIA_HISTORIA_SLA_36H",
                    "Alerta preventiva: 36 horas corridas sin resolver en " + ZONA.getId(), ahora));
        }
    }

    private void validarEvoluciones(HistoriaClinicaExportable historia) {
        for (EvolucionExportable evolucion : historia.evoluciones()) {
            if (evolucion.firma() == null || evolucion.firma().isBlank()
                    || evolucion.matricula() == null || evolucion.matricula().isBlank()) {
                throw new IllegalArgumentException(
                        "Evolución sin firma o matrícula: no se puede emitir copia completa");
            }
        }
    }
}
