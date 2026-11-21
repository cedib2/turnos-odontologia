package com.turnosodontologia.application;

import com.turnosodontologia.support.PostgresFlywayTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Import({CatalogoSchedulingService.class, ConfiguracionSchedulingService.class,
        DisponibilidadService.class})
class CatalogoLecturaC08Test extends PostgresFlywayTest {

    private static final ZoneId ZONA = ZoneId.of("America/Argentina/Buenos_Aires");

    @Autowired
    private CatalogoSchedulingService catalogo;

    @Autowired
    private ConfiguracionSchedulingService configuracion;

    @Autowired
    private DisponibilidadService disponibilidad;

    @Test
    void laLecturaDeC08ConsultaElCatalogoCargadoPorElCrud() {
        LocalDate fecha = LocalDate.of(2026, 12, 3);
        catalogo.crearTratamiento("LIMPIEZA", "Limpieza", 30);
        catalogo.crearHorario(null, fecha.getDayOfWeek().getValue(),
                LocalTime.of(9, 0), LocalTime.of(11, 0));

        assertEquals(30, configuracion.duracionMinutos("LIMPIEZA"));
        Instant inicio = fecha.atTime(9, 0).atZone(ZONA).toInstant();
        assertTrue(configuracion.estaDentroDeHorario(null, inicio, inicio.plusSeconds(1800)));
        assertEquals(4, disponibilidad.huecosLibres(null, 9L, fecha, 30).size());

        catalogo.crearFeriado(fecha, "Fixture", true);

        assertTrue(configuracion.esFeriado(fecha));
        assertTrue(disponibilidad.huecosLibres(null, 9L, fecha, 30).isEmpty());
    }

    @Test
    void laLecturaSigueSinFiltrarPorActivoNiInterpretarAplicaATodos() {
        LocalDate fecha = LocalDate.of(2026, 12, 4);
        var feriado = catalogo.crearFeriado(fecha, "Fixture", false);
        catalogo.eliminarFeriado(feriado.getId());
        var horario = catalogo.crearHorario(null, fecha.getDayOfWeek().getValue(),
                LocalTime.of(8, 0), LocalTime.of(10, 0));
        catalogo.eliminarHorario(horario.getId());
        var tratamiento = catalogo.crearTratamiento("INACTIVO", "Inactivo", 45);
        catalogo.eliminarTratamiento(tratamiento.getId());

        assertTrue(configuracion.esFeriado(fecha));
        Instant inicio = fecha.atTime(8, 0).atZone(ZONA).toInstant();
        assertTrue(configuracion.estaDentroDeHorario(null, inicio, inicio.plusSeconds(1800)));
        assertEquals(45, configuracion.duracionMinutos("INACTIVO"));
    }
}
