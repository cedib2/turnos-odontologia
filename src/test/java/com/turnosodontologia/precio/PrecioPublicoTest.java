package com.turnosodontologia.precio;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.turnosodontologia.application.precio.PrecioPublico;
import com.turnosodontologia.application.precio.PrecioPublicoService;
import com.turnosodontologia.interfaces.web.PrecioController;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** C-21: precio único, público y todo incluido (A5=2), sin desglose de API ni credenciales. */
class PrecioPublicoTest {

    private static final Set<String> CAMPOS_PERMITIDOS = Set.of("modelo", "estado", "plan", "monto", "moneda",
            "periodicidad", "incluye", "todoIncluido", "desgloseCostosApi", "packsAdicionales", "vigenteDesde",
            "nota");

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void sinMontoAprobadoLaSuperficieQuedaPendienteSinCifra() {
        PrecioPublico precio = servicio("Plan consultorio", "", "", "", "").consultar();

        assertEquals(PrecioPublico.PENDIENTE, precio.estado());
        assertNull(precio.monto());
        assertNull(precio.moneda());
        assertTrue(precio.nota().contains("(TBD)"));
    }

    @Test
    void precioPublicadoCoincideConLoConfiguradoYEsTodoIncluido() {
        PrecioPublico precio = servicio("Plan consultorio", "45000.00", "ars", "mensual",
                "Agenda, Recordatorios WhatsApp, Seña Mercado Pago, Facturación ARCA").consultar();

        assertEquals(PrecioPublico.PUBLICADO, precio.estado());
        assertEquals(PrecioPublico.MODELO, precio.modelo());
        assertEquals(new BigDecimal("45000.00"), precio.monto());
        assertEquals("ARS", precio.moneda());
        assertEquals("MENSUAL", precio.periodicidad());
        assertEquals(4, precio.incluye().size());
        assertTrue(precio.todoIncluido());
        assertFalse(precio.desgloseCostosApi());
        assertFalse(precio.packsAdicionales());
    }

    @Test
    void configuracionInconsistenteSeRechaza() {
        assertThrows(IllegalArgumentException.class, () -> servicio("P", "-1", "ARS", "MENSUAL", ""));
        assertThrows(IllegalArgumentException.class, () -> servicio("P", "10", "PESOS", "MENSUAL", ""));
        assertThrows(IllegalArgumentException.class, () -> servicio("P", "10", "ARS", "POR_MENSAJE", ""));
    }

    @Test
    void laSuperficieSoloExponeCamposComercialesYNingunaCredencial() throws Exception {
        System.setProperty("mercadopago.access-token", "APP_USR-SECRETO-MP");
        System.setProperty("whatsapp.api-token", "EAAG-SECRETO-META");
        try {
            MockMvc mvc = MockMvcBuilders.standaloneSetup(new PrecioController(
                            servicio("Plan consultorio", "45000", "ARS", "MENSUAL", "Agenda")))
                    .setMessageConverters(new MappingJackson2HttpMessageConverter(mapper))
                    .build();
            String cuerpo = mvc.perform(get("/precio")).andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            JsonNode json = mapper.readTree(cuerpo);
            List<String> campos = new ArrayList<>();
            json.fieldNames().forEachRemaining(campos::add);
            assertEquals(new TreeSet<>(CAMPOS_PERMITIDOS), new TreeSet<>(campos));
            assertFalse(cuerpo.contains("SECRETO"));
            assertFalse(cuerpo.toLowerCase().contains("token"));
            assertFalse(cuerpo.toLowerCase().contains("secret"));
        } finally {
            System.clearProperty("mercadopago.access-token");
            System.clearProperty("whatsapp.api-token");
        }
    }

    private static PrecioPublicoService servicio(String plan, String monto, String moneda, String periodicidad,
                                                 String incluye) {
        return new PrecioPublicoService(plan, monto, moneda, periodicidad, incluye, "2027-01-01");
    }
}
