package com.turnosodontologia.audit;

import java.util.Set;

public final class AuditoriaCatalogo {

    public static final Set<String> TIPOS = Set.of(
            "turno.creado",
            "turno.reprogramado",
            "turno.sobreturno",
            "bloqueo.creado",
            "publictoken.historia.denegado",
            "notificacion.programada",
            "notificacion.enviada",
            "notificacion.bloqueada",
            "notificacion.fallida",
            "notificacion.confirmada",
            "whatsapp.webhook.rechazado",
            "clinico.ficha.abierta",
            "clinico.anamnesis.registrada",
            "clinico.odontograma.registrado",
            "clinico.evolucion.registrada",
            "clinico.consentimiento.registrado",
            "clinico.acceso.denegado");

    public void validar(String tipo) {
        if (tipo == null || !TIPOS.contains(tipo)) {
            throw new IllegalArgumentException("Tipo de auditoría no autorizado: " + tipo);
        }
    }
}
