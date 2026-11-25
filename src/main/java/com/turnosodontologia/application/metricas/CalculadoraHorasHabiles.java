package com.turnosodontologia.application.metricas;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.function.Predicate;

/**
 * Horas hábiles entre dos instantes (C-01 C2): zona America/Argentina/Buenos_Aires, días hábiles completos
 * (lunes a viernes) y feriados del calendario configurable (C-07). Las horas de sábados, domingos y feriados
 * no cuentan.
 */
public final class CalculadoraHorasHabiles {

    public static final ZoneId ZONA = ZoneId.of("America/Argentina/Buenos_Aires");

    private final Predicate<LocalDate> esFeriado;

    public CalculadoraHorasHabiles(Predicate<LocalDate> esFeriado) {
        this.esFeriado = esFeriado;
    }

    public boolean esHabil(LocalDate fecha) {
        DayOfWeek dia = fecha.getDayOfWeek();
        return dia != DayOfWeek.SATURDAY && dia != DayOfWeek.SUNDAY && !esFeriado.test(fecha);
    }

    public Duration entre(Instant desde, Instant hasta) {
        if (desde == null || hasta == null || !hasta.isAfter(desde)) {
            return Duration.ZERO;
        }
        Duration total = Duration.ZERO;
        ZonedDateTime cursor = desde.atZone(ZONA);
        ZonedDateTime fin = hasta.atZone(ZONA);
        while (cursor.isBefore(fin)) {
            ZonedDateTime finDelDia = cursor.toLocalDate().plusDays(1).atStartOfDay(ZONA);
            ZonedDateTime corte = finDelDia.isBefore(fin) ? finDelDia : fin;
            if (esHabil(cursor.toLocalDate())) {
                total = total.plus(Duration.between(cursor, corte));
            }
            cursor = corte;
        }
        return total;
    }

    public boolean conAnticipacion(Instant evento, Instant inicioTurno, long horasHabiles) {
        return entre(evento, inicioTurno).compareTo(Duration.ofHours(horasHabiles)) >= 0;
    }
}
