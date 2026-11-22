package com.turnosodontologia.application.notificaciones;

public class WebhookRechazadoException extends RuntimeException {

    public WebhookRechazadoException(String motivo) {
        super(motivo);
    }
}
