package com.turnosodontologia.interfaces.web;

import com.turnosodontologia.application.clinico.HistoriaClinicaService;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FichaClinicaControllerTest {

    private final HistoriaClinicaService servicio = mock(HistoriaClinicaService.class);
    private final MockMvc mvc = CatalogoMvc.para(new FichaClinicaController(servicio));

    @Test
    void soloExisteEnDevYTestComoLaAgenda() {
        assertEquals(List.of("dev", "test"),
                List.of(FichaClinicaController.class.getAnnotation(Profile.class).value()));
    }

    @Test
    void abreFichaDesdeTurnoYRegistraOdontograma() throws Exception {
        when(servicio.abrirDesdeTurno(9L)).thenReturn(4L);
        when(servicio.registrarOdontograma(4L, 55, "oclusal", "caries")).thenReturn(1);

        mvc.perform(post("/fichas").contentType(MediaType.APPLICATION_JSON).content("{\"turnoId\":9}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.fichaId").value(4));
        mvc.perform(post("/fichas/4/odontograma").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"piezaFdi\":55,\"cara\":\"oclusal\",\"estado\":\"caries\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.version").value(1));
        mvc.perform(post("/fichas/4/anamnesis").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"respuestas\":{\"alergias\":\"no\"},\"textoLibre\":\"t\"}"))
                .andExpect(status().isOk());

        verify(servicio).registrarAnamnesis(4L, Map.of("alergias", "no"), "t");
    }
}
