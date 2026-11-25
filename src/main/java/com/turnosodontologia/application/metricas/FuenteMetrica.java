package com.turnosodontologia.application.metricas;

/**
 * Puerto de cálculo de una métrica. Los changes que aportan datos (C-09 ventana de
 * cancelación, C-10 confirmaciones, C-18 dossier) registran su implementación como bean
 * y el tablero la toma sin cambios.
 */
public interface FuenteMetrica {

    String codigo();

    ResultadoMetrica calcular(Periodo periodo, MetodologiaMetrica metodologia);
}
