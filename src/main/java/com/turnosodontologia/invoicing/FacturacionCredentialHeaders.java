package com.turnosodontologia.invoicing;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * Cabeceras salientes del frente ARCA: nunca se registran ni se exponen en
 * respuestas. Se construye un MultiValueMap para no introducir API de request
 * HTTP fuera del paquete de autenticación (invariante del proyecto).
 */
public final class FacturacionCredentialHeaders {

    private FacturacionCredentialHeaders() {
    }

    public static MultiValueMap<String, String> crear(String accessToken, String referencia) {
        MultiValueMap<String, String> cabeceras = new LinkedMultiValueMap<>();
        cabeceras.add("Content-Type", "application/json");
        cabeceras.add("Authorization", "Bearer " + (accessToken == null ? "" : accessToken));
        cabeceras.add("X-Referencia", referencia == null ? "" : referencia);
        return cabeceras;
    }
}
