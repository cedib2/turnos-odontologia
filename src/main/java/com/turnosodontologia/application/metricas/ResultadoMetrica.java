package com.turnosodontologia.application.metricas;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

/**
 * Resultado auditable de una métrica: siempre lleva metodología y período explícitos.
 */
public record ResultadoMetrica(String codigo, EstadoMetrica estado, Long numerador, Long denominador,
                               BigDecimal porcentaje, Instant desde, Instant hasta,
                               MetodologiaMetrica metodologia, String dependenciaPendiente) {

    public ResultadoMetrica {
        if (codigo == null || codigo.isBlank() || estado == null) {
            throw new IllegalArgumentException("Código y estado de la métrica son obligatorios");
        }
        if (metodologia == null) {
            throw new IllegalArgumentException("Metodología obligatoria para " + codigo);
        }
        if (desde == null || hasta == null) {
            throw new IllegalArgumentException("Período obligatorio para " + codigo);
        }
        if (estado == EstadoMetrica.CALCULADA && porcentaje == null) {
            throw new IllegalArgumentException("Una métrica calculada requiere porcentaje");
        }
        if (estado != EstadoMetrica.CALCULADA && porcentaje != null) {
            throw new IllegalArgumentException("Sin cálculo no se publica porcentaje");
        }
        if (estado == EstadoMetrica.NO_DISPONIBLE
                && (dependenciaPendiente == null || dependenciaPendiente.isBlank())) {
            throw new IllegalArgumentException("Una métrica no disponible debe nombrar su dependencia");
        }
    }

    public static ResultadoMetrica desdeConteo(String codigo, long numerador, long denominador,
                                               Periodo periodo, MetodologiaMetrica metodologia) {
        if (numerador < 0 || denominador < 0 || numerador > denominador) {
            throw new IllegalArgumentException("Conteo inconsistente para " + codigo);
        }
        if (denominador == 0) {
            return new ResultadoMetrica(codigo, EstadoMetrica.SIN_DATOS, numerador, denominador, null,
                    periodo.desde(), periodo.hasta(), metodologia, null);
        }
        BigDecimal porcentaje = BigDecimal.valueOf(numerador).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominador), 2, RoundingMode.HALF_UP);
        return new ResultadoMetrica(codigo, EstadoMetrica.CALCULADA, numerador, denominador, porcentaje,
                periodo.desde(), periodo.hasta(), metodologia, null);
    }

    public static ResultadoMetrica noDisponible(String codigo, Periodo periodo,
                                                MetodologiaMetrica metodologia, String dependencia) {
        return new ResultadoMetrica(codigo, EstadoMetrica.NO_DISPONIBLE, null, null, null,
                periodo.desde(), periodo.hasta(), metodologia, dependencia);
    }
}
