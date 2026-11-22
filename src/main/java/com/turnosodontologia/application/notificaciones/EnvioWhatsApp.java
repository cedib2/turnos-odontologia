package com.turnosodontologia.application.notificaciones;

import java.util.Map;

/** Mensaje de plantilla aprobada a enviar al proveedor. No admite texto libre. */
public record EnvioWhatsApp(long notificacionId, String destinatario, String codigoPlantilla,
                            String idioma, Map<String, String> parametros) {

    public EnvioWhatsApp {
        parametros = parametros == null ? Map.of() : Map.copyOf(parametros);
    }
}
