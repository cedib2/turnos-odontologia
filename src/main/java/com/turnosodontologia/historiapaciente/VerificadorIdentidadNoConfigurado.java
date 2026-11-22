package com.turnosodontologia.historiapaciente;

/** Fuera de dev/test, sin mecanismo decidido, ninguna identidad se da por verificada. */
public class VerificadorIdentidadNoConfigurado implements VerificadorIdentidadTitular {

    @Override
    public boolean verificar(long pacienteId, String evidencia) {
        return false;
    }
}
