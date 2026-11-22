package com.turnosodontologia.application.notificaciones;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * Verifica la firma del webhook de WhatsApp: HMAC-SHA256 del cuerpo crudo con el secreto de
 * la app, en el formato {@code sha256=<hex>}. Sin secreto configurado rechaza todo (falla
 * cerrado). El valor del secreto es {@code (TBD)} y vive solo en variables de entorno.
 */
@Component
public class WebhookSignatureVerifier {

    private static final String PREFIJO = "sha256=";
    private final byte[] secreto;

    public WebhookSignatureVerifier(@Value("${WHATSAPP_APP_SECRET:}") String secreto) {
        this.secreto = secreto == null ? new byte[0] : secreto.getBytes(StandardCharsets.UTF_8);
    }

    public boolean esValida(String firma, byte[] cuerpo) {
        if (secreto.length == 0 || firma == null || cuerpo == null || !firma.startsWith(PREFIJO)) {
            return false;
        }
        byte[] recibida;
        try {
            recibida = HexFormat.of().parseHex(firma.substring(PREFIJO.length()));
        } catch (IllegalArgumentException e) {
            return false;
        }
        return MessageDigest.isEqual(calcular(cuerpo), recibida);
    }

    byte[] calcular(byte[] cuerpo) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secreto, "HmacSHA256"));
            return mac.doFinal(cuerpo);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
