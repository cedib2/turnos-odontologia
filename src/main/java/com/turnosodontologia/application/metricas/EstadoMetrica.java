package com.turnosodontologia.application.metricas;

public enum EstadoMetrica {
    /** Numerador y denominador obtenidos de la fuente; porcentaje calculado. */
    CALCULADA,
    /** Fuente disponible pero sin casos en el período: no se publica porcentaje. */
    SIN_DATOS,
    /** La fuente depende de un change todavía no integrado. */
    NO_DISPONIBLE
}
