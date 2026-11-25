package com.turnosodontologia.application.cumplimiento;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

/**
 * Parámetros operativos de C-17 que siguen (TBD, IN-05/IN-06). Sin valor configurado no se calcula plazo
 * ni retención: se registra null y el dossier (C-18) lo informa como pendiente; no hay default inventado.
 */
@Component
public class ParametrosCumplimiento {

    private static final ZoneId ZONA = ZoneId.of("America/Argentina/Buenos_Aires");

    private final Integer plazoCopiaHoras;
    private final Integer retencionAnios;

    public ParametrosCumplimiento(
            @Value("${cumplimiento.exportacion.plazo-horas:#{null}}") Integer plazoCopiaHoras,
            @Value("${cumplimiento.respaldo.retencion-anios:${EXPORT_RETENTION_YEARS:#{null}}}") Integer retencionAnios) {
        if (plazoCopiaHoras != null && plazoCopiaHoras <= 0) {
            throw new IllegalArgumentException("cumplimiento.exportacion.plazo-horas debe ser positivo");
        }
        if (retencionAnios != null && retencionAnios <= 0) {
            throw new IllegalArgumentException("cumplimiento.respaldo.retencion-anios debe ser positivo");
        }
        this.plazoCopiaHoras = plazoCopiaHoras;
        this.retencionAnios = retencionAnios;
    }

    public Optional<Integer> plazoCopiaHoras() {
        return Optional.ofNullable(plazoCopiaHoras);
    }

    public Optional<Integer> retencionAnios() {
        return Optional.ofNullable(retencionAnios);
    }

    public Instant plazoDesde(Instant solicitud) {
        return plazoCopiaHoras == null ? null : solicitud.plus(Duration.ofHours(plazoCopiaHoras));
    }

    public Instant retencionDesde(Instant completado) {
        return retencionAnios == null
                ? null
                : completado.atZone(ZONA).plusYears(retencionAnios).toInstant();
    }
}
