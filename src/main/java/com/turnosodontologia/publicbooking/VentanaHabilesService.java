package com.turnosodontologia.publicbooking;

import com.turnosodontologia.application.ConfiguracionSchedulingService;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

public final class VentanaHabilesService {

    private static final int DIAS_HABILES = 2;

    private final ConfiguracionSchedulingService configuracion;
    private final Clock reloj;

    public VentanaHabilesService(ConfiguracionSchedulingService configuracion, Clock reloj) {
        this.configuracion = configuracion;
        this.reloj = reloj;
    }

    public Instant vencimiento(Instant inicio) {
        ZoneId zona = configuracion.zonaHoraria();
        LocalDate dia = inicio.atZone(zona).toLocalDate();
        int contados = 0;
        while (contados < DIAS_HABILES) {
            dia = dia.minusDays(1);
            if (esHabil(dia)) {
                contados++;
            }
        }
        return dia.atTime(LocalTime.of(23, 59, 59)).atZone(zona).toInstant();
    }

    /**
     * El paciente puede cancelar o reprogramar por link hasta el vencimiento inclusive (23:59:59 del segundo
     * día hábil anterior al turno). Después, el cambio lo hace recepción (RN-AG-05; decisión del 2026-11-25,
     * ver docs/decisiones/C-09-sentido-ventana-48hs.md).
     */
    public boolean dentroDeVentana(Instant inicio) {
        return !reloj.instant().isAfter(vencimiento(inicio));
    }

    /** Si el turno todavía no empezó: la confirmación de asistencia no depende de la ventana. */
    public boolean antesDelInicio(Instant inicio) {
        return reloj.instant().isBefore(inicio);
    }

    private boolean esHabil(LocalDate dia) {
        DayOfWeek diaSemana = dia.getDayOfWeek();
        return diaSemana != DayOfWeek.SATURDAY
                && diaSemana != DayOfWeek.SUNDAY
                && !configuracion.esFeriado(dia);
    }
}
