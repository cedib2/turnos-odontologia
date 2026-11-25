package com.turnosodontologia.application.precio;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Superficie de consulta del precio (C-21). Lee solo parámetros comerciales no secretos
 * ({@code precio.plan.*}); nunca lee credenciales de WhatsApp, Mercado Pago ni ARCA.
 * Si el PO no configuró el monto, responde {@link EstadoPrecio#PENDIENTE_DEFINICION}.
 */
@Service
public class PrecioPublicoService {

    public static final String MODELO = "PLANO_TODO_INCLUIDO";
    public static final String DECISION = "C-01 A5 — precio plano todo incluido, sin desglose de costos de API";

    static final List<ConceptoIncluido> INCLUYE = List.of(
            ConceptoIncluido.incluido("agenda", "Agenda con duración variable, bloqueos y anti-solapamiento"),
            ConceptoIncluido.incluido("reserva_online", "Reserva y reprogramación por link/QR"),
            ConceptoIncluido.incluido("whatsapp", "Notificaciones por WhatsApp Business API, mensajería de Meta incluida"),
            ConceptoIncluido.incluido("mercadopago", "Integración de señas con Mercado Pago"),
            ConceptoIncluido.incluido("arca", "Facturación electrónica ARCA (C-01 A1)"),
            ConceptoIncluido.incluido("cumplimiento", "Auditoría, exportación, respaldo y dossier de cumplimiento"));

    static final String PENDIENTE_MONTO =
            "(TBD) monto, moneda y periodicidad del plan: a definir por el PO (US-016)";
    static final String PENDIENTE_COMISIONES =
            "(TBD) tratamiento de las comisiones que Mercado Pago cobra sobre cada seña: a confirmar por el PO";

    private final PrecioPublico precio;

    public PrecioPublicoService(@Value("${precio.plan.monto:}") String monto,
                                @Value("${precio.plan.moneda:}") String moneda,
                                @Value("${precio.plan.periodicidad:}") String periodicidad) {
        this.precio = construir(vacioANull(monto), vacioANull(moneda), vacioANull(periodicidad));
    }

    public PrecioPublico consultar() {
        return precio;
    }

    private static PrecioPublico construir(String monto, String moneda, String periodicidad) {
        List<String> pendientes = new ArrayList<>();
        pendientes.add(PENDIENTE_COMISIONES);
        if (monto == null && moneda == null && periodicidad == null) {
            pendientes.add(0, PENDIENTE_MONTO);
            return new PrecioPublico(EstadoPrecio.PENDIENTE_DEFINICION, MODELO, null, null, null, null,
                    INCLUYE, List.of(), false, pendientes, DECISION);
        }
        if (monto == null || moneda == null || periodicidad == null) {
            throw new IllegalStateException(
                    "precio.plan.monto, precio.plan.moneda y precio.plan.periodicidad se configuran juntos");
        }
        BigDecimal valor;
        try {
            valor = new BigDecimal(monto);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("precio.plan.monto no es numérico");
        }
        if (valor.signum() <= 0) {
            throw new IllegalStateException("precio.plan.monto debe ser positivo");
        }
        if (!moneda.matches("[A-Z]{3}")) {
            throw new IllegalStateException("precio.plan.moneda debe ser un código ISO 4217");
        }
        return new PrecioPublico(EstadoPrecio.PUBLICADO, MODELO, valor, moneda, periodicidad, valor,
                INCLUYE, List.of(), false, pendientes, DECISION);
    }

    private static String vacioANull(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
