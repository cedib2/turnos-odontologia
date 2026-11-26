package com.turnosodontologia.application;

import com.turnosodontologia.domain.agenda.Ocupacion;
import com.turnosodontologia.domain.agenda.Profesional;
import com.turnosodontologia.domain.agenda.Sillon;
import com.turnosodontologia.domain.agenda.Turno;
import com.turnosodontologia.domain.auditoria.ActorSimulado;
import com.turnosodontologia.domain.auditoria.AuditEvent;
import com.turnosodontologia.infrastructure.persistence.OcupacionRepository;
import com.turnosodontologia.infrastructure.persistence.TurnoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReprogramacionServiceTest {

    private static final Instant INICIO = Instant.parse("2026-12-25T13:00:00Z");
    private static final Instant DESTINO = Instant.parse("2026-12-25T13:30:00Z");
    private static final Instant FIN_DESTINO = Instant.parse("2026-12-25T14:00:00Z");

    @Mock
    private TurnoRepository turnos;

    @Mock
    private OcupacionRepository ocupaciones;

    @Mock
    private ConfiguracionSchedulingService configuracion;

    @Mock
    private CurrentActorProvider actorProvider;

    @Mock
    private AuditEventPublisher publisher;

    private ReprogramacionService service;

    @BeforeEach
    void setUp() {
        service = new ReprogramacionService(turnos, ocupaciones, configuracion,
                actorProvider, publisher, com.turnosodontologia.application.notificaciones.AvisosTurno.NINGUNO);
        lenient().when(actorProvider.current()).thenReturn(ActorSimulado.recepcionista());
    }

    @Test
    void reprogramaEnUnaTransaccionYEmiteEventoSinSobreturno() {
        Turno turno = turnoFixture();
        stubContexto(turno);
        when(ocupaciones.findActivosQueSolapan(2L, 3L, DESTINO, FIN_DESTINO))
                .thenReturn(List.of());

        Turno resultado = service.reprogramar(1L, DESTINO, "recepcionista");

        assertEquals(DESTINO, resultado.getInicio());
        assertEquals(FIN_DESTINO, resultado.getFin());
        assertFalse(resultado.isEsSobreturno());
        verify(ocupaciones, times(2)).saveAndFlush(any(Ocupacion.class));
        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(publisher).publish(captor.capture());
        assertEquals("turno.reprogramado", captor.getValue().tipo());
    }

    @Test
    void destinoOcupadoNoModificaElTurnoOriginal() {
        Turno turno = turnoFixture();
        stubContexto(turno);
        when(ocupaciones.findActivosQueSolapan(2L, 3L, DESTINO, FIN_DESTINO))
                .thenReturn(List.of(Ocupacion.paraTurno(99L, 2L, 3L, DESTINO, FIN_DESTINO)));

        assertThrows(IllegalArgumentException.class, () ->
                service.reprogramar(1L, DESTINO, "recepcionista"));

        assertEquals(INICIO, turno.getInicio());
        verify(turnos, never()).saveAndFlush(any());
        verify(publisher, never()).publish(any());
    }

    @Test
    void rechazaActorVacio() {
        assertThrows(IllegalArgumentException.class, () ->
                service.reprogramar(1L, DESTINO, ""));
        verify(turnos, never()).findById(any());
    }

    private Turno turnoFixture() {
        Profesional profesional = org.mockito.Mockito.mock(Profesional.class);
        when(profesional.getId()).thenReturn(2L);
        Sillon sillon = org.mockito.Mockito.mock(Sillon.class);
        when(sillon.getId()).thenReturn(3L);
        Turno turno = new Turno(null, profesional, sillon, null, INICIO,
                INICIO.plusSeconds(1800), 30, "fixture-estado", "fixture-origen", null);
        ReflectionTestUtils.setField(turno, "id", 1L);
        return turno;
    }

    private void stubContexto(Turno turno) {
        when(turnos.findById(1L)).thenReturn(Optional.of(turno));
        when(configuracion.zonaHoraria()).thenReturn(ZoneOffset.UTC);
        when(configuracion.esFeriado(LocalDate.of(2026, 12, 25))).thenReturn(false);
        when(configuracion.estaDentroDeHorario(2, DESTINO, FIN_DESTINO)).thenReturn(true);
        lenient().when(ocupaciones.findByTurnoIdAndActivoTrue(1L)).thenReturn(Optional.of(
                Ocupacion.paraTurno(1L, 2L, 3L, INICIO, INICIO.plusSeconds(1800))));
        lenient().when(turnos.saveAndFlush(turno)).thenReturn(turno);
    }
}
