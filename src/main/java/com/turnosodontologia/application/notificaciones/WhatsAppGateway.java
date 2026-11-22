package com.turnosodontologia.application.notificaciones;

/**
 * Puerto hacia WhatsApp Business API (RN-NT-01, DD-02). La implementación concreta contra el
 * proveedor es {@code (TBD)}: en dev/test se usa {@link SimulatedWhatsAppGateway} y fuera de
 * esos perfiles {@link WhatsAppNoConfiguradoGateway}, que falla cerrado sin llamar a terceros.
 */
public interface WhatsAppGateway {

    ResultadoEnvio enviar(EnvioWhatsApp envio);
}
