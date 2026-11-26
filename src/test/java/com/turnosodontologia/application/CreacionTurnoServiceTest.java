package com.turnosodontologia.application;

import com.turnosodontologia.domain.agenda.Ocupacion;
import com.turnosodontologia.domain.agenda.Profesional;
import com.turnosodontologia.domain.agenda.Sillon;
import com.turnosodontologia.domain.agenda.Tratamiento;
import com.turnosodontologia.domain.agenda.Turno;
import com.turnosodontologia.domain.auditoria.AuditEvent;
import com.turnosodontologia.domain.identidad.DocumentoTipo;
import com.turnosodontologia.domain.identidad.Paciente;
import com.turnosodontologia.infrastructure.persistence.OcupacionRepository;
import com.turnosodontologia.infrastructure.persistence.PacienteRepository;
import com.turnosodontologia.infrastructure.persistence.ProfesionalRepository;
import com.turnosodontologia.infrastructure.persistence.SillonRepository;
import com.turnosodontologia.infrastructure.persistence.TratamientoRepository;
import com.turnosodontologia.infrastructure.persistence.TurnoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreacionTurnoServiceTest {

    private static final Instant INICIO = Instant.parse("2026-12-15T13:00:00Z");

    @Mock
    private PacienteRepository pacientes;

    @Mock
    private ProfesionalRepository profesionales;

    @Mock
    private SillonRepository sillones;

    @Mock
    private TratamientoRepository tratamientos;

    @Mock
    private TurnoRepository turnos;

    @Mock
    private OcupacionRepository ocupaciones;

    @Mock
    private AuditEventPublisher publisher;

    private CreacionTurnoService service;

    @BeforeEach
    void setUp() {
        ConfiguracionSchedulingService configuracion = new ConfiguracionSchedulingService(
                tratamientos, null, null);
        service = new CreacionTurnoService(pacientes, profesionales, sillones, tratamientos,
                turnos, ocupaciones, configuracion, new SimulatedCurrentActorProvider(), publisher,
                com.turnosodontologia.application.notificaciones.AvisosTurno.NINGUNO);
    }

    @Test
    void derivaDuracionDelCatalogoYCalculaFin() {
        stubContexto(new Tratamiento("LIMPIEZA", "Limpieza", 45));
        when(turnos.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Turno creado = service.crearTurno(1L, 2L, 3L, "LIMPIEZA", INICIO,
                "fixture-estado", "fixture-origen");

        ArgumentCaptor<Turno> captorTurno = ArgumentCaptor.forClass(Turno.class);
        verify(turnos).saveAndFlush(captorTurno.capture());
        Turno guardado = captorTurno.getValue();
        assertEquals(45, guardado.getDuracionMinutos());
        assertEquals(INICIO.plus(45, ChronoUnit.MINUTES), guardado.getFin());
        assertEquals(INICIO, creado.getInicio());

        ArgumentCaptor<Ocupacion> captorOcupacion = ArgumentCaptor.forClass(Ocupacion.class);
        verify(ocupaciones).saveAndFlush(captorOcupacion.capture());
        assertEquals(INICIO, captorOcupacion.getValue().getInicio());
        assertEquals(INICIO.plus(45, ChronoUnit.MINUTES), captorOcupacion.getValue().getFin());
        verify(publisher).publish(any(AuditEvent.class));
    }

    @Test
    void rechazaDuracionNoPositivaSinPersistir() {
        Tratamiento invalido = org.mockito.Mockito.mock(Tratamiento.class);
        when(invalido.getDuracionMinutos()).thenReturn(0);
        when(tratamientos.findByCodigo("CERO")).thenReturn(Optional.of(invalido));

        assertThrows(IllegalArgumentException.class, () -> service.crearTurno(1L, 2L, 3L,
                "CERO", INICIO, "fixture-estado", "fixture-origen"));

        verify(turnos, never()).saveAndFlush(any());
        verify(ocupaciones, never()).saveAndFlush(any());
        verify(publisher, never()).publish(any());
    }

    @Test
    void rechazaEstadoYOrigenVaciosSinPersistir() {
        assertThrows(IllegalArgumentException.class, () -> service.crearTurno(1L, 2L, 3L,
                "LIMPIEZA", INICIO, "  ", "fixture-origen"));
        assertThrows(IllegalArgumentException.class, () -> service.crearTurno(1L, 2L, 3L,
                "LIMPIEZA", INICIO, "fixture-estado", ""));

        verifyNoInteractions(turnos, ocupaciones, publisher);
    }

    private void stubContexto(Tratamiento tratamiento) {
        when(pacientes.findById(1L)).thenReturn(Optional.of(new Paciente(
                DocumentoTipo.DNI, "PAC-5", "Paciente", "Fixture", "555")));
        when(profesionales.findById(2L)).thenReturn(Optional.of(new Profesional(
                "MP-5", "Profesional", "Fixture")));
        when(sillones.findById(3L)).thenReturn(Optional.of(new Sillon("S-5", null)));
        when(tratamientos.findByCodigo(tratamiento.getCodigo()))
                .thenReturn(Optional.of(tratamiento));
    }
}
