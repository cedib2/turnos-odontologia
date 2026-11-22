package com.turnosodontologia.publicbooking;

import com.turnosodontologia.application.ConfiguracionSchedulingService;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class VentanaHabilesServiceTest {

    private static final ZoneId ZONA = ZoneId.of("America/Argentina/Buenos_Aires");
    private static final Instant INICIO_JUEVES = Instant.parse("2026-12-03T13:00:00Z");

    private final ConfiguracionSchedulingService configuracion = mock(ConfiguracionSchedulingService.class);

    @Test
    void vencimientoEsElSegundoDiaHabilAnteriorALas235959() {
        when(configuracion.zonaHoraria()).thenReturn(ZONA);
        when(configuracion.esFeriado(any())).thenReturn(false);
        VentanaHabilesService servicio = new VentanaHabilesService(configuracion, Clock.systemUTC());

        assertEquals(Instant.parse("2026-12-02T02:59:59Z"), servicio.vencimiento(INICIO_JUEVES));
    }

    @Test
    void limiteInclusiveDentroYUnSegundoAntesFuera() {
        when(configuracion.zonaHoraria()).thenReturn(ZONA);
        when(configuracion.esFeriado(any())).thenReturn(false);
        Instant vencimiento = Instant.parse("2026-12-02T02:59:59Z");

        VentanaHabilesService dentro = new VentanaHabilesService(configuracion,
                Clock.fixed(vencimiento, ZoneOffset.UTC));
        VentanaHabilesService antes = new VentanaHabilesService(configuracion,
                Clock.fixed(vencimiento.minusSeconds(1), ZoneOffset.UTC));

        assertTrue(dentro.dentroDeVentana(INICIO_JUEVES));
        assertFalse(antes.dentroDeVentana(INICIO_JUEVES));
    }

    @Test
    void finDeSemanaNoCuentaComoDiaHabil() {
        when(configuracion.zonaHoraria()).thenReturn(ZONA);
        when(configuracion.esFeriado(any())).thenReturn(false);
        VentanaHabilesService servicio = new VentanaHabilesService(configuracion, Clock.systemUTC());

        Instant inicioLunes = Instant.parse("2026-12-07T13:00:00Z");
        assertEquals(Instant.parse("2026-12-04T02:59:59Z"), servicio.vencimiento(inicioLunes));
    }

    @Test
    void feriadoNoCuentaComoDiaHabil() {
        when(configuracion.zonaHoraria()).thenReturn(ZONA);
        when(configuracion.esFeriado(LocalDate.of(2026, 12, 2))).thenReturn(true);
        VentanaHabilesService servicio = new VentanaHabilesService(configuracion, Clock.systemUTC());

        assertEquals(Instant.parse("2026-12-01T02:59:59Z"), servicio.vencimiento(INICIO_JUEVES));
    }
}
