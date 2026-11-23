package com.turnosodontologia.depositpayments;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * Verificación local HMAC-SHA256 del webhook (MP-1). El mensaje es
 * {@code ts + "." + x-request-id}; el componente {@code v1} de {@code x-signature}
 * es la firma esperada. No se reconsulta la API de Mercado Pago.
 */
public final class WebhookSignatureVerifier {

    private final byte[] secreto;

    public WebhookSignatureVerifier(String secreto) {
        if (secreto == null || secreto.isBlank()) {
            throw new IllegalArgumentException("El secreto del webhook es obligatorio");
        }
        this.secreto = secreto.getBytes(StandardCharsets.UTF_8);
    }

    public boolean validar(String cabeceraFirma, String requestId, String ts) {
        if (cabeceraFirma == null || cabeceraFirma.isBlank()
                || requestId == null || requestId.isBlank()
                || ts == null || ts.isBlank()) {
            return false;
        }
        String firma = null;
        String timestamp = null;
        for (String parte : cabeceraFirma.split(",")) {
            String[] par = parte.trim().split("=", 2);
            if (par.length != 2) {
                return false;
            }
            if ("v1".equals(par[0])) {
                firma = par[1];
            } else if ("ts".equals(par[0])) {
                timestamp = par[1];
            }
        }
        if (firma == null || timestamp == null || !timestamp.equals(ts)) {
            return false;
        }
        byte[] esperada;
        try {
            esperada = HexFormat.of().parseHex(firma);
        } catch (IllegalArgumentException e) {
            return false;
        }
        byte[] calculada = calcularHmac(ts, requestId);
        return MessageDigest.isEqual(esperada, calculada);
    }

    private byte[] calcularHmac(String ts, String requestId) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secreto, "HmacSHA256"));
            return mac.doFinal((ts + "." + requestId).getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo calcular la firma del webhook", e);
        }
    }
}
