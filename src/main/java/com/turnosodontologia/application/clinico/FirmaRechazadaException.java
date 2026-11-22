package com.turnosodontologia.application.clinico;

public class FirmaRechazadaException extends RuntimeException {

    public FirmaRechazadaException(String motivo) {
        super(motivo);
    }
}
