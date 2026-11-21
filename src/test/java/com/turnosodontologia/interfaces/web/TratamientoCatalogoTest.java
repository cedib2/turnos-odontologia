package com.turnosodontologia.interfaces.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.turnosodontologia.application.CatalogoSchedulingService;
import com.turnosodontologia.domain.agenda.Tratamiento;
import com.turnosodontologia.infrastructure.persistence.TratamientoRepository;
import com.turnosodontologia.support.PostgresFlywayTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import({CatalogoSchedulingService.class, TratamientoController.class})
class TratamientoCatalogoTest extends PostgresFlywayTest {

    @Autowired
    private TratamientoController controller;

    @Autowired
    private TratamientoRepository tratamientos;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockMvc mockMvc;

    @BeforeEach
    void setUpMockMvc() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void crudDeTratamientosEnPerfilDevTest() throws Exception {
        long limpiezaId = crear("LIMPIEZA", "Limpieza", 30);
        long extraccionId = crear("EXTRACCION", "Extracción", 45);

        mockMvc.perform(get("/tratamientos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigo").value("LIMPIEZA"))
                .andExpect(jsonPath("$[1].codigo").value("EXTRACCION"));
        mockMvc.perform(get("/tratamientos/" + limpiezaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Limpieza"))
                .andExpect(jsonPath("$.duracionMinutos").value(30));

        mockMvc.perform(put("/tratamientos/" + limpiezaId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"LIMPIEZA_PROF\",\"nombre\":\"Limpieza profunda\","
                                + "\"duracionMinutos\":60}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value("LIMPIEZA_PROF"))
                .andExpect(jsonPath("$.duracionMinutos").value(60));

        mockMvc.perform(get("/tratamientos/" + limpiezaId))
                .andExpect(jsonPath("$.codigo").value("LIMPIEZA_PROF"));
        mockMvc.perform(get("/tratamientos/" + extraccionId))
                .andExpect(jsonPath("$.duracionMinutos").value(45));

        mockMvc.perform(delete("/tratamientos/" + limpiezaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));

        Tratamiento desactivado = tratamientos.findById(limpiezaId).orElseThrow();
        Tratamiento otro = tratamientos.findById(extraccionId).orElseThrow();
        assertFalse(desactivado.isActivo());
        assertEquals("EXTRACCION", otro.getCodigo());
        assertEquals(2, tratamientos.count());
    }

    @Test
    void rechazaCodigoDuplicadoYDatosInvalidos() throws Exception {
        crear("UNICO", "Único", 30);

        mockMvc.perform(post("/tratamientos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"  \",\"nombre\":\"Sin código\",\"duracionMinutos\":30}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/tratamientos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"CERO\",\"nombre\":\"Sin duración\",\"duracionMinutos\":0}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/tratamientos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"UNICO\",\"nombre\":\"Duplicado\",\"duracionMinutos\":20}"))
                .andExpect(status().isConflict());
        mockMvc.perform(get("/tratamientos/999999"))
                .andExpect(status().isNotFound());

        assertThrows(DataIntegrityViolationException.class, () -> tratamientos.saveAndFlush(
                new Tratamiento("UNICO", "Conflicto en base", 10)));
        assertEquals(1, tratamientos.count());
    }

    @Test
    void actualizaPorPatchConValoresDistintosYRechazaNombreVacio() throws Exception {
        long id = crear("RADIOGRAFIA", "Radiografía", 20);

        mockMvc.perform(patch("/tratamientos/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"RADIOGRAFIA_PERI\",\"nombre\":\"Radiografía periapical\","
                                + "\"duracionMinutos\":15}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value("RADIOGRAFIA_PERI"))
                .andExpect(jsonPath("$.duracionMinutos").value(15));

        mockMvc.perform(get("/tratamientos/" + id))
                .andExpect(jsonPath("$.nombre").value("Radiografía periapical"));
        mockMvc.perform(post("/tratamientos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"SIN_NOMBRE\",\"nombre\":\"  \",\"duracionMinutos\":25}"))
                .andExpect(status().isBadRequest());
        assertEquals(1, tratamientos.count());
    }

    private long crear(String codigo, String nombre, int duracionMinutos) throws Exception {
        String respuesta = mockMvc.perform(post("/tratamientos").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TratamientoRequest(
                                codigo, nombre, duracionMinutos))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value(codigo))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(respuesta).get("id").asLong();
    }
}
