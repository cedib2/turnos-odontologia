package com.turnosodontologia.application.metricas;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Métrica auditable (C-22, DD-06, C-01 E): no existe sin definición, fórmula, fuente y período explícitos.
 * La meta queda (TBD) hasta el piloto; nunca se publica una cifra decorativa.
 */
public record Metrica(String codigo, String definicion, String formula, String fuente, Instant periodoDesde,
                      Instant periodoHasta, String zonaHoraria, String estado, Long numerador, Long denominador,
                      BigDecimal valor, String meta, Map<String, Object> detalle) {

    public static final String CALCULADA = "CALCULADA";
    public static final String SIN_DATOS = "SIN_DATOS";
    public static final String NO_DISPONIBLE = "NO_DISPONIBLE";
    public static final String DIFERIDA = "DIFERIDA";
    public static final String META_TBD = "(TBD) — baseline del piloto y umbral revisable (C-01 E, SU-03)";

    public Metrica {
        exigir(codigo, "codigo");
        exigir(definicion, "definicion");
        exigir(formula, "formula");
        exigir(fuente, "fuente");
        exigir(zonaHoraria, "zonaHoraria");
        exigir(estado, "estado");
        if (periodoDesde == null || periodoHasta == null || !periodoHasta.isAfter(periodoDesde)) {
            throw new IllegalArgumentException("La métrica " + codigo + " requiere un período explícito válido");
        }
        meta = meta == null || meta.isBlank() ? META_TBD : meta;
        detalle = detalle == null ? Map.of() : new LinkedHashMap<>(detalle);
    }

    static BigDecimal porcentaje(long numerador, long denominador) {
        return denominador == 0
                ? null
                : BigDecimal.valueOf(numerador * 100L).divide(BigDecimal.valueOf(denominador), 2, RoundingMode.HALF_UP);
    }

    private static void exigir(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("Metodología incompleta: falta " + campo);
        }
    }
}
