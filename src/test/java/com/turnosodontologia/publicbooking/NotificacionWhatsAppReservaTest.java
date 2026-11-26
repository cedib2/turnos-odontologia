package com.turnosodontologia.publicbooking;

import com.turnosodontologia.application.notificaciones.NotificacionService;
import com.turnosodontologia.application.notificaciones.NotificacionStore;
import com.turnosodontologia.application.notificaciones.Proposito;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class NotificacionWhatsAppReservaTest {

    private static final Instant AHORA = Instant.parse("2026-11-26T12:00:00Z");
    private static final String TELEFONO = "+5491150000001";

    private NotificacionService notificaciones;
    private NotificacionStore store;
    private final Clock reloj = Clock.fixed(AHORA, ZoneOffset.UTC);

    @BeforeEach
    void preparar() {
        notificaciones = mock(NotificacionService.class);
        store = mock(NotificacionStore.class);
    }

    private NotificacionWhatsAppReserva adaptador(Duration anticipacion) {
        return new NotificacionWhatsAppReserva(notificaciones, store, reloj, anticipacion);
    }

    private void turnoQueEmpiezaEn(Duration desdeAhora) {
        when(store.datosTurno(7L)).thenReturn(Optional.of(
                new NotificacionStore.DatosTurno(TELEFONO, AHORA.plus(desdeAhora), true)));
    }

    @Test
    void crearProgramaConfirmacionInmediataYRecordatorioConAnticipacion() {
        turnoQueEmpiezaEn(Duration.ofDays(3));

        adaptador(Duration.ofHours(24)).solicitar(7L, "turno.crear");

        verify(notificaciones).programar(7L, Proposito.CONFIRMACION, TELEFONO, AHORA);
        verify(notificaciones).programar(7L, Proposito.RECORDATORIO, TELEFONO,
                AHORA.plus(Duration.ofDays(3)).minus(Duration.ofHours(24)));
    }

    @Test
    void turnoMasCercanoQueLaAnticipacionSoloRecibeConfirmacion() {
        turnoQueEmpiezaEn(Duration.ofHours(5));

        adaptador(Duration.ofHours(24)).solicitar(7L, "turno.crear");

        verify(notificaciones).programar(7L, Proposito.CONFIRMACION, TELEFONO, AHORA);
        verify(notificaciones, never()).programar(anyLong(), eq(Proposito.RECORDATORIO), anyString(), any());
    }

    @Test
    void sinAnticipacionConfiguradaNoSeInventaRecordatorio() {
        turnoQueEmpiezaEn(Duration.ofDays(3));

        adaptador(null).solicitar(7L, "turno.crear");

        verify(notificaciones).programar(7L, Proposito.CONFIRMACION, TELEFONO, AHORA);
        verify(notificaciones, never()).programar(anyLong(), eq(Proposito.RECORDATORIO), anyString(), any());
    }

    @Test
    void cancelarAnulaLosAvisosPendientesSinProgramarNuevos() {
        adaptador(Duration.ofHours(24)).solicitar(7L, "turno.cancelar");

        verify(notificaciones).anularPendientes(7L, "turno-cancelado");
        verify(notificaciones, never()).programar(anyLong(), any(), anyString(), any());
    }

    @Test
    void reprogramarAnulaLoAnteriorYProgramaElNuevoHorario() {
        turnoQueEmpiezaEn(Duration.ofDays(5));

        adaptador(Duration.ofHours(24)).solicitar(7L, "turno.reprogramar");

        InOrder orden = inOrder(notificaciones);
        orden.verify(notificaciones).anularPendientes(7L, "turno-reprogramado");
        orden.verify(notificaciones).programar(7L, Proposito.CONFIRMACION, TELEFONO, AHORA);
        orden.verify(notificaciones).programar(7L, Proposito.RECORDATORIO, TELEFONO,
                AHORA.plus(Duration.ofDays(5)).minus(Duration.ofHours(24)));
    }

    @Test
    void confirmarNoGeneraAvisosNuevos() {
        adaptador(Duration.ofHours(24)).solicitar(7L, "turno.confirmar");

        verifyNoInteractions(notificaciones);
    }
}
