package com.turnosodontologia.invoicing;

import com.turnosodontologia.domain.auditoria.ActorSimulado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** AR-13: cuatro rutas aisladas, sin credenciales de staff ni RBAC nuevo. */
class ComprobanteControllerTest {

    private ComprobanteService servicio;
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        servicio = mock(ComprobanteService.class);
        mvc = MockMvcBuilders.standaloneSetup(new ComprobanteController(servicio)).build();
    }

    @Test
    void exponeEmisionReintentoNotaDeCreditoYConsulta() throws Exception {
        ComprobanteResponse emitida = respuesta(1L, "factura", "emitido");
        when(servicio.emitir(eq(12L), any(ActorSimulado.class))).thenReturn(emitida);
        when(servicio.reintentar(1L, ActorSimulado.recepcionista())).thenReturn(emitida);
        when(servicio.notaCredito(1L, ActorSimulado.recepcionista()))
                .thenReturn(respuesta(2L, "nota_credito", "emitido"));
        when(servicio.consultar(1L)).thenReturn(emitida);

        mvc.perform(post("/comprobantes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"seniaId\":12}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.estado").value("emitido"));
        mvc.perform(post("/comprobantes/1/reintentar")).andExpect(status().isOk());
        mvc.perform(post("/comprobantes/1/nota-credito"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("nota_credito"));
        mvc.perform(get("/comprobantes/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.seniaId").value(12));

        verify(servicio).emitir(eq(12L), any(ActorSimulado.class));
        verify(servicio).reintentar(1L, ActorSimulado.recepcionista());
        verify(servicio).notaCredito(1L, ActorSimulado.recepcionista());
        verify(servicio).consultar(1L);
    }

    @Test
    void traduceErroresSinExponerDetalles() throws Exception {
        when(servicio.emitir(eq(99L), any(ActorSimulado.class)))
                .thenThrow(new IllegalStateException("Solo se factura una seña aprobada"));
        when(servicio.consultar(404L)).thenThrow(new NoSuchElementException("Comprobante inexistente"));

        mvc.perform(post("/comprobantes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"seniaId\":99}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.motivo").value("Solo se factura una seña aprobada"));
        mvc.perform(get("/comprobantes/404")).andExpect(status().isNotFound());
    }

    @Test
    void elControladorSoloExisteEnDevYTestYNoTocaLaCadenaAuth() throws Exception {
        Profile perfil = ComprobanteController.class.getAnnotation(Profile.class);
        assertArrayEquals(new String[]{"dev", "test"}, perfil.value());

        String cadenaAuth = Files.readString(Path.of(
                "src/main/java/com/turnosodontologia/auth/AuthSecurityConfig.java"));
        assertTrue(cadenaAuth.contains("securityMatcher(\"/auth/**\")"));
        assertTrue(cadenaAuth.contains("anyRequest().permitAll()"));
    }

    private ComprobanteResponse respuesta(Long id, String tipo, String estado) {
        return new ComprobanteResponse(id, 12L, 20L, 30L, tipo, estado, "ARS",
                new BigDecimal("100.00"), "Seña odontológica", "CAE-TEST-1", 1, false);
    }
}
