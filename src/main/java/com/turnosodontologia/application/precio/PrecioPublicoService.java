package com.turnosodontologia.application.precio;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Precio único, público y predecible (C-21). El monto, la moneda y la periodicidad no están decididos:
 * hasta que el PO los configure ({@code precio.plan.*}) la superficie responde PENDIENTE sin cifra (RN-GL-03, DD-06).
 */
@Service
public class PrecioPublicoService {

    static final String NOTA = "Precio único todo incluido: recordatorios por WhatsApp, seña con Mercado Pago "
            + "y facturación incluidos en el plan, sin desglose de costos de API ni packs adicionales "
            + "(RN-SE-06, RN-NT-04, C-01 A5=2).";

    private final String plan;
    private final BigDecimal monto;
    private final String moneda;
    private final String periodicidad;
    private final List<String> incluye;
    private final String vigenteDesde;

    public PrecioPublicoService(@Value("${precio.plan.nombre:}") String plan,
                                @Value("${precio.plan.monto:}") String monto,
                                @Value("${precio.plan.moneda:}") String moneda,
                                @Value("${precio.plan.periodicidad:}") String periodicidad,
                                @Value("${precio.plan.incluye:}") String incluye,
                                @Value("${precio.plan.vigente-desde:}") String vigenteDesde) {
        this.plan = vacioANull(plan);
        this.monto = parsearMonto(monto);
        this.moneda = parsearMoneda(moneda);
        this.periodicidad = parsearPeriodicidad(periodicidad);
        this.incluye = Arrays.stream(incluye == null ? new String[0] : incluye.split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).toList();
        this.vigenteDesde = vigenteDesde == null || vigenteDesde.isBlank()
                ? null
                : LocalDate.parse(vigenteDesde.trim()).toString();
    }

    public PrecioPublico consultar() {
        boolean completo = plan != null && monto != null && moneda != null && periodicidad != null;
        if (!completo) {
            return new PrecioPublico(PrecioPublico.MODELO, PrecioPublico.PENDIENTE, plan, null, null, null,
                    incluye, true, false, false, null,
                    "Monto, moneda y periodicidad (TBD): pendientes de aprobación del PO. " + NOTA);
        }
        return new PrecioPublico(PrecioPublico.MODELO, PrecioPublico.PUBLICADO, plan, monto, moneda, periodicidad,
                incluye, true, false, false, vigenteDesde, NOTA);
    }

    private static BigDecimal parsearMonto(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        BigDecimal monto = new BigDecimal(valor.trim());
        if (monto.signum() <= 0) {
            throw new IllegalArgumentException("precio.plan.monto debe ser positivo");
        }
        return monto;
    }

    private static String parsearMoneda(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        String moneda = valor.trim().toUpperCase(Locale.ROOT);
        if (!moneda.matches("[A-Z]{3}")) {
            throw new IllegalArgumentException("precio.plan.moneda debe ser un código ISO 4217");
        }
        return moneda;
    }

    private static String parsearPeriodicidad(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        String periodicidad = valor.trim().toUpperCase(Locale.ROOT);
        if (!List.of("MENSUAL", "ANUAL").contains(periodicidad)) {
            throw new IllegalArgumentException("precio.plan.periodicidad debe ser MENSUAL o ANUAL");
        }
        return periodicidad;
    }

    private static String vacioANull(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
