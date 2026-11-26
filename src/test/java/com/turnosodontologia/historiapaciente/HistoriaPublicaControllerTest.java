package com.turnosodontologia.historiapaciente;

import com.turnosodontologia.application.clinico.HistoriaClinica;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class HistoriaPublicaControllerTest {

    private final AccesoHistoriaPacienteService accesos = mock(AccesoHistoriaPacienteService.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new HistoriaPublicaController(accesos)).build();

    private static final HistoriaClinica HISTORIA = new HistoriaClinica(3L, 8L, 5, null, List.of(), List.of(), List.of());

    @Test
    void titularVerificadoVeSuHistoriaConLaEvidenciaEnElCuerpo() throws Exception {
        when(accesos.consultar("tok", "90000008"))
                .thenReturn(new AccesoHistoriaPacienteService.Consulta(true, "ok", HISTORIA));

        mvc.perform(post("/publico/v1/historia/consulta").param("token", "tok")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"documento\":\"90000008\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tieneFicha").value(true))
                .andExpect(jsonPath("$.historia.pacienteId").value(8));
        verify(accesos).consultar("tok", "90000008");
    }

    @Test
    void identidadNoVerificadaResponde403SinDatos() throws Exception {
        when(accesos.consultar("tok", "otro"))
                .thenReturn(new AccesoHistoriaPacienteService.Consulta(false, "identidad_no_verificada", null));

        mvc.perform(post("/publico/v1/historia/consulta").param("token", "tok")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"documento\":\"otro\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("identidad_no_verificada"))
                .andExpect(jsonPath("$.historia").doesNotExist());
    }

    @Test
    void tokenVencidoOInvalidoResponde401() throws Exception {
        when(accesos.consultar(eq("viejo"), anyString()))
                .thenReturn(new AccesoHistoriaPacienteService.Consulta(false, "token_vencido", null));

        mvc.perform(post("/publico/v1/historia/consulta").param("token", "viejo")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"documento\":\"1\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("token_vencido"));
    }

    @Test
    void pacienteSinRegistrosRespondeSinFicha() throws Exception {
        when(accesos.consultar("tok", "1")).thenReturn(new AccesoHistoriaPacienteService.Consulta(true, "ok", null));

        mvc.perform(post("/publico/v1/historia/consulta").param("token", "tok")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"documento\":\"1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tieneFicha").value(false));
    }

    @Test
    void copiaSeDescargaComoAdjuntoConSuHash() throws Exception {
        when(accesos.exportar("tok", "1")).thenReturn(new AccesoHistoriaPacienteService.Copia(true, "ok",
                "{}".getBytes(), "ab".repeat(32), true));

        mvc.perform(post("/publico/v1/historia/copia").param("token", "tok")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"documento\":\"1\"}"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Hash-Documento", "ab".repeat(32)))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"historia-clinica.json\""));
    }

    @Test
    void verificadorDeDevComparaElDocumentoNormalizado() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForList(anyString(), eq(String.class), anyLong())).thenReturn(List.of("90000008"));
        VerificadorIdentidadDocumentoDev verificador = new VerificadorIdentidadDocumentoDev(jdbc);

        assertTrue(verificador.verificar(8L, "90.000.008"));
        assertTrue(verificador.verificar(8L, " 90000008 "));
        assertFalse(verificador.verificar(8L, "90000001"));
        assertFalse(verificador.verificar(8L, ""));
    }
}
