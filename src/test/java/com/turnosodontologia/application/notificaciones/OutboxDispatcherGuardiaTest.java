package com.turnosodontologia.application.notificaciones;

import com.turnosodontologia.application.AuditEventPublisher;
import com.turnosodontologia.application.CurrentActorProvider;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OutboxDispatcherGuardiaTest {

    private static final Instant AHORA = Instant.parse("2026-11-26T12:00:00Z");

    private final NotificacionStore store = mock(NotificacionStore.class);
    private final WhatsAppGateway gateway = mock(WhatsAppGateway.class);
    private final OutboxDispatcher dispatcher = new OutboxDispatcher(store, gateway,
            mock(CurrentActorProvider.class), mock(AuditEventPublisher.class), 3, 300);

    private void pendiente(boolean turnoActivo) {
        when(store.tomarPendientes(AHORA, 10)).thenReturn(List.of(1L));
        when(store.notificacion(1L)).thenReturn(Optional.of(new Notificacion(1L, 7L, 2L, Proposito.RECORDATORIO,
                "+5491150000001", EstadoNotificacion.PENDIENTE, 0, null, AHORA, null)));
        when(store.datosTurno(7L)).thenReturn(Optional.of(
                new NotificacionStore.DatosTurno("+5491150000001", AHORA.plusSeconds(86_400), turnoActivo)));
        when(store.plantilla(2L)).thenReturn(Optional.of(new PlantillaWhatsApp(2L, "turno_recordatorio_v1",
                Proposito.RECORDATORIO, "es_AR", "APROBADA", null, true)));
        when(store.inicioTurno(7L)).thenReturn(AHORA.plusSeconds(86_400));
    }

    @Test
    void turnoCanceladoNoRecibeElAvisoYQuedaBloqueado() {
        pendiente(false);

        assertEquals(1, dispatcher.procesar(AHORA, 10));

        verify(gateway, never()).enviar(any());
        verify(store).registrarIntento(1L, 1, "BLOQUEADO", null, "turno-inactivo");
        verify(store).actualizar(1L, EstadoNotificacion.BLOQUEADA, 1, null, "turno-inactivo");
        verify(store).cerrarOutbox(1L);
    }

    @Test
    void turnoActivoSeEnviaPorElGateway() {
        pendiente(true);
        when(gateway.enviar(any())).thenReturn(ResultadoEnvio.exito("wamid.test-1"));

        dispatcher.procesar(AHORA, 10);

        verify(gateway).enviar(new EnvioWhatsApp(1L, "+5491150000001", "turno_recordatorio_v1", "es_AR",
                Map.of("turno_inicio", "2026-11-27T09:00")));
        verify(store).actualizar(1L, EstadoNotificacion.ENVIADA, 1, "wamid.test-1", null);
    }
}
