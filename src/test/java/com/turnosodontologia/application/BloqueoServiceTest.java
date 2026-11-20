package com.turnosodontologia.application;

import com.turnosodontologia.domain.agenda.Bloqueo;
import com.turnosodontologia.domain.agenda.Ocupacion;
import com.turnosodontologia.domain.auditoria.AuditEvent;
import com.turnosodontologia.infrastructure.persistence.BloqueoRepository;
import com.turnosodontologia.infrastructure.persistence.OcupacionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BloqueoServiceTest {

    private static final Instant DESDE = Instant.parse("2026-12-20T13:00:00Z");
    private static final Instant HASTA = Instant.parse("2026-12-20T14:00:00Z");

    @Mock
    private BloqueoRepository bloqueos;

    @Mock
    private OcupacionRepository ocupaciones;

    @Mock
    private AuditEventPublisher publisher;

    private BloqueoService service;

    @BeforeEach
    void setUp() {
        service = new BloqueoService(bloqueos, ocupaciones,
                new SimulatedCurrentActorProvider(), publisher);
    }

    @Test
    void creaBloqueoDeSillonConOcupacionYEvento() {
        when(bloqueos.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Bloqueo creado = service.crearBloqueo(null, 7L, DESDE, HASTA, "fixture-motivo");

        ArgumentCaptor<Bloqueo> captor = ArgumentCaptor.forClass(Bloqueo.class);
        verify(bloqueos).saveAndFlush(captor.capture());
        assertEquals(7L, captor.getValue().getSillonId());
        assertNull(captor.getValue().getProfesionalId());
        assertEquals("fixture-motivo", creado.getMotivo());

        ArgumentCaptor<Ocupacion> captorOcupacion = ArgumentCaptor.forClass(Ocupacion.class);
        verify(ocupaciones).saveAndFlush(captorOcupacion.capture());
        assertEquals(7L, captorOcupacion.getValue().getSillonId());
        assertNull(captorOcupacion.getValue().getProfesionalId());
        verify(publisher).publish(any(AuditEvent.class));
    }

    @Test
    void rechazaBloqueoSinRecursoYConMotivoVacioSinPersistir() {
        assertThrows(IllegalArgumentException.class, () ->
                service.crearBloqueo(null, null, DESDE, HASTA, "fixture-motivo"));
        assertThrows(IllegalArgumentException.class, () ->
                service.crearBloqueo(null, 7L, DESDE, HASTA, "   "));

        verifyNoInteractions(bloqueos, ocupaciones, publisher);
    }
}
