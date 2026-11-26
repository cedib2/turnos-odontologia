package com.turnosodontologia.application;

import com.turnosodontologia.domain.agenda.Ocupacion;
import com.turnosodontologia.domain.agenda.Profesional;
import com.turnosodontologia.domain.agenda.Sillon;
import com.turnosodontologia.domain.agenda.Turno;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SobreturnoServiceTest {

    private static final Instant INICIO = Instant.parse("2026-12-24T13:00:00Z");
    private static final Instant DESTINO = Instant.parse("2026-12-24T13:30:00Z");
    private static final Instant FIN_DESTINO = Instant.parse("2026-12-24T14:00:00Z");

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

    private SobreturnoService service;

    @BeforeEach
    void setUp() {
        service = new SobreturnoService(turnos, ocupaciones, configuracion,
                actorProvider, publisher, com.turnosodontologia.application.notificaciones.AvisosTurno.NINGUNO);
        lenient().when(actorProvider.current()).thenReturn(
                com.turnosodontologia.domain.auditoria.ActorSimulado.recepcionista());
    }

    @Test
    void desplazaTurnoAlDestinoValidoConMotivoYActor() {
        Turno turno = turnoFixture();
        stubDestinoLibre(turno);
        when(ocupaciones.findActivosQueSolapan(2L, 3L, DESTINO, FIN_DESTINO))
                .thenReturn(List.of());

        Turno resultado = service.reprogramarConSobreturno(1L, DESTINO,
                "fixture-motivo", "recepcionista");

        assertTrue(resultado.isEsSobreturno());
        assertEquals("fixture-motivo", resultado.getMotivoOverride());
        assertEquals(DESTINO, resultado.getInicio());
        assertEquals(FIN_DESTINO, resultado.getFin());
        verify(ocupaciones, times(2)).saveAndFlush(any(Ocupacion.class));
        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(publisher).publish(captor.capture());
        assertEquals("turno.sobreturno", captor.getValue().tipo());
        assertEquals("fixture-motivo", captor.getValue().motivo());
        assertEquals("recepcionista", captor.getValue().actor().nombre());
        assertEquals("SIMULADO", captor.getValue().actor().modo());
    }

    @Test
    void rechazaSobreturnoSinMotivoOSinActor() {
        assertThrows(IllegalArgumentException.class, () ->
                service.reprogramarConSobreturno(1L, DESTINO, "  ", "recepcionista"));
        assertThrows(IllegalArgumentException.class, () ->
                service.reprogramarConSobreturno(1L, DESTINO, "fixture-motivo", ""));

        verify(turnos, never()).saveAndFlush(any());
        verify(publisher, never()).publish(any());
    }

    @Test
    void rechazaDestinoOcupadoSinModificarElTurno() {
        Turno turno = turnoFixture();
        stubDestinoLibre(turno);
        Ocupacion ajena = Ocupacion.paraTurno(99L, 2L, 3L, DESTINO, FIN_DESTINO);
        when(ocupaciones.findActivosQueSolapan(2L, 3L, DESTINO, FIN_DESTINO))
                .thenReturn(List.of(ajena));

        assertThrows(IllegalArgumentException.class, () ->
                service.reprogramarConSobreturno(1L, DESTINO, "fixture-motivo", "recepcionista"));

        assertEquals(INICIO, turno.getInicio());
        verify(turnos, never()).saveAndFlush(any());
        verify(publisher, never()).publish(any());
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

    private void stubDestinoLibre(Turno turno) {
        when(turnos.findById(1L)).thenReturn(Optional.of(turno));
        when(configuracion.zonaHoraria()).thenReturn(ZoneOffset.UTC);
        when(configuracion.esFeriado(LocalDate.of(2026, 12, 24))).thenReturn(false);
        when(configuracion.estaDentroDeHorario(2, DESTINO, FIN_DESTINO)).thenReturn(true);
        lenient().when(ocupaciones.findByTurnoIdAndActivoTrue(1L)).thenReturn(Optional.of(
                Ocupacion.paraTurno(1L, 2L, 3L, INICIO, INICIO.plusSeconds(1800))));
        lenient().when(turnos.saveAndFlush(turno)).thenReturn(turno);
    }
}
