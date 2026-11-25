package com.turnosodontologia.invoicing;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** AR-7: REST con RestTemplate, sin SDK, sin credenciales en el cuerpo. */
class FacturacionRestAdapterTest {

    @Test
    void enviaPayloadYCabecerasAcotadasAlProveedor() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(restTemplate).build();
        servidor.expect(requestTo("http://arca.test/comprobantes"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer token-de-prueba"))
                .andExpect(header("X-Referencia", "senia-42"))
                .andExpect(content().string(containsString("\"tipo\":\"factura\"")))
                .andExpect(content().string(containsString("\"monto\":\"100.00\"")))
                .andExpect(content().string(containsString("\"moneda\":\"ARS\"")))
                .andExpect(content().string(containsString("\"letra\":\"C\"")))
                .andExpect(content().string(containsString("\"ivaNoDiscriminado\":true")))
                .andExpect(content().string(containsString("\"receptorConsumidorFinal\":true")))
                .andRespond(withSuccess("{\"cae\":\"CAE-123\"}", MediaType.APPLICATION_JSON));

        FacturacionPort adaptador = new FacturacionRestAdapter(
                restTemplate, "http://arca.test/", "token-de-prueba");
        FacturacionResultado resultado = adaptador.emitir(new SolicitudFacturacion(
                "factura", "senia-42", "Seña odontológica", new BigDecimal("100.00"),
                "ARS", "C", "0001"));

        assertEquals("CAE-123", resultado.cae());
        assertEquals(true, resultado.exitoso());
        servidor.verify();
    }

    @Test
    void unErrorDelProveedorNuncaPropagaSuDetalle() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(restTemplate).build();
        servidor.expect(requestTo("http://arca.test/comprobantes"))
                .andRespond(withBadRequest().body("token=APP_USR-secreto")
                        .contentType(MediaType.APPLICATION_JSON));

        FacturacionResultado resultado = new FacturacionRestAdapter(
                restTemplate, "http://arca.test", "token-de-prueba")
                .emitir(new SolicitudFacturacion("factura", "senia-43", "Seña odontológica",
                        new BigDecimal("100.00"), "ARS", "C", "0001"));

        assertFalse(resultado.exitoso());
        assertEquals(FacturacionRestAdapter.CODIGO_ERROR, resultado.codigoError());
        assertFalse(resultado.codigoError().contains("APP_USR"));
        servidor.verify();
    }
}
