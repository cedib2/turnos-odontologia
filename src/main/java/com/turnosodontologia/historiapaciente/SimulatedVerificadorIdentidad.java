package com.turnosodontologia.historiapaciente;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Verificador simulado para dev/test: solo acepta la evidencia que el test registró para cada
 * paciente. No representa ningún mecanismo real decidido.
 */
public class SimulatedVerificadorIdentidad implements VerificadorIdentidadTitular {

    private final Map<Long, String> evidencias = new ConcurrentHashMap<>();

    @Override
    public boolean verificar(long pacienteId, String evidencia) {
        return evidencia != null && evidencia.equals(evidencias.get(pacienteId));
    }

    public void registrar(long pacienteId, String evidencia) {
        evidencias.put(pacienteId, evidencia);
    }

    public void limpiar() {
        evidencias.clear();
    }
}
