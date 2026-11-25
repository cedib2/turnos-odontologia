package com.turnosodontologia.application;

import com.turnosodontologia.domain.agenda.Consultorio;
import com.turnosodontologia.domain.agenda.Profesional;
import com.turnosodontologia.domain.agenda.Sillon;
import com.turnosodontologia.domain.agenda.Tratamiento;
import com.turnosodontologia.domain.agenda.Turno;
import com.turnosodontologia.domain.auditoria.AuditEvent;
import com.turnosodontologia.domain.financiero.Presupuesto;
import com.turnosodontologia.domain.financiero.PresupuestoNoModificableException;
import com.turnosodontologia.domain.identidad.DocumentoTipo;
import com.turnosodontologia.domain.identidad.Paciente;
import com.turnosodontologia.infrastructure.audit.InMemoryAuditEventRecorder;
import com.turnosodontologia.infrastructure.persistence.ConsultorioRepository;
import com.turnosodontologia.infrastructure.persistence.PacienteRepository;
import com.turnosodontologia.infrastructure.persistence.ProfesionalRepository;
import com.turnosodontologia.infrastructure.persistence.SillonRepository;
import com.turnosodontologia.infrastructure.persistence.TratamientoRepository;
import com.turnosodontologia.infrastructure.persistence.TurnoRepository;
import com.turnosodontologia.support.PostgresFlywayTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PresupuestoServiceIntegrationTest extends PostgresFlywayTest {

    @Autowired
    private PresupuestoService servicio;

    @Autowired
    private PacienteRepository pacientes;

    @Autowired
    private TratamientoRepository tratamientos;

    @Autowired
    private ProfesionalRepository profesionales;

    @Autowired
    private SillonRepository sillones;

    @Autowired
    private ConsultorioRepository consultorios;

    @Autowired
    private TurnoRepository turnos;

    @Autowired
    private InMemoryAuditEventRecorder eventos;

    private static final Instant INICIO = Instant.parse("2027-01-10T10:00:00Z");

    private Long pacienteId;
    private Long tratamientoId;
    private Long turnoId;

    @BeforeEach
    void datos() {
        eventos.clear();
        Paciente paciente = pacientes.saveAndFlush(new Paciente(DocumentoTipo.DNI, "PRES-SVC-1",
                "Servicio", "Presupuesto", "555"));
        Tratamiento tratamiento = tratamientos.saveAndFlush(
                new Tratamiento("PRES-SVC", "Tratamiento", 30));
        Profesional profesional = profesionales.saveAndFlush(
                new Profesional("MP-PRES-SVC", "Profesional", "Fixture"));
        Sillon sillon = sillones.saveAndFlush(new Sillon("S-PRES-SVC",
                consultorios.findById(1L).orElseThrow()));
        Turno turno = new Turno(paciente, profesional, sillon, tratamiento,
                INICIO, INICIO.plusSeconds(1800), 30, "confirmado", "recepcion", null);
        turno.registrarCreacion("recepcionista");
        turnoId = turnos.saveAndFlush(turno).getId();
        pacienteId = paciente.getId();
        tratamientoId = tratamiento.getId();
    }

    @Test
    void creaConItemsYPublicaEventoDeCreacion() {
        Presupuesto presupuesto = servicio.crear(pacienteId, null, List.of(item(2, "10.00")));

        assertNotNull(presupuesto.getId());
        assertEquals(0, new BigDecimal("20.00").compareTo(presupuesto.getTotal()));
        assertEquals("ARS", presupuesto.getMoneda());
        assertEquals("emitido", presupuesto.getEstado());
        assertNull(presupuesto.getTurno());
        assertEquals(1, presupuesto.getItems().size());
        assertEquals("recepcionista", presupuesto.getCreatedBy());
        assertEquals("recepcionista", presupuesto.getItems().get(0).getCreatedBy());

        assertEquals(1, eventos.eventos().size());
        AuditEvent evento = eventos.eventos().get(0);
        assertEquals("presupuesto.creado", evento.tipo());
        assertEquals("presupuesto", evento.entidad());
        assertEquals(presupuesto.getId(), evento.entidadId());
        assertEquals("recepcionista", evento.actor().nombre());
        assertEquals("SIMULADO", evento.actor().modo());
        assertEquals(presupuesto.getId(), evento.metadata().get("presupuestoId"));
        assertEquals("20.00", evento.metadata().get("total"));
    }

    @Test
    void modificaItemsYPublicaEventoDeModificacion() {
        Presupuesto presupuesto = servicio.crear(pacienteId, null, List.of(item(1, "10.00")));
        eventos.clear();

        Presupuesto modificado = servicio.modificar(presupuesto.getId(), null,
                List.of(item(2, "7.50"), item(1, "5.00")));

        assertEquals(0, new BigDecimal("20.00").compareTo(modificado.getTotal()));
        assertEquals(2, modificado.getItems().size());
        assertEquals("recepcionista", modificado.getUpdatedBy());
        assertEquals(1, eventos.eventos().size());
        assertEquals("presupuesto.modificado", eventos.eventos().get(0).tipo());
        assertEquals("modificacion", eventos.eventos().get(0).motivo());
        assertEquals(2, eventos.eventos().get(0).metadata().get("cantidadItems"));
    }

    @Test
    void anulaDesactivaYPublicaModificacion() {
        Presupuesto presupuesto = servicio.crear(pacienteId, null, List.of(item(1, "10.00")));
        eventos.clear();

        Presupuesto anulado = servicio.anular(presupuesto.getId());

        assertEquals("anulado", anulado.getEstado());
        assertFalse(anulado.isActivo());
        assertEquals(1, eventos.eventos().size());
        AuditEvent evento = eventos.eventos().get(0);
        assertEquals("presupuesto.modificado", evento.tipo());
        assertEquals("anulado", evento.motivo());
        assertEquals("anulado", evento.metadata().get("motivo"));

        assertThrows(PresupuestoNoModificableException.class,
                () -> servicio.modificar(presupuesto.getId(), null, List.of(item(1, "1.00"))));
    }

    @Test
    void asociaTurnoOpcionalYRechazaInexistentes() {
        Presupuesto presupuesto = servicio.crear(pacienteId, turnoId, List.of(item(1, "10.00")));
        assertNotNull(presupuesto.getTurno());
        assertEquals(turnoId, presupuesto.getTurno().getId());
        assertEquals(turnoId, eventos.eventos().get(0).metadata().get("turnoId"));

        assertThrows(IllegalArgumentException.class,
                () -> servicio.crear(pacienteId, 123456L, List.of(item(1, "10.00"))));
        assertThrows(IllegalArgumentException.class,
                () -> servicio.crear(123456L, null, List.of(item(1, "10.00"))));
        assertThrows(IllegalArgumentException.class,
                () -> servicio.crear(pacienteId, null, List.of()));
    }

    @Test
    void permiteVariosPresupuestosPorTurnoSinAlterarElTurno() {
        Presupuesto uno = servicio.crear(pacienteId, turnoId, List.of(item(1, "10.00")));
        Presupuesto dos = servicio.crear(pacienteId, turnoId, List.of(item(1, "20.00")));

        Turno turno = turnos.findById(turnoId).orElseThrow();
        assertEquals("confirmado", turno.getEstado());
        assertTrue(turno.isActivo());
        assertNotNull(uno.getId());
        assertNotNull(dos.getId());
        assertEquals(2, servicio.listar().stream()
                .filter(presupuesto -> presupuesto.getTurno() != null
                        && turnoId.equals(presupuesto.getTurno().getId()))
                .count());
    }

    @Test
    void rechazaTratamientosInexistentesEnItems() {
        ItemPresupuestoSolicitud inexistente = new ItemPresupuestoSolicitud(123456L, 1,
                BigDecimal.ONE, "Inexistente");
        assertThrows(IllegalArgumentException.class,
                () -> servicio.crear(pacienteId, null, List.of(inexistente)));
    }

    @Test
    void obtieneYListaPresupuestos() {
        Presupuesto uno = servicio.crear(pacienteId, null, List.of(item(1, "10.00")));
        Presupuesto dos = servicio.crear(pacienteId, null, List.of(item(1, "20.00")));

        assertEquals(uno.getId(), servicio.obtener(uno.getId()).getId());
        assertEquals(2, servicio.listar().size());
        assertThrows(NoSuchElementException.class, () -> servicio.obtener(123456L));
        assertTrue(servicio.listar().stream().anyMatch(p -> dos.getId().equals(p.getId())));
    }

    private ItemPresupuestoSolicitud item(int cantidad, String precio) {
        return new ItemPresupuestoSolicitud(tratamientoId, cantidad, new BigDecimal(precio),
                "Ítem de prueba");
    }
}
