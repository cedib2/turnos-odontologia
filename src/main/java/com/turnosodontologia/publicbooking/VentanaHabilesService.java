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

    public boolean dentroDeVentana(Instant inicio) {
        Instant ahora = reloj.instant();
        return !ahora.isBefore(vencimiento(inicio)) && ahora.isBefore(inicio);
    }

    private boolean esHabil(LocalDate dia) {
        DayOfWeek diaSemana = dia.getDayOfWeek();
        return diaSemana != DayOfWeek.SATURDAY
                && diaSemana != DayOfWeek.SUNDAY
                && !configuracion.esFeriado(dia);
    }
}
