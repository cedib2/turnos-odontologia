package com.turnosodontologia.application;

import com.turnosodontologia.domain.agenda.Tratamiento;
import com.turnosodontologia.domain.agenda.Turno;
import com.turnosodontologia.domain.auditoria.ActorSimulado;
import com.turnosodontologia.domain.auditoria.AuditEvent;
import com.turnosodontologia.domain.financiero.ItemPresupuesto;
import com.turnosodontologia.domain.financiero.Presupuesto;
import com.turnosodontologia.domain.financiero.PresupuestoAuditMetadata;
import com.turnosodontologia.domain.identidad.Paciente;
import com.turnosodontologia.infrastructure.persistence.PacienteRepository;
import com.turnosodontologia.infrastructure.persistence.PresupuestoRepository;
import com.turnosodontologia.infrastructure.persistence.TratamientoRepository;
import com.turnosodontologia.infrastructure.persistence.TurnoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@Transactional
public class PresupuestoService {

    private final PresupuestoRepository presupuestos;
    private final PacienteRepository pacientes;
    private final TratamientoRepository tratamientos;
    private final TurnoRepository turnos;
    private final CurrentActorProvider actorProvider;
    private final AuditEventPublisher publisher;

    public PresupuestoService(PresupuestoRepository presupuestos, PacienteRepository pacientes,
                              TratamientoRepository tratamientos, TurnoRepository turnos,
                              CurrentActorProvider actorProvider, AuditEventPublisher publisher) {
        this.presupuestos = presupuestos;
        this.pacientes = pacientes;
        this.tratamientos = tratamientos;
        this.turnos = turnos;
        this.actorProvider = actorProvider;
        this.publisher = publisher;
    }

    public Presupuesto crear(Long pacienteId, Long turnoId, List<ItemPresupuestoSolicitud> items) {
        validarItems(items);
        if (pacienteId == null) {
            throw new IllegalArgumentException("El paciente es obligatorio");
        }
        Paciente paciente = pacientes.findById(pacienteId)
                .orElseThrow(() -> new IllegalArgumentException("Paciente inexistente: " + pacienteId));
        ActorSimulado actor = actorProvider.current();
        Presupuesto presupuesto = new Presupuesto(paciente, resolverTurno(turnoId));
        agregarItems(presupuesto, items, actor.nombre());
        presupuesto.registrarCreacion(actor.nombre());
        Presupuesto guardado = presupuestos.saveAndFlush(presupuesto);
        publicar(guardado, "presupuesto.creado", "", actor);
        return guardado;
    }

    public Presupuesto modificar(Long id, Long turnoId, List<ItemPresupuestoSolicitud> items) {
        validarItems(items);
        Presupuesto presupuesto = presupuestos.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Presupuesto inexistente: " + id));
        ActorSimulado actor = actorProvider.current();
        presupuesto.asignarTurno(resolverTurno(turnoId));
        presupuesto.limpiarItems();
        agregarItems(presupuesto, items, actor.nombre());
        presupuesto.registrarActualizacion(actor.nombre());
        Presupuesto guardado = presupuestos.saveAndFlush(presupuesto);
        publicar(guardado, "presupuesto.modificado", "modificacion", actor);
        return guardado;
    }

    public Presupuesto anular(Long id) {
        Presupuesto presupuesto = presupuestos.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Presupuesto inexistente: " + id));
        ActorSimulado actor = actorProvider.current();
        presupuesto.anular(actor.nombre());
        Presupuesto guardado = presupuestos.saveAndFlush(presupuesto);
        publicar(guardado, "presupuesto.modificado", "anulado", actor);
        return guardado;
    }

    @Transactional(readOnly = true)
    public Presupuesto obtener(Long id) {
        Presupuesto presupuesto = presupuestos.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Presupuesto inexistente: " + id));
        presupuesto.getItems().size();
        return presupuesto;
    }

    @Transactional(readOnly = true)
    public List<Presupuesto> listar() {
        List<Presupuesto> resultado = presupuestos.findAllByOrderByIdAsc();
        resultado.forEach(presupuesto -> presupuesto.getItems().size());
        return resultado;
    }

    private void agregarItems(Presupuesto presupuesto, List<ItemPresupuestoSolicitud> items, String autor) {
        for (ItemPresupuestoSolicitud item : items) {
            if (item.tratamientoId() == null) {
                throw new IllegalArgumentException("El tratamiento del ítem es obligatorio");
            }
            Tratamiento tratamiento = tratamientos.findById(item.tratamientoId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Tratamiento inexistente: " + item.tratamientoId()));
            presupuesto.agregarItem(tratamiento, item.cantidad(), item.precio(), item.descripcion());
        }
        for (ItemPresupuesto item : presupuesto.getItems()) {
            if (item.getCreatedBy() == null) {
                item.registrarCreacion(autor);
            } else {
                item.registrarActualizacion(autor);
            }
        }
    }

    private Turno resolverTurno(Long turnoId) {
        if (turnoId == null) {
            return null;
        }
        return turnos.findById(turnoId)
                .orElseThrow(() -> new IllegalArgumentException("Turno inexistente: " + turnoId));
    }

    private void validarItems(List<ItemPresupuestoSolicitud> items) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("El presupuesto debe tener al menos un ítem");
        }
        if (items.stream().anyMatch(java.util.Objects::isNull)) {
            throw new IllegalArgumentException("Los ítems no pueden ser nulos");
        }
    }

    private void publicar(Presupuesto presupuesto, String tipo, String motivo, ActorSimulado actor) {
        Long turnoId = presupuesto.getTurno() == null ? null : presupuesto.getTurno().getId();
        AuditEvent evento = AuditEvent.de(tipo, "presupuesto", presupuesto.getId(), actor, motivo,
                PresupuestoAuditMetadata.de(presupuesto.getId(), turnoId, presupuesto.getTotal(),
                        presupuesto.getItems().size(), motivo),
                UUID.randomUUID().toString());
        publisher.publish(evento);
    }
}
