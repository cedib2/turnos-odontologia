package com.turnosodontologia.application;

import com.turnosodontologia.domain.agenda.HorarioAtencion;
import com.turnosodontologia.domain.agenda.Ocupacion;
import com.turnosodontologia.infrastructure.persistence.OcupacionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DisponibilidadServiceTest {

    private static final LocalDate FECHA = LocalDate.of(2026, 12, 22);
    private static final Instant TRECE = Instant.parse("2026-12-22T13:00:00Z");

    @Mock
    private ConfiguracionSchedulingService configuracion;

    @Mock
    private OcupacionRepository ocupaciones;

    private DisponibilidadService service;

    @BeforeEach
    void setUp() {
        service = new DisponibilidadService(configuracion, ocupaciones);
    }

    @Test
    void feriadoNoOfreceHuecos() {
        when(configuracion.esFeriado(FECHA)).thenReturn(true);

        assertEquals(List.of(), service.huecosLibres(1L, 2L, FECHA, 30));

        verifyNoInteractions(ocupaciones);
    }

    @Test
    void excluyeFueraDeHorarioYBloqueos() {
        when(configuracion.esFeriado(FECHA)).thenReturn(false);
        when(configuracion.zonaHoraria()).thenReturn(ZoneOffset.UTC);
        when(configuracion.horariosDelDia(1, FECHA)).thenReturn(List.of(
                new HorarioAtencion(null, FECHA.getDayOfWeek().getValue(),
                        LocalTime.of(13, 0), LocalTime.of(14, 30))));
        Ocupacion bloqueo = Ocupacion.paraBloqueo(9L, null, 2L,
                TRECE, TRECE.plusSeconds(1800));
        when(ocupaciones.findActivosQueSolapan(any(), any(), any(), any()))
                .thenAnswer(invocation -> {
                    Instant inicio = invocation.getArgument(2);
                    return inicio.equals(TRECE) ? List.of(bloqueo) : List.of();
                });

        List<IntervaloDisponible> libres = service.huecosLibres(1L, 2L, FECHA, 30);

        assertFalse(libres.stream().anyMatch(hueco -> hueco.inicio().equals(TRECE)));
        assertTrue(libres.stream().anyMatch(hueco ->
                hueco.inicio().equals(TRECE.plusSeconds(1800))));
        assertTrue(libres.stream().anyMatch(hueco ->
                hueco.inicio().equals(TRECE.plusSeconds(3600))));
        assertFalse(libres.stream().anyMatch(hueco ->
                hueco.inicio().equals(TRECE.plusSeconds(5400))));
    }
}
