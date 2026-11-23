package com.turnosodontologia.depositpayments;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.hamcrest.Matchers.containsString;

class MercadoPagoRestAdapterTest {

    @Test
    void creaPreferenciaConReferenciaMontoYClaveDeIdempotencia() {
        RestTemplate rest = new RestTemplate();
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(rest).build();
        MercadoPagoPort adaptador = new MercadoPagoRestAdapter(rest,
                "http://mercadopago.test", "test-access-token");

        servidor.expect(requestTo("http://mercadopago.test/v1/preferences"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Idempotency-Key", "idem-021-10"))
                .andExpect(header("Authorization", "Bearer test-access-token"))
                .andExpect(content().string(containsString("senia-ref-021-10")))
                .andExpect(content().string(containsString("10.00")))
                .andRespond(withSuccess("{\"id\":\"pref-021-10\"}", MediaType.APPLICATION_JSON));

        assertEquals("pref-021-10", adaptador.crearPreferencia("senia-ref-021-10",
                new BigDecimal("10.00"), "idem-021-10"));
        servidor.verify();
    }

    @Test
    void reembolsaElTotalConClavePropia() {
        RestTemplate rest = new RestTemplate();
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(rest).build();
        MercadoPagoPort adaptador = new MercadoPagoRestAdapter(rest,
                "http://mercadopago.test", "test-access-token");

        servidor.expect(requestTo("http://mercadopago.test/v1/payments/pay-021-11/refund"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Idempotency-Key", "senia-ref-021-11-reembolso"))
                .andExpect(content().string(containsString("25.50")))
                .andRespond(withSuccess("{\"id\":\"refund-021-11\"}", MediaType.APPLICATION_JSON));

        adaptador.reembolsar("pay-021-11", "senia-ref-021-11", new BigDecimal("25.50"));
        servidor.verify();
    }

    @Test
    void noAgregaDependenciasAlPom() throws Exception {
        String pom = Files.readString(Path.of("pom.xml")).toLowerCase();
        assertFalse(pom.contains("mercadopago"));
        assertFalse(pom.contains("sdk-java"));
    }
}
