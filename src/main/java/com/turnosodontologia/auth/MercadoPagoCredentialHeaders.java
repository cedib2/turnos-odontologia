package com.turnosodontologia.auth;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;

/**
 * Construcción de cabeceras de autenticación del frente de cobro. Vive en el
 * paquete de autenticación para que ningún controlador ni adaptador de negocio
 * manipule credenciales ni cabeceras de identidad directamente.
 */
public final class MercadoPagoCredentialHeaders {

    private MercadoPagoCredentialHeaders() {
    }

    public static MultiValueMap<String, String> crear(String accessToken, String idempotencyKey) {
        HttpHeaders cabeceras = new HttpHeaders();
        cabeceras.setContentType(MediaType.APPLICATION_JSON);
        cabeceras.setBearerAuth(accessToken == null ? "" : accessToken);
        cabeceras.set("X-Idempotency-Key", idempotencyKey);
        return cabeceras;
    }
}
