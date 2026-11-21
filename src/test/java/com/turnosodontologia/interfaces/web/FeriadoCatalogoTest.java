package com.turnosodontologia.interfaces.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.turnosodontologia.application.CatalogoSchedulingService;
import com.turnosodontologia.domain.agenda.Feriado;
import com.turnosodontologia.infrastructure.persistence.FeriadoRepository;
import com.turnosodontologia.support.PostgresFlywayTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import({CatalogoSchedulingService.class, FeriadoController.class})
class FeriadoCatalogoTest extends PostgresFlywayTest {

    @Autowired
    private FeriadoController controller;

    @Autowired
    private FeriadoRepository feriados;

    private final ObjectMapper objectMapper = CatalogoMvc.mapper();

    private MockMvc mockMvc;

    @BeforeEach
    void setUpMockMvc() {
        mockMvc = CatalogoMvc.para(controller);
    }

    @Test
    void crudDeFeriadosConAplicaATodosPersistido() throws Exception {
        long navidad = crear("2026-12-25", "Navidad", true);
        long otro = crear("2026-12-26", "Feriado derivado", null);

        mockMvc.perform(get("/feriados"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fecha").value("2026-12-25"))
                .andExpect(jsonPath("$[1].fecha").value("2026-12-26"));
        mockMvc.perform(get("/feriados/" + navidad))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.descripcion").value("Navidad"))
                .andExpect(jsonPath("$.aplicaATodos").value(true));

        mockMvc.perform(put("/feriados/" + navidad).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fecha\":\"2026-12-25\",\"descripcion\":\"Navidad (nacional)\","
                                + "\"aplicaATodos\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.descripcion").value("Navidad (nacional)"))
                .andExpect(jsonPath("$.aplicaATodos").value(false));

        mockMvc.perform(delete("/feriados/" + navidad))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));

        Feriado desactivado = feriados.findById(navidad).orElseThrow();
        Feriado otroFeriado = feriados.findById(otro).orElseThrow();
        assertFalse(desactivado.isActivo());
        assertEquals("Navidad (nacional)", desactivado.getDescripcion());
        assertNull(otroFeriado.getAplicaATodos());
        assertEquals(LocalDate.of(2026, 12, 26), otroFeriado.getFecha());
        assertEquals(2, feriados.count());
    }

    @Test
    void rechazaFeriadosInvalidosYFechaDuplicada() throws Exception {
        crear("2026-11-26", "Fixture", false);

        mockMvc.perform(post("/feriados").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fecha\":\"2026-11-27\",\"descripcion\":\"  \"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/feriados").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"descripcion\":\"Sin fecha\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/feriados").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fecha\":\"2026-11-26\",\"descripcion\":\"Duplicado\"}"))
                .andExpect(status().isConflict());
        mockMvc.perform(get("/feriados/999999"))
                .andExpect(status().isNotFound());

        assertThrows(DataIntegrityViolationException.class, () -> feriados.saveAndFlush(
                new Feriado(LocalDate.of(2026, 11, 26), "Conflicto en base", null)));
        assertEquals(1, feriados.count());
    }

    @Test
    void actualizaPorPatchConOtraFechaYRechazaFechaYaUsada() throws Exception {
        long primero = crear("2027-01-01", "Año nuevo", false);
        long segundo = crear("2027-01-02", "Segundo feriado", true);

        mockMvc.perform(patch("/feriados/" + primero).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fecha\":\"2027-01-03\",\"descripcion\":\"Año nuevo (traslado)\","
                                + "\"aplicaATodos\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fecha").value("2027-01-03"))
                .andExpect(jsonPath("$.aplicaATodos").value(true));

        mockMvc.perform(get("/feriados/" + segundo))
                .andExpect(jsonPath("$.fecha").value("2027-01-02"));
        mockMvc.perform(put("/feriados/" + primero).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fecha\":\"2027-01-02\",\"descripcion\":\"Conflicto\","
                                + "\"aplicaATodos\":false}"))
                .andExpect(status().isConflict());
        assertEquals(2, feriados.count());
    }

    private long crear(String fecha, String descripcion, Boolean aplicaATodos) throws Exception {
        String cuerpo = "{\"fecha\":\"" + fecha + "\",\"descripcion\":\"" + descripcion + "\""
                + (aplicaATodos == null ? "" : ",\"aplicaATodos\":" + aplicaATodos) + "}";
        String respuesta = mockMvc.perform(post("/feriados").contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fecha").value(fecha))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(respuesta).get("id").asLong();
    }
}
