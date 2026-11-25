package com.turnosodontologia.audit;

import com.turnosodontologia.export.ExportAuditEvents;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class AuditoriaCatalogo {

    public static final Set<String> TIPOS = Stream.concat(
            Stream.of("turno.creado", "turno.reprogramado", "turno.sobreturno",
                    "bloqueo.creado", "publictoken.historia.denegado"),
            ExportAuditEvents.TIPOS.stream())
            .collect(Collectors.toUnmodifiableSet());

    public void validar(String tipo) {
        if (tipo == null || !TIPOS.contains(tipo)) {
            throw new IllegalArgumentException("Tipo de auditoría no autorizado: " + tipo);
        }
    }
}
