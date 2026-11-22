package com.turnosodontologia.application.notificaciones;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.turnosodontologia.application.AuditEventPublisher;
import com.turnosodontologia.application.CurrentActorProvider;
import com.turnosodontologia.domain.auditoria.AuditEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Procesa webhooks de WhatsApp Business API (RN-NT-03, RN-GL-02).
 *
 * <ul>
 *   <li>Firma inválida o ausente → se descarta sin efectos y se audita el rechazo.</li>
 *   <li>Cada evento (estado o respuesta) se registra una sola vez: los reintentos del proveedor
 *       son idempotentes.</li>
 *   <li>Estados {@code delivered}/{@code read}/{@code failed} avanzan la notificación sin
 *       retroceder; la respuesta de botón a una confirmación la marca {@code CONFIRMADA}.</li>
 * </ul>
 *
 * <p>El cambio de {@code turnos.estado} por la confirmación queda {@code (TBD)}: los valores de
 * estado del turno no están definidos (RN-GL-03); se registra {@code notificacion.confirmada}.
 * La exposición HTTP del webhook queda {@code (TBD)} (ver decisión C-10).
 */
@Service
@Transactional(noRollbackFor = WebhookRechazadoException.class)
public class WhatsAppWebhookService {

    private final WebhookSignatureVerifier verificador;
    private final NotificacionStore store;
    private final CurrentActorProvider actores;
    private final AuditEventPublisher auditoria;
    private final ObjectMapper mapper = new ObjectMapper();

    public WhatsAppWebhookService(WebhookSignatureVerifier verificador, NotificacionStore store,
                                  CurrentActorProvider actores, AuditEventPublisher auditoria) {
        this.verificador = verificador;
        this.store = store;
        this.actores = actores;
        this.auditoria = auditoria;
    }

    /** Devuelve la cantidad de eventos nuevos aplicados. */
    public int recibir(String firma, byte[] cuerpo) {
        if (!verificador.esValida(firma, cuerpo)) {
            rechazar(firma == null ? "firma-ausente" : "firma-invalida");
        }
        JsonNode raiz;
        try {
            raiz = mapper.readTree(cuerpo);
        } catch (Exception e) {
            rechazar("cuerpo-invalido");
            return 0;
        }
        int aplicados = 0;
        for (JsonNode entrada : raiz.path("entry")) {
            for (JsonNode cambio : entrada.path("changes")) {
                JsonNode valor = cambio.path("value");
                for (JsonNode estado : valor.path("statuses")) {
                    aplicados += aplicarEstado(estado);
                }
                for (JsonNode mensaje : valor.path("messages")) {
                    aplicados += aplicarRespuesta(mensaje);
                }
            }
        }
        return aplicados;
    }

    private int aplicarEstado(JsonNode estado) {
        String messageId = estado.path("id").asText("");
        String valor = estado.path("status").asText("");
        if (messageId.isEmpty() || valor.isEmpty()
                || !store.registrarEventoWebhook("status:" + messageId + ":" + valor, "status", messageId)) {
            return 0;
        }
        Optional<Notificacion> notificacion = store.porProveedorMessageId(messageId);
        EstadoNotificacion nuevo = switch (valor) {
            case "delivered" -> EstadoNotificacion.ENTREGADA;
            case "read" -> EstadoNotificacion.LEIDA;
            case "failed" -> EstadoNotificacion.FALLIDA;
            default -> null;
        };
        if (notificacion.isEmpty() || nuevo == null || !avanza(notificacion.get().estado(), nuevo)) {
            return 1;
        }
        store.cambiarEstado(notificacion.get().id(), nuevo);
        if (nuevo == EstadoNotificacion.FALLIDA) {
            publicar("notificacion.fallida", notificacion.get(), "proveedor-informo-fallo");
        }
        return 1;
    }

    private int aplicarRespuesta(JsonNode mensaje) {
        String messageId = mensaje.path("id").asText("");
        String contexto = mensaje.path("context").path("id").asText("");
        if (messageId.isEmpty() || !"button".equals(mensaje.path("type").asText())
                || !store.registrarEventoWebhook("message:" + messageId, "button", contexto)) {
            return 0;
        }
        Optional<Notificacion> notificacion = contexto.isEmpty()
                ? Optional.empty() : store.porProveedorMessageId(contexto);
        if (notificacion.isEmpty() || notificacion.get().proposito() != Proposito.CONFIRMACION
                || notificacion.get().estado().esFinal()) {
            return 1;
        }
        store.cambiarEstado(notificacion.get().id(), EstadoNotificacion.CONFIRMADA);
        publicar("notificacion.confirmada", notificacion.get(), "respuesta-paciente");
        return 1;
    }

    private static boolean avanza(EstadoNotificacion actual, EstadoNotificacion nuevo) {
        if (actual.esFinal()) {
            return false;
        }
        if (nuevo == EstadoNotificacion.FALLIDA) {
            return true;
        }
        return nuevo.ordinal() > actual.ordinal();
    }

    private void rechazar(String motivo) {
        auditoria.publish(AuditEvent.de("whatsapp.webhook.rechazado", "webhook", null,
                actores.system(), motivo, Map.of("proveedor", "whatsapp"), UUID.randomUUID().toString()));
        throw new WebhookRechazadoException(motivo);
    }

    private void publicar(String tipo, Notificacion notificacion, String motivo) {
        auditoria.publish(AuditEvent.de(tipo, "notificacion", notificacion.id(), actores.system(), motivo,
                Map.of("proposito", notificacion.proposito().name(), "turnoId", notificacion.turnoId()),
                UUID.randomUUID().toString()));
    }
}
