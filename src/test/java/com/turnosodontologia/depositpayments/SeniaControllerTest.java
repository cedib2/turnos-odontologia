package com.turnosodontologia.depositpayments;

import com.turnosodontologia.domain.auditoria.ActorSimulado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SeniaControllerTest {

    private SeniaService servicio;
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        servicio = mock(SeniaService.class);
        mvc = MockMvcBuilders.standaloneSetup(new SeniaController(servicio)).build();
    }

    @Test
    void creaSeniaConPresupuesto() throws Exception {
        when(servicio.solicitar(any(), any())).thenReturn(new SeniaResponse(
                1L, "senia-021", "pendiente", "ARS", "100.00", "pref-021"));

        mvc.perform(post("/senias").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"turnoId":10,"presupuestoId":20,"monto":100.00,
                                 "idempotencyKey":"idem-021"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.externalReference").value("senia-021"))
                .andExpect(jsonPath("$.estado").value("pendiente"))
                .andExpect(jsonPath("$.moneda").value("ARS"));
        verify(servicio).solicitar(any(), eq(ActorSimulado.recepcionista()));
    }

    @Test
    void creaSeniaSinPresupuesto() throws Exception {
        when(servicio.solicitar(any(), any())).thenReturn(new SeniaResponse(
                2L, "senia-022", "pendiente", "ARS", "50.00", null));

        mvc.perform(post("/senias").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"turnoId":10,"monto":50.00,"idempotencyKey":"idem-022"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2));
    }

    @Test
    void reembolsaSeniaAprobada() throws Exception {
        when(servicio.reembolsar(eq(3L), any())).thenReturn(
                new RefundResponse(3L, "reembolsado", new BigDecimal("100.00")));

        mvc.perform(post("/senias/3/reembolsar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("reembolsado"))
                .andExpect(jsonPath("$.montoReembolsado").value(100.00));
    }

    @Test
    void reembolsoRechazadoRespondeConflict() throws Exception {
        when(servicio.reembolsar(eq(4L), any()))
                .thenThrow(new IllegalStateException("Solo se puede reembolsar una seña aprobada"));

        mvc.perform(post("/senias/4/reembolsar"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.motivo").exists());
    }

    @Test
    void seniaInexistenteRespondeNotFound() throws Exception {
        when(servicio.reembolsar(eq(5L), any()))
                .thenThrow(new NoSuchElementException("Seña inexistente"));

        mvc.perform(post("/senias/5/reembolsar"))
                .andExpect(status().isNotFound());
    }
}
