package com.turnosodontologia.application.cumplimiento;

import java.util.Map;
import java.util.Optional;

/**
 * Contrato con C-11/C-13: aporta la copia de la historia clínica de un paciente para la exportación.
 * Lo implementa {@code application.clinico.HistoriaClinicaParaExportacion} con la vista vigente de C-11/C-13.
 */
public interface ProveedorHistoriaClinica {

    Optional<Map<String, Object>> copiaHistoria(long pacienteId);
}
