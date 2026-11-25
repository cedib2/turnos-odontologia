package com.turnosodontologia.interfaces.web;

import com.turnosodontologia.application.SimulatedCurrentActorProvider;
import com.turnosodontologia.application.metricas.MetricasAdopcionService;
import com.turnosodontologia.infrastructure.audit.InMemoryAuditEventRecorder;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Profile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MetricasControllerTest {

    private final InMemoryAuditEventRecorder recorder = new InMemoryAuditEventRecorder();
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new MetricasController(
            new MetricasAdopcionService(List.of(), recorder, new SimulatedCurrentActorProvider()))).build();

    @Test
    void sinPeriodoResponde400() throws Exception {
        mvc.perform(get("/metricas/adopcion")).andExpect(status().isBadRequest());
        assertTrue(recorder.eventos().isEmpty());
    }

    @Test
    void periodoInvertidoResponde400() throws Exception {
        mvc.perform(get("/metricas/adopcion")
                        .param("desde", "2027-01-01T03:00:00Z")
                        .param("hasta", "2026-12-01T03:00:00Z"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void respondeMetodologiaYPeriodoPorMetrica() throws Exception {
        mvc.perform(get("/metricas/adopcion")
                        .param("desde", "2026-12-01T03:00:00Z")
                        .param("hasta", "2027-01-01T03:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metricas[0].metodologia.formula").exists())
                .andExpect(jsonPath("$.metricas[0].desde").exists())
                .andExpect(jsonPath("$.metricas[0].estado").value("NO_DISPONIBLE"));
    }

    @Test
    void laSuperficieEstaLimitadaADevYTest() {
        Profile profile = MetricasController.class.getAnnotation(Profile.class);
        assertTrue(Arrays.asList(profile.value()).containsAll(List.of("dev", "test")));
    }
}
