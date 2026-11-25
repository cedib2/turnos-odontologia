package com.turnosodontologia.metricas;

import com.turnosodontologia.application.metricas.CalculadoraHorasHabiles;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CalculadoraHorasHabilesTest {

    // 2026-12-18 es viernes; 2026-12-21 lunes; 2026-12-25 viernes feriado.
    private static final LocalDate VIERNES = LocalDate.of(2026, 12, 18);

    private final CalculadoraHorasHabiles sinFeriados = new CalculadoraHorasHabiles(f -> false);

    @Test
    void elFinDeSemanaNoCuenta() {
        assertEquals(Duration.ofHours(24), sinFeriados.entre(local(VIERNES, 10), local(VIERNES.plusDays(3), 10)));
    }

    @Test
    void cuarentaYOchoHorasHabilesCruzandoUnFinDeSemana() {
        assertTrue(sinFeriados.conAnticipacion(local(VIERNES, 10), local(VIERNES.plusDays(4), 10), 48));
        assertFalse(sinFeriados.conAnticipacion(local(VIERNES, 10), local(VIERNES.plusDays(4), 9), 48));
    }

    @Test
    void losFeriadosDelCalendarioNoCuentan() {
        LocalDate lunes = VIERNES.plusDays(3);
        CalculadoraHorasHabiles conFeriado = new CalculadoraHorasHabiles(Set.of(lunes)::contains);

        assertEquals(Duration.ofHours(24), conFeriado.entre(local(VIERNES, 10), local(lunes.plusDays(1), 10)));
    }

    @Test
    void intervaloInvertidoEsCero() {
        assertEquals(Duration.ZERO, sinFeriados.entre(local(VIERNES, 10), local(VIERNES, 9)));
    }

    private static Instant local(LocalDate fecha, int hora) {
        return fecha.atTime(hora, 0).atZone(CalculadoraHorasHabiles.ZONA).toInstant();
    }
}
