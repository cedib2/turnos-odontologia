package com.turnosodontologia.waitlist;

import com.turnosodontologia.domain.waitlist.MetricaOcupacion;

public record MetricaResponse(Long id, Long sillonId, String periodo, String ocupacion,
                              String ausentismo, String metodologia, String calculadaEn) {

    public static MetricaResponse de(MetricaOcupacion metrica) {
        if (metrica == null) {
            return null;
        }
        return new MetricaResponse(metrica.getId(), metrica.getSillonId(), metrica.getPeriodo(),
                metrica.getOcupacion() == null ? null : metrica.getOcupacion().toPlainString(),
                metrica.getAusentismo() == null ? null : metrica.getAusentismo().toPlainString(),
                metrica.getMetodologia(),
                metrica.getCalculadaEn() == null ? null : metrica.getCalculadaEn().toString());
    }
}
