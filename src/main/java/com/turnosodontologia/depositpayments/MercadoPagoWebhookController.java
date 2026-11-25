package com.turnosodontologia.depositpayments;

import com.turnosodontologia.auth.WebhookSignatureHeaders;
import com.turnosodontologia.domain.auditoria.ActorSimulado;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;

import java.util.Map;

/**
 * Webhook público de Mercado Pago (MP-1/MP-2): se autentica solo por firma HMAC,
 * sin token público de C-06. Una firma inválida responde 400 sin efectos.
 */
@RestController
@RequestMapping("/publico/v1/webhooks/mercadopago")
@Profile({"dev", "test"})
public class MercadoPagoWebhookController {

    private final SeniaService servicio;
    private final WebhookSignatureVerifier verificador;

    public MercadoPagoWebhookController(SeniaService servicio,
                                        WebhookSignatureVerifier verificador) {
        this.servicio = servicio;
        this.verificador = verificador;
    }

    @PostMapping
    public ResponseEntity<?> recibir(WebRequest request, @RequestBody WebhookRequest cuerpo) {
        WebhookSignatureHeaders.Datos cabeceras = WebhookSignatureHeaders.extraer(request);
        String ts = extraerTs(cabeceras.firma());
        if (!verificador.validar(cabeceras.firma(), cabeceras.requestId(), ts)) {
            return ResponseEntity.badRequest().body(Map.of("motivo", "firma_invalida"));
        }
        WebhookResult resultado = servicio.procesarWebhook(
                new WebhookSenia(cuerpo.externalReference(), cuerpo.status(),
                        cuerpo.paymentId(), ts),
                cabeceras.requestId(), ActorSimulado.system());
        return ResponseEntity.ok(resultado);
    }

    private String extraerTs(String firma) {
        if (firma == null) {
            return null;
        }
        for (String parte : firma.split(",")) {
            String[] par = parte.trim().split("=", 2);
            if (par.length == 2 && "ts".equals(par[0])) {
                return par[1];
            }
        }
        return null;
    }

    public record WebhookRequest(String externalReference, String status, String paymentId) {
    }
}
