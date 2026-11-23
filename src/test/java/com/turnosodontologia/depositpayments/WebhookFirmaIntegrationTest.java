package com.turnosodontologia.depositpayments;

import com.turnosodontologia.audit.RealAuditPublisherConfig;
import com.turnosodontologia.domain.financiero.Senia;
import com.turnosodontologia.infrastructure.persistence.SeniaRepository;
import com.turnosodontologia.support.PostgresFlywayTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc(addFilters = false)
@ExtendWith(OutputCaptureExtension.class)
@Import({RealAuditPublisherConfig.class, PagoTestConfig.class})
class WebhookFirmaIntegrationTest extends PostgresFlywayTest {

    private static final String SECRETO = "test-webhook-secret-c15";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private SeniaRepository senias;

    @Test
    void firmaInvalidaNoCambiaElEstadoNiInsertaAuditoria(CapturedOutput salida) throws Exception {
        Senia seña = senias.saveAndFlush(new Senia(1L, null, new BigDecimal("10.00"),
                "senia-firma-invalida-021", "clave-firma-invalida-021"));

        String respuesta = mvc.perform(post("/publico/v1/webhooks/mercadopago")
                        .header("x-signature", "ts=1763880000,v1=" + "0".repeat(64))
                        .header("x-request-id", "req-firma-invalida")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"externalReference":"senia-firma-invalida-021",
                                 "status":"approved"}
                                """))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();
        assertFalse(respuesta.contains(SECRETO));
        assertFalse(salida.getAll().contains(SECRETO));

        assertEquals("pendiente", senias.findById(seña.getId()).orElseThrow().getEstado());
        assertEquals(0, contar("auditoria_log"));
        assertEquals(0, contarIdempotencia());
    }

    @Test
    void firmaValidaProcesaElPagoYDejaUnaFilaDeAuditoria(CapturedOutput salida) throws Exception {
        Senia seña = senias.saveAndFlush(new Senia(1L, null, new BigDecimal("10.00"),
                "senia-firma-valida-021", "clave-firma-valida-021"));
        String ts = "1763880000";
        String requestId = "req-firma-valida-021";

        mvc.perform(post("/publico/v1/webhooks/mercadopago")
                        .header("x-signature", "ts=" + ts + ",v1=" + firmar(ts, requestId))
                        .header("x-request-id", requestId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"externalReference":"senia-firma-valida-021",
                                 "status":"rejected","paymentId":"pay-firma-021"}
                                """))
                .andExpect(status().isOk());

        assertEquals("rechazado", senias.findById(seña.getId()).orElseThrow().getEstado());
        Integer aprobados = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM auditoria_log WHERE accion = 'pago.rechazado'",
                Integer.class);
        assertEquals(1, aprobados);
        String metadata = jdbcTemplate.queryForObject(
                "SELECT metadata::text FROM auditoria_log", String.class);
        assertFalse(metadata.contains(SECRETO));
        assertFalse(salida.getAll().contains(SECRETO));
        assertEquals(1, contarIdempotencia());
    }

    private int contar(String tabla) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tabla, Integer.class);
    }

    private int contarIdempotencia() {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM public_idempotencia WHERE operacion = 'pago.webhook'",
                Integer.class);
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
