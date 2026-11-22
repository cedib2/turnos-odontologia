package com.turnosodontologia.application.clinico;

public class AccesoClinicoDenegadoException extends RuntimeException {

    public AccesoClinicoDenegadoException(String motivo) {
        super(motivo);
    }
}
