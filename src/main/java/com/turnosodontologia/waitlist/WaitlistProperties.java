package com.turnosodontologia.waitlist;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

/**
 * Propiedades no secretas de C-19. Los defaults son valores de prueba seguros:
 * nunca una cifra de negocio ni un plazo productivo (RN-GL-03, W-5, W-6, W-12).
 */
@ConfigurationProperties(prefix = "waitlist")
public class WaitlistProperties {

    private long ofertaMinutos = 15;
    private String metricasPeriodo = "YYYY-MM";
    private BigDecimal depositoMonto = BigDecimal.ONE;

    public long getOfertaMinutos() {
        return ofertaMinutos;
    }

    public void setOfertaMinutos(long ofertaMinutos) {
        if (ofertaMinutos <= 0) {
            throw new IllegalArgumentException("waitlist.oferta.minutos debe ser positivo");
        }
        this.ofertaMinutos = ofertaMinutos;
    }

    public String getMetricasPeriodo() {
        return metricasPeriodo;
    }

    public void setMetricasPeriodo(String metricasPeriodo) {
        if (metricasPeriodo == null || !metricasPeriodo.equals("YYYY-MM")) {
            throw new IllegalArgumentException("waitlist.metricas.periodo debe ser YYYY-MM");
        }
        this.metricasPeriodo = metricasPeriodo;
    }

    public BigDecimal getDepositoMonto() {
        return depositoMonto;
    }

    public void setDepositoMonto(BigDecimal depositoMonto) {
        if (depositoMonto == null || depositoMonto.signum() <= 0) {
            throw new IllegalArgumentException("waitlist.deposito.monto debe ser positivo");
        }
        this.depositoMonto = depositoMonto;
    }

    public long ofertaMinutos() {
        return ofertaMinutos;
    }

    public String periodoFormato() {
        return metricasPeriodo;
    }

    /** Importe determinista de prueba; el monto productivo sigue siendo (TBD) -> forma de obtención. */
    public BigDecimal depositoMonto() {
        return depositoMonto;
    }
}
