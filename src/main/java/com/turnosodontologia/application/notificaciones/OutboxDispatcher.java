package com.turnosodontologia.application.notificaciones;

import com.turnosodontologia.application.AuditEventPublisher;
import com.turnosodontologia.application.CurrentActorProvider;
import com.turnosodontologia.domain.auditoria.AuditEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;
import java.util.UUID;

/**
 * Despacha el outbox de notificaciones (RN-NT-03): cada intento queda registrado con su número,
 * resultado y {@code proveedor_message_id}; los errores se reintentan con espera lineal hasta el
 * máximo configurado y luego la notificación pasa a {@code FALLIDA}. Antes de enviar se vuelve a
 * comprobar la aprobación de la plantilla y que el turno siga activo. Lo invoca periódicamente
 * {@link OutboxDispatcherProgramado} cuando {@code notificaciones.dispatcher.habilitado=true}.
 */
@Service
@Transactional
public class OutboxDispatcher {

    private static final ZoneId ZONA = ZoneId.of("America/Argentina/Buenos_Aires");

    private final NotificacionStore store;
    private final WhatsAppGateway gateway;
    private final CurrentActorProvider actores;
    private final AuditEventPublisher auditoria;
    private final int maxIntentos;
    private final Duration espera;

    public OutboxDispatcher(NotificacionStore store, WhatsAppGateway gateway, CurrentActorProvider actores,
                            AuditEventPublisher auditoria,
                            @Value("${notificaciones.max-intentos:3}") int maxIntentos,
                            @Value("${notificaciones.reintento-segundos:300}") long reintentoSegundos) {
        if (maxIntentos < 1 || reintentoSegundos < 1) {
            throw new IllegalArgumentException("Parámetros de reintento inválidos");
        }
        this.store = store;
        this.gateway = gateway;
        this.actores = actores;
        this.auditoria = auditoria;
        this.maxIntentos = maxIntentos;
        this.espera = Duration.ofSeconds(reintentoSegundos);
    }

    public int procesar(Instant ahora, int lote) {
        int procesadas = 0;
        for (Long id : store.tomarPendientes(ahora, lote)) {
            despachar(store.notificacion(id).orElseThrow(), ahora);
            procesadas++;
        }
        return procesadas;
    }

    private void despachar(Notificacion notificacion, Instant ahora) {
        if (notificacion.estado() != EstadoNotificacion.PENDIENTE) {
            store.cerrarOutbox(notificacion.id());
            return;
        }
        int numero = notificacion.intentos() + 1;
        // Guardia: un turno cancelado no recibe avisos aunque su notificación siga en el outbox.
        boolean turnoActivo = store.datosTurno(notificacion.turnoId())
                .map(NotificacionStore.DatosTurno::activo).orElse(false);
        if (!turnoActivo) {
            store.registrarIntento(notificacion.id(), numero, "BLOQUEADO", null, "turno-inactivo");
            store.actualizar(notificacion.id(), EstadoNotificacion.BLOQUEADA, numero, null, "turno-inactivo");
            store.cerrarOutbox(notificacion.id());
            publicar("notificacion.bloqueada", notificacion, "turno-inactivo", Map.of());
            return;
        }
        PlantillaWhatsApp plantilla = store.plantilla(notificacion.plantillaId()).orElse(null);
        if (plantilla == null || !plantilla.aprobada()) {
            store.registrarIntento(notificacion.id(), numero, "BLOQUEADO", null, "plantilla-no-aprobada");
            store.actualizar(notificacion.id(), EstadoNotificacion.BLOQUEADA, numero, null,
                    "plantilla-no-aprobada");
            store.cerrarOutbox(notificacion.id());
            publicar("notificacion.bloqueada", notificacion, "plantilla-no-aprobada",
                    Map.of("aviso", "administracion"));
            return;
        }

        String inicio = store.inicioTurno(notificacion.turnoId()).atZone(ZONA).toLocalDateTime().toString();
        ResultadoEnvio resultado = gateway.enviar(new EnvioWhatsApp(notificacion.id(),
                notificacion.destinatario(), plantilla.codigoMeta(), plantilla.idioma(),
                Map.of("turno_inicio", inicio)));

        if (resultado.exitoso()) {
            store.registrarIntento(notificacion.id(), numero, "EXITO", resultado.proveedorMessageId(), null);
            store.actualizar(notificacion.id(), EstadoNotificacion.ENVIADA, numero,
                    resultado.proveedorMessageId(), null);
            store.cerrarOutbox(notificacion.id());
            publicar("notificacion.enviada", notificacion, "", Map.of("intento", numero));
            return;
        }

        String error = truncar(resultado.error());
        store.registrarIntento(notificacion.id(), numero, "ERROR", null, error);
        if (numero >= maxIntentos) {
            store.actualizar(notificacion.id(), EstadoNotificacion.FALLIDA, numero, null, error);
            store.cerrarOutbox(notificacion.id());
            publicar("notificacion.fallida", notificacion, error, Map.of("intentos", numero));
        } else {
            store.actualizar(notificacion.id(), EstadoNotificacion.PENDIENTE, numero, null, error);
            store.reprogramar(notificacion.id(), ahora.plus(espera.multipliedBy(numero)));
        }
    }

    private void publicar(String tipo, Notificacion notificacion, String motivo, Map<String, Object> extra) {
        Map<String, Object> metadata = new java.util.HashMap<>(extra);
        metadata.put("proposito", notificacion.proposito().name());
        metadata.put("turnoId", notificacion.turnoId());
        auditoria.publish(AuditEvent.de(tipo, "notificacion", notificacion.id(), actores.system(),
                motivo, metadata, UUID.randomUUID().toString()));
    }

    private static String truncar(String error) {
        String valor = error == null || error.isBlank() ? "error-desconocido" : error;
        return valor.length() > 255 ? valor.substring(0, 255) : valor;
    }
}
