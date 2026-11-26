package com.turnosodontologia.application.notificaciones;

import com.turnosodontologia.application.AuditEventPublisher;
import com.turnosodontologia.application.CurrentActorProvider;
import com.turnosodontologia.domain.auditoria.AuditEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Programa confirmaciones y recordatorios por WhatsApp sobre el outbox transaccional.
 *
 * <p>Solo se encola un mensaje si existe una plantilla <b>aprobada</b> para el propósito
 * (RN-NT-01). Si la plantilla falta o fue rechazada por Meta, la notificación queda
 * {@code BLOQUEADA}, no se envía y se emite {@code notificacion.bloqueada} como aviso a
 * administración. La programación es idempotente por turno, propósito e instante.
 * La anticipación del recordatorio es {@code (TBD)}: la decide quien invoca.
 */
@Service
@Transactional
public class NotificacionService {

    private final NotificacionStore store;
    private final CurrentActorProvider actores;
    private final AuditEventPublisher auditoria;

    public NotificacionService(NotificacionStore store, CurrentActorProvider actores,
                               AuditEventPublisher auditoria) {
        this.store = store;
        this.actores = actores;
        this.auditoria = auditoria;
    }

    /**
     * Anula los avisos que todavía no salieron de un turno cancelado o reprogramado: quedan
     * {@code BLOQUEADA} con el motivo, salen del outbox y cada anulación se audita. Devuelve cuántas anuló.
     */
    public int anularPendientes(long turnoId, String motivo) {
        int anuladas = 0;
        for (Long id : store.pendientesDeTurno(turnoId)) {
            Notificacion notificacion = store.notificacion(id).orElseThrow();
            store.actualizar(id, EstadoNotificacion.BLOQUEADA, notificacion.intentos(), null, motivo);
            store.cerrarOutbox(id);
            auditoria.publish(AuditEvent.de("notificacion.anulada", "notificacion", id, actores.system(), motivo,
                    Map.of("proposito", notificacion.proposito().name(), "turnoId", turnoId),
                    UUID.randomUUID().toString()));
            anuladas++;
        }
        return anuladas;
    }

    public Notificacion programar(long turnoId, Proposito proposito, String destinatario,
                                  Instant programadaPara) {
        if (destinatario == null || destinatario.isBlank()) {
            throw new IllegalArgumentException("destinatario obligatorio");
        }
        if (proposito == null || programadaPara == null) {
            throw new IllegalArgumentException("propósito e instante obligatorios");
        }
        String clave = turnoId + ":" + proposito + ":" + programadaPara.toEpochMilli();
        Optional<Notificacion> existente = store.porIdempotencia(clave);
        if (existente.isPresent()) {
            return existente.get();
        }

        Optional<PlantillaWhatsApp> plantilla = store.plantillaPara(proposito);
        if (plantilla.isEmpty() || !plantilla.get().aprobada()) {
            String motivo = plantilla.isEmpty() ? "plantilla-inexistente" : "plantilla-no-aprobada";
            long id = store.crearNotificacion(turnoId, plantilla.map(PlantillaWhatsApp::id).orElse(null),
                    proposito, destinatario, EstadoNotificacion.BLOQUEADA, programadaPara, clave, motivo);
            auditoria.publish(AuditEvent.de("notificacion.bloqueada", "notificacion", id,
                    actores.system(), motivo, Map.of("proposito", proposito.name(), "turnoId", turnoId,
                            "aviso", "administracion"), UUID.randomUUID().toString()));
            return store.notificacion(id).orElseThrow();
        }

        long id = store.crearNotificacion(turnoId, plantilla.get().id(), proposito, destinatario,
                EstadoNotificacion.PENDIENTE, programadaPara, clave, null);
        store.encolar(id, programadaPara);
        auditoria.publish(AuditEvent.de("notificacion.programada", "notificacion", id,
                actores.system(), "", Map.of("proposito", proposito.name(), "turnoId", turnoId,
                        "plantilla", plantilla.get().codigoMeta()), UUID.randomUUID().toString()));
        return store.notificacion(id).orElseThrow();
    }
}
