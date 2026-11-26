package com.turnosodontologia.waitlist;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Fuera de dev/test/default, mientras el canal real del aviso de lista de espera sea {@code (TBD)}, ofrecer un
 * hueco falla de forma explícita: nunca se simula un aviso enviado. La transacción de la oferta se revierte, así
 * que el hueco no queda reservado a un paciente que no fue avisado. Mismo criterio que
 * {@code WhatsAppNoConfiguradoGateway} (C-10).
 */
@Component
@Profile("!dev & !test & !default")
public class NotificacionWaitlistNoConfigurada implements NotificacionWaitlistPort {

    public static final String ERROR = "notificacion-waitlist-no-configurada";

    @Override
    public void ofrecer(OfertaWaitlist oferta) {
        throw new IllegalStateException(ERROR + ": no se puede avisar la oferta " + oferta.entradaId()
                + " del turno " + oferta.turnoId());
    }
}
