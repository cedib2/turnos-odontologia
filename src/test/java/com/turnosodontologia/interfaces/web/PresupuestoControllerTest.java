package com.turnosodontologia.interfaces.web;

import com.turnosodontologia.application.PresupuestoService;
import com.turnosodontologia.domain.financiero.Presupuesto;
import com.turnosodontologia.domain.financiero.PresupuestoNoModificableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PresupuestoControllerTest {

    private PresupuestoService servicio;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        servicio = mock(PresupuestoService.class);
        mvc = MockMvcBuilders.standaloneSetup(new PresupuestoController(servicio)).build();
    }

    @Test
    void devuelveEstadoYContratoAlCrear() throws Exception {
        when(servicio.crear(eq(1L), eq(null), any())).thenReturn(presupuesto());

        mvc.perform(post("/presupuestos").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"pacienteId":1,"items":[{"tratamientoId":2,"cantidad":2,
                                "precio":10.00,"descripcion":"Limpieza"}]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.pacienteId").value(1))
                .andExpect(jsonPath("$.turnoId").doesNotExist())
                .andExpect(jsonPath("$.estado").value("emitido"))
                .andExpect(jsonPath("$.moneda").value("ARS"))
                .andExpect(jsonPath("$.total").value(20.00))
                .andExpect(jsonPath("$.activo").value(true))
                .andExpect(jsonPath("$.items[0].tratamientoId").value(2));
    }

    @Test
    void devuelveListaYObtener() throws Exception {
        when(servicio.listar()).thenReturn(List.of(presupuesto()));
        when(servicio.obtener(10L)).thenReturn(presupuesto());

        mvc.perform(get("/presupuestos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10));
        mvc.perform(get("/presupuestos/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    void actualizaYAnula() throws Exception {
        Presupuesto anulado = presupuesto();
        anulado.anular("qa");
        when(servicio.modificar(eq(10L), eq(null), any())).thenReturn(presupuesto());
        when(servicio.anular(10L)).thenReturn(anulado);

        String cuerpo = """
                {"turnoId":null,"items":[{"tratamientoId":2,"cantidad":2,
                "precio":10.00,"descripcion":"Limpieza"}]}
                """;
        mvc.perform(put("/presupuestos/10").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(20.00));
        mvc.perform(patch("/presupuestos/10/anular"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("anulado"))
                .andExpect(jsonPath("$.activo").value(false));
    }

    @Test
    void traduceErroresAEstadosHttp() throws Exception {
        when(servicio.obtener(99L)).thenThrow(new NoSuchElementException("Inexistente"));
        when(servicio.anular(11L)).thenThrow(new PresupuestoNoModificableException("Bloqueado"));
        when(servicio.crear(any(), any(), any()))
                .thenThrow(new IllegalArgumentException("Sin items"));

        mvc.perform(get("/presupuestos/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Inexistente"));
        mvc.perform(patch("/presupuestos/11/anular"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Bloqueado"));
        mvc.perform(post("/presupuestos").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Sin items"));
    }

    private Presupuesto presupuesto() {
        Presupuesto presupuesto = new Presupuesto(paciente(), null) {
            @Override
            public Long getId() {
                return 10L;
            }
        };
        presupuesto.agregarItem(tratamiento(), 2, new BigDecimal("10.00"), "Limpieza");
        return presupuesto;
    }

    private com.turnosodontologia.domain.agenda.Tratamiento tratamiento() {
        com.turnosodontologia.domain.agenda.Tratamiento tratamiento =
                new com.turnosodontologia.domain.agenda.Tratamiento("LIMPIEZA", "Limpieza", 30);
        asignarId(tratamiento, 2L);
        return tratamiento;
    }

    private com.turnosodontologia.domain.identidad.Paciente paciente() {
        com.turnosodontologia.domain.identidad.Paciente paciente =
                new com.turnosodontologia.domain.identidad.Paciente(
                        com.turnosodontologia.domain.identidad.DocumentoTipo.DNI, "1", "Nombre",
                        "Apellido", "555");
        asignarId(paciente, 1L);
        return paciente;
    }

    private void asignarId(Object entidad, Long id) {
        try {
            java.lang.reflect.Field campo = entidad.getClass().getDeclaredField("id");
            campo.setAccessible(true);
            campo.set(entidad, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
