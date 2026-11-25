package com.turnosodontologia.interfaces.web;

import com.turnosodontologia.application.precio.PrecioPublicoService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PrecioControllerTest {

    private static final List<String> TERMINOS_SENSIBLES = List.of(
            "token", "secret", "password", "apikey", "api_key", "credential", "bearer", "authorization");

    @Test
    void publicaElPrecioConfiguradoYSuDetalle() throws Exception {
        mvc(new PrecioPublicoService("25000", "ARS", "mensual")).perform(get("/precio"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("PUBLICADO"))
                .andExpect(jsonPath("$.monto").value(25000))
                .andExpect(jsonPath("$.total").value(25000))
                .andExpect(jsonPath("$.desgloseCostosApi").value(false))
                .andExpect(jsonPath("$.cargosAdicionales").isEmpty());
    }

    @Test
    void sinMontoLaSuperficieMuestraPendiente() throws Exception {
        mvc(new PrecioPublicoService("", "", "")).perform(get("/precio"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("PENDIENTE_DEFINICION"))
                .andExpect(jsonPath("$.monto").doesNotExist());
    }

    @Test
    void laRespuestaNoExponeCredencialesNiNombresDeSecretos() throws Exception {
        String cuerpo = mvc(new PrecioPublicoService("25000", "ARS", "mensual")).perform(get("/precio"))
                .andReturn().getResponse().getContentAsString();
        String normalizado = cuerpo.toLowerCase(Locale.ROOT);
        for (String termino : TERMINOS_SENSIBLES) {
            assertFalse(normalizado.contains(termino), "término sensible en /precio: " + termino);
        }
        for (String variable : Files.readAllLines(Path.of(".env.example"))) {
            if (!variable.isBlank()) {
                assertFalse(cuerpo.contains(variable), "variable de entorno expuesta: " + variable);
            }
        }
    }

    @Test
    void elCodigoDePrecioNoLeeVariablesDeCredenciales() throws Exception {
        Path paquete = Path.of("src", "main", "java", "com", "turnosodontologia", "application", "precio");
        Path controlador = Path.of("src", "main", "java", "com", "turnosodontologia", "interfaces", "web",
                "PrecioController.java");
        String codigo;
        try (Stream<Path> archivos = Stream.concat(Files.walk(paquete), Stream.of(controlador))) {
            codigo = String.join("\n", archivos.filter(Files::isRegularFile).map(p -> {
                try {
                    return Files.readString(p);
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }).toList());
        }
        for (String variable : Files.readAllLines(Path.of(".env.example"))) {
            if (!variable.isBlank()) {
                assertFalse(codigo.contains(variable), "el código de precio referencia " + variable);
            }
        }
    }

    private static MockMvc mvc(PrecioPublicoService servicio) {
        return MockMvcBuilders.standaloneSetup(new PrecioController(servicio)).build();
    }
}
