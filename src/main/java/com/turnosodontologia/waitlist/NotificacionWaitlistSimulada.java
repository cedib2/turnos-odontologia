package com.turnosodontologia.waitlist;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Adaptador NO-OP/simulado (W-3). Registra la intención en memoria para poder
 * verificarla en tests; no realiza llamadas de red ni requiere credenciales.
 */
@Component
// `default` cubre los contextos de test sin perfil explícito (@DataJpaTest y
// PublicBookingIntegrationTest); el adaptador sigue fuera de un perfil productivo.
@Profile({"dev", "test", "default"})
public class NotificacionWaitlistSimulada implements NotificacionWaitlistPort {

    private final List<OfertaWaitlist> ofertas = new CopyOnWriteArrayList<>();

    @Override
    public void ofrecer(OfertaWaitlist oferta) {
        ofertas.add(oferta);
    }

    public List<OfertaWaitlist> ofertas() {
        return List.copyOf(ofertas);
    }

    public void clear() {
        ofertas.clear();
    }
}
