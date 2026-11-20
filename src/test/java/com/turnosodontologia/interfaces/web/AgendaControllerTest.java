package com.turnosodontologia.interfaces.web;

import com.turnosodontologia.application.AgendaConsultaService;
import com.turnosodontologia.application.EntradaAgenda;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AgendaControllerTest {

    @Mock
    private AgendaConsultaService consulta;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AgendaController(consulta)).build();
    }

    @Test
    void exponeLaVistaDiariaFiltradaPorRecurso() throws Exception {
        when(consulta.porDia(7L, 9L, LocalDate.of(2026, 12, 28)))
                .thenReturn(List.of(new EntradaAgenda("turno", 1L,
                        Instant.parse("2026-12-28T16:00:00Z"),
                        Instant.parse("2026-12-28T16:30:00Z"), 7L, 9L, "reservado")));

        mockMvc.perform(get("/agenda/dia")
                        .param("fecha", "2026-12-28")
                        .param("profesionalId", "7")
                        .param("sillonId", "9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tipo").value("turno"))
                .andExpect(jsonPath("$[0].detalle").value("reservado"));

        verify(consulta).porDia(7L, 9L, LocalDate.of(2026, 12, 28));
    }

    @Test
    void exponeLaVistaSemanalEnLaZonaConfigurada() throws Exception {
        when(consulta.porSemana(7L, null, LocalDate.of(2026, 12, 28))).thenReturn(List.of());

        mockMvc.perform(get("/agenda/semana")
                        .param("inicio", "2026-12-28")
                        .param("profesionalId", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        verify(consulta).porSemana(7L, null, LocalDate.of(2026, 12, 28));
    }
}
