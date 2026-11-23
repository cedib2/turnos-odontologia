package com.turnosodontologia.invoicing;

import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

/** Adaptador HTTP de ARCA sobre {@link RestTemplate} (AR-7), sin SDK externo. */
public final class FacturacionRestAdapter implements FacturacionPort {

    public static final String CODIGO_ERROR = "ERROR_PROVEEDOR";

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String accessToken;

    public FacturacionRestAdapter(RestTemplate restTemplate, String baseUrl, String accessToken) {
        this.restTemplate = restTemplate;
        String url = baseUrl == null ? "" : baseUrl.trim();
        this.baseUrl = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
        this.accessToken = accessToken == null ? "" : accessToken;
    }

    @Override
    @SuppressWarnings("unchecked")
    public FacturacionResultado emitir(SolicitudFacturacion solicitud) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("tipo", solicitud.tipo());
        cuerpo.put("referencia", solicitud.referencia());
        cuerpo.put("concepto", solicitud.concepto());
        cuerpo.put("monto", solicitud.monto().toPlainString());
        cuerpo.put("moneda", solicitud.moneda());
        cuerpo.put("letra", solicitud.letra());
        cuerpo.put("puntoVenta", solicitud.puntoVenta());
        cuerpo.put("receptorConsumidorFinal", true);
        cuerpo.put("ivaNoDiscriminado", true);

        HttpEntity<Map<String, Object>> peticion = new HttpEntity<>(
                cuerpo, FacturacionCredentialHeaders.crear(accessToken, solicitud.referencia()));
        try {
            ResponseEntity<Map> respuesta = restTemplate.postForEntity(
                    baseUrl + "/comprobantes", peticion, Map.class);
            Object cae = respuesta.getBody() == null ? null : respuesta.getBody().get("cae");
            if (respuesta.getStatusCode().is2xxSuccessful() && cae != null && !cae.toString().isBlank()) {
                return FacturacionResultado.aprobado(cae.toString());
            }
        } catch (RestClientException ignored) {
            // El detalle de la excepción puede contener credenciales: nunca se propaga.
        }
        return FacturacionResultado.rechazado(CODIGO_ERROR);
    }
}
