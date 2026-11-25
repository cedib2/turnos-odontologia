package com.turnosodontologia.auth;

import org.springframework.web.context.request.WebRequest;

/** Lectura aislada de las cabeceras de firma del webhook (MP-1). */
public final class WebhookSignatureHeaders {

    private WebhookSignatureHeaders() {
    }

    public static Datos extraer(WebRequest request) {
        return new Datos(primera(request.getHeaderValues("x-signature")),
                primera(request.getHeaderValues("x-request-id")));
    }

    private static String primera(String[] valores) {
        return valores == null || valores.length == 0 ? null : valores[0];
    }

    public record Datos(String firma, String requestId) {
    }
}
