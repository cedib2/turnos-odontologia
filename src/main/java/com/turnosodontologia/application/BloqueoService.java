package com.turnosodontologia.application;

import com.turnosodontologia.domain.agenda.Bloqueo;
import com.turnosodontologia.domain.agenda.Ocupacion;
import com.turnosodontologia.domain.auditoria.ActorSimulado;
import com.turnosodontologia.domain.auditoria.AuditEvent;
import com.turnosodontologia.infrastructure.persistence.BloqueoRepository;
import com.turnosodontologia.infrastructure.persistence.OcupacionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class BloqueoService {

    private final BloqueoRepository bloqueos;
    private final OcupacionRepository ocupaciones;
    private final CurrentActorProvider actorProvider;
    private final AuditEventPublisher publisher;

    public BloqueoService(BloqueoRepository bloqueos,
                          OcupacionRepository ocupaciones,
                          CurrentActorProvider actorProvider,
                          AuditEventPublisher publisher) {
        this.bloqueos = bloqueos;
        this.ocupaciones = ocupaciones;
        this.actorProvider = actorProvider;
        this.publisher = publisher;
    }

    public Bloqueo crearBloqueo(Long profesionalId, Long sillonId, Instant desde, Instant hasta,
                                String motivo) {
        if (profesionalId == null && sillonId == null) {
            throw new IllegalArgumentException("El bloqueo requiere profesional o sillón");
        }
        if (motivo == null || motivo.isBlank()) {
            throw new IllegalArgumentException("El motivo del bloqueo no puede estar vacío");
        }
        if (desde == null || hasta == null || !hasta.isAfter(desde)) {
            throw new IllegalArgumentException("El bloqueo debe tener un rango válido");
        }

        ActorSimulado actor = actorProvider.current();
        Bloqueo bloqueo = new Bloqueo(profesionalId, sillonId, desde, hasta, motivo);
        bloqueo.registrarCreacion(actor.nombre());
        Bloqueo guardado = bloqueos.saveAndFlush(bloqueo);

        ocupaciones.saveAndFlush(Ocupacion.paraBloqueo(guardado.getId(),
                profesionalId, sillonId, desde, hasta));

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("profesionalId", profesionalId);
        metadata.put("sillonId", sillonId);
        publicarDespuesDeConfirmar(AuditEvent.de("bloqueo.creado", "bloqueo", guardado.getId(),
                actor, motivo, metadata, UUID.randomUUID().toString()));
        return guardado;
    }

    private void publicarDespuesDeConfirmar(AuditEvent evento) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publisher.publish(evento);
                }
            });
        } else {
            publisher.publish(evento);
        }
    }
}
