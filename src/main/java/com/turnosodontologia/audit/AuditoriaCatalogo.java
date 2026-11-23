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
            "pago.conciliado");

    public void validar(String tipo) {
        if (tipo == null || !TIPOS.contains(tipo)) {
            throw new IllegalArgumentException("Tipo de auditoría no autorizado: " + tipo);
        }
    }
}
