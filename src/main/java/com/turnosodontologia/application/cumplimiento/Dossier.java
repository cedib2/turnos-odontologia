package com.turnosodontologia.application.cumplimiento;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Dossier de cumplimiento demostrable (C-18, RN-SE-04). Es un dossier mínimo con pendientes explícitos
 * (C-01 D=b): se puede descargar incompleto, pero solo se publica sin pendientes obligatorios.
 */
public record Dossier(Instant generadoAt, List<Item> items, List<Subprocesador> subprocesadoresIncluidos,
                      List<Subprocesador> subprocesadoresExcluidos, List<String> pendientes,
                      Completitud completitud, Map<String, List<String>> matrizRoles,
                      Map<String, String> parametrosOperativos, boolean publicable) {

    public record Item(String codigo, String descripcion, String categoria, String fuente, String baseNormativa,
                       boolean obligatorio, String estado, String evidencia, String verificadoPor,
                       Instant verificadoAt) {

        public boolean presente() {
            return "PRESENTE".equals(estado);
        }
    }

    public record Subprocesador(Long id, String nombre, String servicio, String ubicacionDatos,
                                String clausulasAaipReferencia, String dpaReferencia) {

        public boolean conClausulas() {
            return clausulasAaipReferencia != null && !clausulasAaipReferencia.isBlank();
        }
    }

    /** Métrica «Evidencia de cumplimiento» de C-01 (tabla de Métricas), con metodología explícita. */
    public record Completitud(int presentes, int exigidos, BigDecimal porcentaje, String formula,
                              String metodologia, String meta) {
    }
}
