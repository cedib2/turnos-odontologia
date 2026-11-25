package com.turnosodontologia.depositpayments;

import com.turnosodontologia.domain.auditoria.ActorSimulado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MercadoPagoWebhookControllerTest {

    private static final String SECRETO = "test-webhook-secret-c15";

    private SeniaService servicio;
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        servicio = mock(SeniaService.class);
        mvc = MockMvcBuilders.standaloneSetup(new MercadoPagoWebhookController(
                servicio, new WebhookSignatureVerifier(SECRETO))).build();
    }

    @Test
    void webhookConFirmaValidaProcesaElEvento() throws Exception {
        String ts = "1763880000";
        String requestId = "req-webhook-021";
        when(servicio.procesarWebhook(any(), eq(requestId), any())).thenReturn(
                new WebhookResult(false, true, "aprobado", true));

        mvc.perform(post("/publico/v1/webhooks/mercadopago")
                        .header("x-signature", "ts=" + ts + ",v1=" + firmar(ts, requestId))
                        .header("x-request-id", requestId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"externalReference":"senia-webhook-021","status":"approved",
                                 "paymentId":"pay-021"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("aprobado"))
                .andExpect(jsonPath("$.conciliado").value(true));

        verify(servicio).procesarWebhook(any(), eq(requestId), eq(ActorSimulado.system()));
    }

    @Test
    void firmaInvalidaResponde400SinTocarElServicio() throws Exception {
        String contenido = mvc.perform(post("/publico/v1/webhooks/mercadopago")
                        .header("x-signature", "ts=1763880000,v1=" + "0".repeat(64))
                        .header("x-request-id", "req-invalida")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"externalReference":"senia-webhook-022","status":"approved"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.motivo").value("firma_invalida"))
                .andReturn().getResponse().getContentAsString();

        verifyNoInteractions(servicio);
        org.junit.jupiter.api.Assertions.assertFalse(contenido.contains(SECRETO));
    }

    @Test
    void firmaAusenteResponde400SinProcesar() throws Exception {
        mvc.perform(post("/publico/v1/webhooks/mercadopago")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"externalReference":"senia-webhook-023","status":"rejected"}
                                """))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(servicio);
    }

    private String firmar(String ts, String requestId) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(SECRETO.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal((ts + "." + requestId)
                    .getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
