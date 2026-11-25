package com.turnosodontologia.depositpayments;

import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebhookSignatureVerifierTest {

    private static final String SECRETO = "test-webhook-secret-c15";

    private final WebhookSignatureVerifier verifier = new WebhookSignatureVerifier(SECRETO);

    @Test
    void aceptaFirmaCalculadaConTimestampYRequestId() {
        String ts = "1763880000";
        String requestId = "req-021-valida";
        String firma = "ts=" + ts + ",v1=" + firmar(ts, requestId);

        assertTrue(verifier.validar(firma, requestId, ts));
    }

    @Test
    void rechazaFirmaAusenteMalformadaOInvalida() {
        String ts = "1763880000";
        String requestId = "req-021-invalida";

        assertFalse(verifier.validar(null, requestId, ts));
        assertFalse(verifier.validar("", requestId, ts));
        assertFalse(verifier.validar("no-es-un-header", requestId, ts));
        assertFalse(verifier.validar("ts=" + ts, requestId, ts));
        assertFalse(verifier.validar("v1=abc", requestId, ts));
        assertFalse(verifier.validar("ts=" + ts + ",v1=" + "0".repeat(64), requestId, ts));
        assertFalse(verifier.validar("ts=" + ts + ",v1=" + firmar(ts, requestId),
                "otro-request-id", ts));
    }

    @Test
    void elSecretoNoApareceEnLaApiDelVerificador() {
        assertFalse(verifier.toString().contains(SECRETO));
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
