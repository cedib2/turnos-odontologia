package com.turnosodontologia.infrastructure.persistence;

import com.turnosodontologia.application.ConfiguracionSchedulingService;
import com.turnosodontologia.domain.agenda.Feriado;
import com.turnosodontologia.domain.agenda.HorarioAtencion;
import com.turnosodontologia.domain.agenda.Tratamiento;
import com.turnosodontologia.support.PostgresFlywayTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Import(ConfiguracionSchedulingService.class)
class SchedulingCatalogPersistenceTest extends PostgresFlywayTest {

    private static final ZoneId ZONA = ZoneId.of("America/Argentina/Buenos_Aires");

    @Autowired
    private ConfiguracionSchedulingService service;

    @Autowired
    private TratamientoRepository tratamientos;

    @Autowired
    private HorarioAtencionRepository horarios;

    @Autowired
    private FeriadoRepository feriados;

    @Test
    void consultaCatalogoExplicitoSinDependerDeSeeds() {
        tratamientos.saveAndFlush(new Tratamiento("LIMPIEZA", "Limpieza", 30));
        horarios.saveAndFlush(new HorarioAtencion(null, 2,
                LocalTime.of(9, 0), LocalTime.of(17, 0)));
        feriados.saveAndFlush(new Feriado(LocalDate.of(2026, 11, 26), "Fixture"));

        assertEquals(30, service.duracionMinutos("LIMPIEZA"));
        assertTrue(service.esFeriado(LocalDate.of(2026, 11, 26)));
        assertFalse(service.esFeriado(LocalDate.of(2026, 11, 27)));
        Instant dentro = LocalDate.of(2026, 11, 24).atTime(10, 0).atZone(ZONA).toInstant();
        assertTrue(service.estaDentroDeHorario(null, dentro, dentro.plusSeconds(1800)));
    }

    @Test
    void laZonaHorariaDelServicioEsLaAcordada() {
        assertEquals(ZONA, service.zonaHoraria());
    }
}
