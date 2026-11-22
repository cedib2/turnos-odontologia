package com.turnosodontologia.historiapaciente;

/**
 * Verificación de identidad del titular de la historia (RN-CL-04). El mecanismo es
 * {@code (TBD)}: en dev/test se usa {@link SimulatedVerificadorIdentidad}; fuera de esos
 * perfiles {@link VerificadorIdentidadNoConfigurado} no verifica a nadie (falla cerrado).
 */
public interface VerificadorIdentidadTitular {

    boolean verificar(long pacienteId, String evidencia);
}
