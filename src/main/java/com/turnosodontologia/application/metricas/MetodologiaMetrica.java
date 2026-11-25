package com.turnosodontologia.application.metricas;

/**
 * Metodología publicable de una métrica. Ningún campo puede faltar: sin metodología
 * no hay cifra (C-01 §E, DD-06, SU-04).
 */
public record MetodologiaMetrica(String definicion, String formula, String fuente, String metaInicial) {

    public MetodologiaMetrica {
        requerir(definicion, "definicion");
        requerir(formula, "formula");
        requerir(fuente, "fuente");
        requerir(metaInicial, "metaInicial");
    }

    private static void requerir(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("Metodología incompleta: falta " + campo);
        }
    }
}
