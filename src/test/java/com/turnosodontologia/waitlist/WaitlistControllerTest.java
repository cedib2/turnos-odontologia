package com.turnosodontologia.waitlist;

import com.turnosodontologia.domain.waitlist.EntradaWaitlist;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** W-12: rutas indicativas sin token de C-06 ni credenciales de staff. */
class WaitlistControllerTest {

    private ListaEsperaService listaEspera;
    private RellenoWaitlistService relleno;
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        listaEspera = mock(ListaEsperaService.class);
        relleno = mock(RellenoWaitlistService.class);
        mvc = MockMvcBuilders.standaloneSetup(new WaitlistController(listaEspera, relleno)).build();
    }

    @Test
    void altaResponde201SinCredenciales() throws Exception {
        EntradaWaitlist entrada = mock(EntradaWaitlist.class);
        when(entrada.getId()).thenReturn(1L);
        when(entrada.getCandidatoId()).thenReturn(10L);
        when(entrada.getEstado()).thenReturn(EntradaWaitlist.ACTIVA);
        when(listaEspera.registrar(any())).thenReturn(entrada);

        mvc.perform(post("/lista-espera")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"candidatoId\":10,\"profesionalId\":2,\"sillonId\":3,"
                                + "\"inicioDeseado\":\"2026-12-16T13:00:00Z\","
                                + "\"finDeseado\":\"2026-12-16T14:00:00Z\","
                                + "\"depositoRequerido\":true}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.estado").value("activa"));
    }

    @Test
    void consultaPorCandidatoResponde200() throws Exception {
        when(listaEspera.porCandidato(10L)).thenReturn(List.of());
        mvc.perform(get("/lista-espera/candidato/10")).andExpect(status().isOk());
    }

    @Test
    void ofrecerYAdjudicarRespondenSinTokenPublico() throws Exception {
        when(relleno.ofrecerTurnoLiberado(5L))
                .thenReturn(new ResultadoRelleno(5L, 7L, "ofrecida", null));
        when(relleno.adjudicarTurnoLiberado(5L))
                .thenReturn(new ResultadoRelleno(5L, 7L, "adjudicada", "sin_deposito"));

        mvc.perform(post("/lista-espera/ofrecer/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ofrecida"));
        mvc.perform(post("/lista-espera/adjudicar/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.detalle").value("sin_deposito"));
    }

    @Test
    void erroresSeTraducenSinModificarLaCadenaAuth() throws Exception {
        when(listaEspera.registrar(any())).thenThrow(new IllegalArgumentException("rango inválido"));
        when(relleno.adjudicarTurnoLiberado(5L))
                .thenThrow(new IllegalStateException("ya adjudicado"));
        when(relleno.ofrecerTurnoLiberado(6L))
                .thenThrow(new NoSuchElementException("turno inexistente"));

        mvc.perform(post("/lista-espera").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("rango inválido"));
        mvc.perform(post("/lista-espera/adjudicar/5")).andExpect(status().isConflict());
        mvc.perform(post("/lista-espera/ofrecer/6")).andExpect(status().isNotFound());
    }
}
