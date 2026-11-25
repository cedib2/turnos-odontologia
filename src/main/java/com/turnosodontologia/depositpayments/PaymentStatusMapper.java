package com.turnosodontologia.depositpayments;

import com.turnosodontologia.domain.financiero.Senia;

import java.util.Map;

/** Mapping cerrado de estados del proveedor a estados de la seña (MP-4). */
public final class PaymentStatusMapper {

    private static final Map<String, String> ESTADOS = Map.of(
            "pending", Senia.PENDIENTE,
            "approved", Senia.APROBADO,
            "rejected", Senia.RECHAZADO,
            "cancelled", Senia.CANCELADO,
            "refunded", Senia.REEMBOLSADO,
            "charged_back", Senia.REEMBOLSADO);

    private PaymentStatusMapper() {
    }

    public static String estado(String estadoProveedor) {
        String estado = ESTADOS.get(estadoProveedor);
        if (estado == null) {
            throw new IllegalArgumentException("Estado de pago no reconocido");
        }
        return estado;
    }

    /**
     * Devuelve {@code null} cuando el estado no tiene tipo de evento autorizado
     * en el catálogo de MP-6 (pendiente y cancelado).
     */
    public static String tipoEvento(String estadoSenia) {
        return switch (estadoSenia) {
            case Senia.APROBADO -> "pago.aprobado";
            case Senia.RECHAZADO -> "pago.rechazado";
            case Senia.REEMBOLSADO -> "pago.reembolsado";
            default -> null;
        };
    }
}
