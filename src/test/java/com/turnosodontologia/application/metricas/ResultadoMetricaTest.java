package com.turnosodontologia.application.metricas;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ResultadoMetricaTest {

    private static final Instant DESDE = Instant.parse("2026-12-01T03:00:00Z");
    private static final Instant HASTA = Instant.parse("2027-01-01T03:00:00Z");
    private static final MetodologiaMetrica METODOLOGIA =
            new MetodologiaMetrica("definición", "fórmula", "fuente", "(TBD)");

    @Test
    void metodologiaIncompletaSeRechaza() {
        assertThrows(IllegalArgumentException.class,
                () -> new MetodologiaMetrica("definición", " ", "fuente", "(TBD)"));
        assertThrows(IllegalArgumentException.class,
                () -> new MetodologiaMetrica("definición", "fórmula", null, "(TBD)"));
    }

    @Test
    void resultadoSinMetodologiaSeRechaza() {
        assertThrows(IllegalArgumentException.class, () -> new ResultadoMetrica("x",
                EstadoMetrica.CALCULADA, 1L, 2L, BigDecimal.TEN, DESDE, HASTA, null, null));
    }

    @Test
    void resultadoSinPeriodoSeRechaza() {
        assertThrows(IllegalArgumentException.class, () -> new ResultadoMetrica("x",
                EstadoMetrica.SIN_DATOS, 0L, 0L, null, null, HASTA, METODOLOGIA, null));
    }

    @Test
    void periodoInvalidoSeRechaza() {
        assertThrows(IllegalArgumentException.class, () -> new Periodo(HASTA, DESDE));
        assertThrows(IllegalArgumentException.class, () -> new Periodo(DESDE, DESDE));
        assertThrows(IllegalArgumentException.class, () -> new Periodo(null, HASTA));
    }

    @Test
    void conteoCalculaPorcentajeConDosDecimales() {
        ResultadoMetrica r = ResultadoMetrica.desdeConteo("x", 1, 3, new Periodo(DESDE, HASTA), METODOLOGIA);

        assertEquals(EstadoMetrica.CALCULADA, r.estado());
        assertEquals(new BigDecimal("33.33"), r.porcentaje());
    }

    @Test
    void denominadorCeroNoPublicaCifra() {
        ResultadoMetrica r = ResultadoMetrica.desdeConteo("x", 0, 0, new Periodo(DESDE, HASTA), METODOLOGIA);

        assertEquals(EstadoMetrica.SIN_DATOS, r.estado());
        assertNull(r.porcentaje());
    }

    @Test
    void noDisponibleExigeDependenciaYNoTieneCifra() {
        assertThrows(IllegalArgumentException.class, () -> ResultadoMetrica.noDisponible("x",
                new Periodo(DESDE, HASTA), METODOLOGIA, " "));
        ResultadoMetrica r = ResultadoMetrica.noDisponible("x", new Periodo(DESDE, HASTA), METODOLOGIA, "C-09");
        assertNull(r.porcentaje());
        assertNull(r.numerador());
    }

    @Test
    void elCatalogoTieneMetodologiaYMetaTbdParaCadaMetrica() {
        for (CatalogoMetricas.Definicion d : CatalogoMetricas.DEFINICIONES) {
            assertEquals(true, d.metodologia().metaInicial().startsWith("(TBD)"), d.codigo());
        }
    }
}
