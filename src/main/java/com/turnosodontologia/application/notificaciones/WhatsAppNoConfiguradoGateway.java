package com.turnosodontologia.application.notificaciones;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Fuera de dev/test, mientras el adaptador real de WhatsApp Business API sea {@code (TBD)},
 * todo envío falla de forma explícita y queda registrado como intento con error: nunca se
 * simula un envío exitoso ni se contacta a un servicio externo no aprobado.
 */
@Component
@Profile("!dev & !test")
public class WhatsAppNoConfiguradoGateway implements WhatsAppGateway {

    public static final String ERROR = "proveedor-whatsapp-no-configurado";

    @Override
    public ResultadoEnvio enviar(EnvioWhatsApp envio) {
        return ResultadoEnvio.error(ERROR);
    }
}
