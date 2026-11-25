package com.turnosodontologia.application.cumplimiento;

import java.util.Map;
import java.util.Optional;

/**
 * Contrato con C-11/C-13: aporta la copia de la historia clínica de un paciente para la exportación.
 * Mientras C-13 no exista no hay implementación y la exportación lo declara como pendiente.
 */
public interface ProveedorHistoriaClinica {

    Optional<Map<String, Object>> copiaHistoria(long pacienteId);
}
