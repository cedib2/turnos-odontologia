package com.turnosodontologia.audit;

import java.util.Set;

public final class AuditoriaCatalogo {

    public static final Set<String> TIPOS = Set.of(
            "turno.creado",
            "turno.confirmado",
            "turno.cancelado",
            "turno.reprogramado",
            "turno.sobreturno",
            "bloqueo.creado",
            "publictoken.historia.denegado",
            "presupuesto.creado",
            "presupuesto.modificado",
            "pago.solicitado",
            "pago.aprobado",
            "pago.rechazado",
            "pago.reembolsado",
            "pago.conciliado",
            "factura.emitida",
            "factura.rechazada",
            "factura.conciliada",
            "factura.anulada",
            "nota_credito.emitida",
            // C-19 (W-10): catálogo ampliado por acta PO del 2026-11-23.
            "waitlist.creado",
            "waitlist.ofrecido",
            "waitlist.adjudicado",
            "waitlist.expirado",
            "turno.inasistencia",
            "metrica.publicada");

    public void validar(String tipo) {
        if (tipo == null || !TIPOS.contains(tipo)) {
            throw new IllegalArgumentException("Tipo de auditoría no autorizado: " + tipo);
        }
    }
}
