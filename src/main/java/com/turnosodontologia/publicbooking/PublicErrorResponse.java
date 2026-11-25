package com.turnosodontologia.publicbooking;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PublicErrorResponse(String error, Boolean requiereNuevoEnlace,
                                  Boolean derivarARecepcion, List<HuecoResponse> alternativas) {

    public static PublicErrorResponse de(PublicApiException excepcion) {
        return new PublicErrorResponse(
                excepcion.motivo(),
                excepcion.requiereNuevoEnlace() ? Boolean.TRUE : null,
                excepcion.derivarARecepcion() ? Boolean.TRUE : null,
                excepcion.alternativas().isEmpty() ? null : excepcion.alternativas());
    }

    public static PublicErrorResponse simple(String motivo) {
        return new PublicErrorResponse(motivo, null, null, null);
    }
}
