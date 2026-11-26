package com.turnosodontologia.publicbooking;

import com.turnosodontologia.application.notificaciones.AvisosTurno;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class NotificacionWhatsAppReservaTest {

    private final AvisosTurno avisos = mock(AvisosTurno.class);
    private final NotificacionWhatsAppReserva adaptador = new NotificacionWhatsAppReserva(avisos);

    @Test
    void cadaOperacionDeLaReservaDisparaSuAviso() {
        adaptador.solicitar(1L, "turno.crear");
        adaptador.solicitar(2L, "turno.reprogramar");
        adaptador.solicitar(3L, "turno.cancelar");

        verify(avisos).creado(1L);
        verify(avisos).reprogramado(2L);
        verify(avisos).cancelado(3L);
    }

    @Test
    void confirmarNoGeneraAvisosNuevos() {
        adaptador.solicitar(1L, "turno.confirmar");

        verifyNoInteractions(avisos);
    }
}
