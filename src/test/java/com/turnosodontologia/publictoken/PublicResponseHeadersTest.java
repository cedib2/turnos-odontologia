package com.turnosodontologia.publictoken;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PublicResponseHeadersTest {

    @Test
    void exitoYRechazoDeLaSuperficiePublicaIncluyenHeadersSinLeerRequestHeaders() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new ControladorPublicoPrueba())
                .setControllerAdvice(new PublicResponseHeaders(List.of("/publico")))
                .build();

        mvc.perform(get("/publico/prueba/ok"))
                .andExpect(status().isOk())
                .andExpect(header().string("Referrer-Policy", "no-referrer"))
                .andExpect(header().string("Cache-Control", "no-store"));

        mvc.perform(get("/publico/prueba/rechazo"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Referrer-Policy", "no-referrer"))
                .andExpect(header().string("Cache-Control", "no-store"));
    }

    @RestController
    @RequestMapping("/publico/prueba")
    static class ControladorPublicoPrueba {

        @GetMapping("/ok")
        Map<String, String> ok() {
            return Map.of("estado", "ok");
        }

        @GetMapping("/rechazo")
        Map<String, String> rechazo() {
            throw new IllegalStateException("rechazo-de-prueba");
        }

        @ExceptionHandler(IllegalStateException.class)
        ResponseEntity<Map<String, String>> rechazar() {
            return ResponseEntity.badRequest().body(Map.of("motivo", "rechazado"));
        }
    }
}
