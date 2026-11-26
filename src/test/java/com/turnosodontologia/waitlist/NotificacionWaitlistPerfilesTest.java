package com.turnosodontologia.waitlist;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Profile;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * El puerto de aviso de la lista de espera tiene implementación en todo perfil: la simulada en dev/test/default
 * y la no configurada en cualquier otro (p. ej. prod), que falla de forma explícita en lugar de simular un envío.
 */
class NotificacionWaitlistPerfilesTest {

    @Test
    void losPerfilesDeAmbasImplementacionesSonComplementarios() {
        assertArrayEquals(new String[]{"dev", "test", "default"},
                NotificacionWaitlistSimulada.class.getAnnotation(Profile.class).value());
        assertArrayEquals(new String[]{"!dev & !test & !default"},
                NotificacionWaitlistNoConfigurada.class.getAnnotation(Profile.class).value());
    }

    @Test
    void fueraDeDevTestDefaultOfrecerFallaSinSimularElAviso() {
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> new NotificacionWaitlistNoConfigurada().ofrecer(new OfertaWaitlist(7L, 42L)));

        assertTrue(error.getMessage().startsWith(NotificacionWaitlistNoConfigurada.ERROR));
    }
}
