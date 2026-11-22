package com.turnosodontologia.application.notificaciones;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Profile;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebhookSignatureVerifierTest {

    private static final byte[] CUERPO = "{\"entry\":[]}".getBytes(StandardCharsets.UTF_8);

    @Test
    void aceptaSoloLaFirmaHmacDelCuerpoExacto() {
        WebhookSignatureVerifier verificador = new WebhookSignatureVerifier("secreto-de-prueba");
        String firma = "sha256=" + HexFormat.of().formatHex(verificador.calcular(CUERPO));

        assertTrue(verificador.esValida(firma, CUERPO));
        assertFalse(verificador.esValida(firma, "{\"entry\":[1]}".getBytes(StandardCharsets.UTF_8)));
        assertFalse(verificador.esValida(firma.toUpperCase().replace("SHA256=", "sha1="), CUERPO));
    }

    @Test
    void sinFirmaFirmaMalFormadaOSinSecretoRechaza() {
        WebhookSignatureVerifier verificador = new WebhookSignatureVerifier("secreto-de-prueba");
        assertFalse(verificador.esValida(null, CUERPO));
        assertFalse(verificador.esValida("", CUERPO));
        assertFalse(verificador.esValida("sha256=zz", CUERPO));
        assertFalse(verificador.esValida("sha256=" + "00".repeat(32), CUERPO));

        WebhookSignatureVerifier sinSecreto = new WebhookSignatureVerifier("");
        String firmaConOtroSecreto = "sha256=" + HexFormat.of().formatHex(verificador.calcular(CUERPO));
        assertFalse(sinSecreto.esValida(firmaConOtroSecreto, CUERPO));
    }

    @Test
    void gatewaySimuladoSoloEnDevTestYFueraFallaCerrado() {
        assertEquals(List.of("dev", "test"),
                List.of(SimulatedWhatsAppGateway.class.getAnnotation(Profile.class).value()));
        assertEquals(List.of("!dev & !test"),
                List.of(WhatsAppNoConfiguradoGateway.class.getAnnotation(Profile.class).value()));

        ResultadoEnvio resultado = new WhatsAppNoConfiguradoGateway()
                .enviar(new EnvioWhatsApp(1L, "+5490000000", "plantilla", "es_AR", null));
        assertFalse(resultado.exitoso());
        assertEquals(WhatsAppNoConfiguradoGateway.ERROR, resultado.error());
    }
}
