package com.turnosodontologia.interfaces.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.turnosodontologia.application.CatalogoSchedulingService;
import com.turnosodontologia.domain.agenda.HorarioAtencion;
import com.turnosodontologia.domain.agenda.Profesional;
import com.turnosodontologia.infrastructure.persistence.HorarioAtencionRepository;
import com.turnosodontologia.infrastructure.persistence.ProfesionalRepository;
import com.turnosodontologia.support.PostgresFlywayTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import({CatalogoSchedulingService.class, HorarioController.class})
class HorarioCatalogoTest extends PostgresFlywayTest {

    @Autowired
    private HorarioController controller;

    @Autowired
    private HorarioAtencionRepository horarios;

    @Autowired
    private ProfesionalRepository profesionales;

    private final ObjectMapper objectMapper = CatalogoMvc.mapper();

    private MockMvc mockMvc;

    @BeforeEach
    void setUpMockMvc() {
        mockMvc = CatalogoMvc.para(controller);
    }

    @Test
    void crudDeHorariosGlobalesYPorProfesional() throws Exception {
        Long profesionalId = profesionales.save(new Profesional("MAT-701", "Ana", "Pérez")).getId();
        long global = crear(null, 2, "09:00:00", "17:00:00");
        long propio = crear(profesionalId.intValue(), 4, "08:30:00", "12:30:00");

        mockMvc.perform(get("/horarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].diaSemana").value(2))
                .andExpect(jsonPath("$[1].diaSemana").value(4));
        mockMvc.perform(get("/horarios/" + global))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.desde").value("09:00:00"));
        mockMvc.perform(get("/horarios/" + propio))
                .andExpect(jsonPath("$.profesionalId").value(profesionalId));

        mockMvc.perform(put("/horarios/" + global).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"profesionalId\":null,\"diaSemana\":3,"
                                + "\"desde\":\"10:00:00\",\"hasta\":\"14:00:00\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.diaSemana").value(3))
                .andExpect(jsonPath("$.hasta").value("14:00:00"));

        mockMvc.perform(delete("/horarios/" + global))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));

        HorarioAtencion desactivado = horarios.findById(global).orElseThrow();
        HorarioAtencion otro = horarios.findById(propio).orElseThrow();
        assertFalse(desactivado.isActivo());
        assertEquals(LocalTime.of(8, 30), otro.getDesde());
        assertEquals(profesionalId, otro.getProfesionalId());
        assertEquals(2, horarios.count());
    }

    @Test
    void rechazaHorariosInvalidosYProfesionalInexistente() throws Exception {
        mockMvc.perform(post("/horarios").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"profesionalId\":null,\"diaSemana\":0,"
                                + "\"desde\":\"09:00:00\",\"hasta\":\"17:00:00\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/horarios").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"profesionalId\":null,\"diaSemana\":8,"
                                + "\"desde\":\"09:00:00\",\"hasta\":\"17:00:00\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/horarios").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"profesionalId\":null,\"diaSemana\":2,"
                                + "\"desde\":\"17:00:00\",\"hasta\":\"09:00:00\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/horarios").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"profesionalId\":999999,\"diaSemana\":2,"
                                + "\"desde\":\"09:00:00\",\"hasta\":\"17:00:00\"}"))
                .andExpect(status().isConflict());
        mockMvc.perform(get("/horarios/999999"))
                .andExpect(status().isNotFound());
        assertEquals(0, horarios.count());
    }

    @Test
    void actualizaPorPatchConOtrosValoresYRechazaRangoIgual() throws Exception {
        long id = crear(null, 5, "07:00:00", "11:00:00");

        mockMvc.perform(patch("/horarios/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"profesionalId\":null,\"diaSemana\":1,"
                                + "\"desde\":\"07:30:00\",\"hasta\":\"12:00:00\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.diaSemana").value(1))
                .andExpect(jsonPath("$.desde").value("07:30:00"));

        mockMvc.perform(get("/horarios/" + id))
                .andExpect(jsonPath("$.hasta").value("12:00:00"));
        mockMvc.perform(post("/horarios").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"profesionalId\":null,\"diaSemana\":6,"
                                + "\"desde\":\"09:00:00\",\"hasta\":\"09:00:00\"}"))
                .andExpect(status().isBadRequest());
        assertEquals(1, horarios.count());
    }

    private long crear(Integer profesionalId, int diaSemana, String desde, String hasta) throws Exception {
        String respuesta = mockMvc.perform(post("/horarios").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new HorarioRequest(profesionalId, diaSemana, LocalTime.parse(desde),
                                        LocalTime.parse(hasta)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.diaSemana").value(diaSemana))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(respuesta).get("id").asLong();
    }
}
