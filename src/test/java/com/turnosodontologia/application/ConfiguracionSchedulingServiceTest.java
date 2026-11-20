package com.turnosodontologia.application;

import com.turnosodontologia.domain.agenda.Feriado;
import com.turnosodontologia.domain.agenda.HorarioAtencion;
import com.turnosodontologia.domain.agenda.Tratamiento;
import com.turnosodontologia.infrastructure.persistence.FeriadoRepository;
import com.turnosodontologia.infrastructure.persistence.HorarioAtencionRepository;
import com.turnosodontologia.infrastructure.persistence.TratamientoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConfiguracionSchedulingServiceTest {

    private static final ZoneId ZONA = ZoneId.of("America/Argentina/Buenos_Aires");

    @Mock
    private TratamientoRepository tratamientos;

    @Mock
    private HorarioAtencionRepository horarios;

    @Mock
    private FeriadoRepository feriados;

    @InjectMocks
    private ConfiguracionSchedulingService service;

    @Test
    void devuelveLaDuracionConfiguradaDelTratamiento() {
        when(tratamientos.findByCodigo("LIMPIEZA"))
                .thenReturn(Optional.of(new Tratamiento("LIMPIEZA", "Limpieza", 30)));

        assertEquals(30, service.duracionMinutos("LIMPIEZA"));
    }

    @Test
    void noInventaDuracionParaUnTratamientoNoConfigurado() {
        when(tratamientos.findByCodigo("SIN_CONFIGURACION")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> service.duracionMinutos("SIN_CONFIGURACION"));
    }

    @Test
    void detectaFeriadoConfiguradoYNoInventaUno() {
        when(feriados.findByFecha(LocalDate.of(2026, 11, 26)))
                .thenReturn(Optional.of(new Feriado(LocalDate.of(2026, 11, 26), "Fixture")));
        when(feriados.findByFecha(LocalDate.of(2026, 11, 27))).thenReturn(Optional.empty());

        assertTrue(service.esFeriado(LocalDate.of(2026, 11, 26)));
        assertFalse(service.esFeriado(LocalDate.of(2026, 11, 27)));
    }

    @Test
    void evaluaHorariosEnLaZonaHorariaAcordada() {
        when(horarios.findByDiaSemanaAndProfesionalIdIsNull(2))
                .thenReturn(List.of(new HorarioAtencion(null, 2,
                        LocalTime.of(9, 0), LocalTime.of(17, 0))));
        Instant dentro = LocalDate.of(2026, 11, 24).atTime(10, 0).atZone(ZONA).toInstant();
        Instant fuera = LocalDate.of(2026, 11, 24).atTime(20, 0).atZone(ZONA).toInstant();

        assertTrue(service.estaDentroDeHorario(null, dentro, dentro.plusSeconds(1800)));
        assertFalse(service.estaDentroDeHorario(null, fuera, fuera.plusSeconds(1800)));
    }
}
