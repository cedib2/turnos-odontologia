package com.turnosodontologia.publicbooking;

import org.springframework.http.HttpStatus;

import java.util.List;

public class PublicApiException extends RuntimeException {

    private final HttpStatus status;
    private final String motivo;
    private final boolean requiereNuevoEnlace;
    private final boolean derivarARecepcion;
    private final List<HuecoResponse> alternativas;

    public PublicApiException(HttpStatus status, String motivo) {
        this(status, motivo, false, false, List.of());
    }

    public PublicApiException(HttpStatus status, String motivo, boolean requiereNuevoEnlace,
                              boolean derivarARecepcion, List<HuecoResponse> alternativas) {
        super(motivo);
        this.status = status;
        this.motivo = motivo;
        this.requiereNuevoEnlace = requiereNuevoEnlace;
        this.derivarARecepcion = derivarARecepcion;
        this.alternativas = alternativas == null ? List.of() : List.copyOf(alternativas);
    }

    public static PublicApiException vencido(String motivo) {
        return new PublicApiException(HttpStatus.GONE, motivo, true, false, List.of());
    }

    public static PublicApiException fueraDeVentana() {
        return new PublicApiException(HttpStatus.CONFLICT, "fuera_de_ventana", false, true, List.of());
    }

    public static PublicApiException huecoTomado(List<HuecoResponse> alternativas) {
        return new PublicApiException(HttpStatus.CONFLICT, "hueco_tomado", false, false, alternativas);
    }

    public HttpStatus status() {
        return status;
    }

    public String motivo() {
        return motivo;
    }

    public boolean requiereNuevoEnlace() {
        return requiereNuevoEnlace;
    }

    public boolean derivarARecepcion() {
        return derivarARecepcion;
    }

    public List<HuecoResponse> alternativas() {
        return alternativas;
    }
}
