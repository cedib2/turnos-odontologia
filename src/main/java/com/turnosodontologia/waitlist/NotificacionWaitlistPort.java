package com.turnosodontologia.waitlist;

/** Puerto de ofrecimiento de C-19 (W-3). Sin WhatsApp real, outbox ni plantillas. */
public interface NotificacionWaitlistPort {

    void ofrecer(OfertaWaitlist oferta);
}
