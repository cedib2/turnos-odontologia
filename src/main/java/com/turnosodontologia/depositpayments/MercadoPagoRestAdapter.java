package com.turnosodontologia.depositpayments;

import com.turnosodontologia.auth.MercadoPagoCredentialHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** Adaptador HTTP de Mercado Pago sobre {@link RestTemplate} (MP-2/MP-5). */
public final class MercadoPagoRestAdapter implements MercadoPagoPort {

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String accessToken;

    public MercadoPagoRestAdapter(RestTemplate restTemplate, String baseUrl, String accessToken) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.accessToken = accessToken == null ? "" : accessToken;
    }

    @Override
    public String crearPreferencia(String externalReference, BigDecimal monto,
                                   String idempotencyKey) {
        Map<String, Object> item = Map.of(
                "title", "Seña turno odontología",
                "quantity", 1,
                "currency_id", "ARS",
                "unit_price", monto.toPlainString());
        Map<String, Object> cuerpo = Map.of(
                "external_reference", externalReference,
                "items", List.of(item));
        Map<?, ?> respuesta = restTemplate.postForObject(baseUrl + "/v1/preferences",
                new HttpEntity<>(cuerpo, headers(idempotencyKey)), Map.class);
        Object id = respuesta == null ? null : respuesta.get("id");
        return id == null ? "" : String.valueOf(id);
    }

    @Override
    public void reembolsar(String paymentId, String externalReference, BigDecimal monto) {
        Map<String, Object> cuerpo = Map.of("amount", monto.toPlainString());
        String destino = (paymentId == null || paymentId.isBlank())
                ? externalReference : paymentId;
        restTemplate.postForObject(baseUrl + "/v1/payments/" + destino + "/refund",
                new HttpEntity<>(cuerpo, headers(externalReference + "-reembolso")), Map.class);
    }

    private MultiValueMap<String, String> headers(String idempotencyKey) {
        return MercadoPagoCredentialHeaders.crear(accessToken, idempotencyKey);
    }
}
