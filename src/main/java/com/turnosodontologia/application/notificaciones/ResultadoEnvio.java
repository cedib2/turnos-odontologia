package com.turnosodontologia.application.notificaciones;

public record ResultadoEnvio(boolean exitoso, String proveedorMessageId, String error) {

    public static ResultadoEnvio exito(String proveedorMessageId) {
        if (proveedorMessageId == null || proveedorMessageId.isBlank()) {
            throw new IllegalArgumentException("Envío exitoso sin proveedor_message_id");
        }
        return new ResultadoEnvio(true, proveedorMessageId, null);
    }

    public static ResultadoEnvio error(String error) {
        return new ResultadoEnvio(false, null, error);
    }
}
