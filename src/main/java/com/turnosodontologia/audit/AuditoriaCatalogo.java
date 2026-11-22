package com.turnosodontologia.audit;

import java.util.Set;

public final class AuditoriaCatalogo {

    public static final Set<String> TIPOS = Set.of(
            "turno.creado",
            "turno.reprogramado",
            "turno.sobreturno",
            "bloqueo.creado",
            "publictoken.historia.denegado");

    public void validar(String tipo) {
        if (tipo == null || !TIPOS.contains(tipo)) {
            throw new IllegalArgumentException("Tipo de auditoría no autorizado: " + tipo);
        }
    }
}
