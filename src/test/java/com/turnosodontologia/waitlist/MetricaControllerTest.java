package com.turnosodontologia.waitlist;

import com.turnosodontologia.domain.agenda.Turno;
import com.turnosodontologia.domain.waitlist.MetricaOcupacion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** W-12: la superficie devuelve cifra, metodología y período; sin meta inventada. */
class MetricaControllerTest {

    private MetricaOcupacionService metricas;
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        metricas = mock(MetricaOcupacionService.class);
        mvc = MockMvcBuilders.standaloneSetup(new MetricaController(metricas)).build();
    }

    @Test
    void publicarDevuelveCifraMetodologiaYPeriodo() throws Exception {
        when(metricas.publicar(3L, "2026-12")).thenReturn(metrica());

        mvc.perform(post("/metricas/publicar/3/2026-12"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ocupacion").value("70.0000"))
                .andExpect(jsonPath("$.ausentismo").value("15.0000"))
                .andExpect(jsonPath("$.periodo").value("2026-12"))
                .andExpect(jsonPath("$.metodologia").value(
                        org.hamcrest.Matchers.containsString("fuente=")));
    }

    @Test
    void sinDenominadorResponde422SinCifra() throws Exception {
        when(metricas.publicar(3L, "2026-12")).thenReturn(null);
        mvc.perform(post("/metricas/publicar/3/2026-12")).andExpect(status().isUnprocessableEntity());
    }

    @Test
    void consultarDevuelve404CuandoNoHayMetrica() throws Exception {
        when(metricas.consultar(3L, "2026-12")).thenReturn(null);
        mvc.perform(get("/metricas/3/2026-12")).andExpect(status().isNotFound());
    }

    @Test
    void registrarInasistenciaResponde201YErroresSeTraducen() throws Exception {
        Turno turno = mock(Turno.class);
        when(turno.getId()).thenReturn(9L);
        when(metricas.registrarInasistencia(9L)).thenReturn(turno);
        when(metricas.publicar(3L, "2026-13"))
                .thenThrow(new IllegalArgumentException("período inválido"));
        when(metricas.registrarInasistencia(10L))
                .thenThrow(new NoSuchElementException("turno inexistente"));

        mvc.perform(post("/metricas/inasistencia/9")).andExpect(status().isCreated());
        mvc.perform(post("/metricas/inasistencia/10")).andExpect(status().isNotFound());
        mvc.perform(post("/metricas/publicar/3/2026-13")).andExpect(status().isBadRequest());
    }

    private MetricaOcupacion metrica() {
        return new MetricaOcupacion(3L, "2026-12", new BigDecimal("70.0000"),
                new BigDecimal("15.0000"), "ocupacion=...; fuente=turnos,bloqueos",
                Instant.parse("2026-12-10T12:00:00Z"));
    }
}
