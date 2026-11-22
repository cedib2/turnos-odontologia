# C-10 — `whatsapp-notifications-outbox`

- **Fecha**: 2026-11-22
- **Solicitante / autorización**: dueño de la rama `Parte-2-Donato` (Parte 2), pedido explícito de implementar C-10. Governance **ALTO**: esta autorización reemplaza la revisión previa; se recomienda revisión posterior del PO.
- **Migración**: `V010__whatsapp_notifications_outbox.sql` (primer número del rango de la Parte 2).
- **Spec**: `openspec/specs/whatsapp-notifications/spec.md`.

## Qué se implementó (`application/notificaciones`)

| Pieza | Responsabilidad |
|---|---|
| V010 | `plantillas_whatsapp`, `notificaciones` (estado, intentos, `proveedor_message_id` único, clave idempotente), `notificaciones_outbox`, `notificacion_intentos` y `whatsapp_webhook_eventos` (ambas append-only por trigger). Sin seed: códigos `(TBD)`. |
| `NotificacionService` | Programa confirmación/recordatorio. Sin plantilla aprobada → `BLOQUEADA` + `notificacion.bloqueada` (aviso a administración). Idempotente. |
| `OutboxDispatcher` | Toma pendientes con `FOR UPDATE SKIP LOCKED`, revalida la plantilla, llama al puerto, registra cada intento y reintenta con espera lineal hasta el máximo → `FALLIDA`. |
| `WhatsAppWebhookService` | Firma HMAC-SHA256 obligatoria, dedupe por evento, estados que no retroceden, confirmación por respuesta de botón. El rechazo se audita y no se revierte (`noRollbackFor`). |
| `WhatsAppGateway` | Puerto. `SimulatedWhatsAppGateway` (dev/test) y `WhatsAppNoConfiguradoGateway` (resto: falla cerrado, sin llamadas externas). |
| Auditoría | Seis tipos nuevos en `AuditoriaCatalogo`: `notificacion.{programada,enviada,bloqueada,fallida,confirmada}` y `whatsapp.webhook.rechazado`, con actor `SYSTEM` y sin datos personales en metadata. |

## Supuestos y `(TBD)` que siguen abiertos

- **Adaptador real de WhatsApp Business API**: `(TBD)`; no se agregan dependencias ni servicios externos. Variables previstas por la KB: `WHATSAPP_ACCESS_TOKEN`, `WHATSAPP_PHONE_NUMBER_ID`, `WHATSAPP_WEBHOOK_VERIFY_TOKEN`, más `WHATSAPP_APP_SECRET` para la firma.
- **Esquema de firma**: se adopta el documentado por el proveedor (HMAC-SHA256 del cuerpo, `sha256=<hex>`); sin secreto configurado se rechaza todo.
- **Exposición HTTP del webhook**: `(TBD)`. Leer la cabecera de firma en un controlador fuera del paquete `auth` choca con las guardias `AgendaSurfaceTest`/`CurrentActorProviderTest` (prohíben `@RequestHeader`/`getHeader(`). Afecta también al webhook de Mercado Pago (C-15, Parte 3): **decisión transversal a coordinar con el PO**. El punto de entrada es `WhatsAppWebhookService.recibir(firma, cuerpo)`.
- **Disparo desde la agenda**: C-08 no se modifica; quien crea o reprograma el turno invoca `NotificacionService.programar`. Conectarlo automáticamente cambia la cantidad de eventos de auditoría que fijan los tests de C-08: se deja para el change que defina el flujo de confirmación.
- **Anticipación del recordatorio** y **scheduler/cola** del despachador: `(TBD)`; los define quien invoca.
- **Reintentos**: `notificaciones.max-intentos` (por defecto 3) y `notificaciones.reintento-segundos` (por defecto 300): metas iniciales, no normativas.
- **Estado del turno al confirmar**: los valores de `turnos.estado` siguen `(TBD)` (RN-GL-03); la confirmación se registra en la notificación y en auditoría, sin tocar el turno.
- **Costos predecibles (RN-NT-04)**: modelo comercial `(TBD)`; el plan plano está en C-01 A5 y C-21.

## Ajuste de test existente

`AuthMigrationV002Test.baseNuevaAplicaV002EnOrdenJuntoALasDemasMigraciones` fijaba la lista completa `[1..6]` y rompía con cualquier migración nueva. Se acota a su intención: el rango de la Parte 1 sigue siendo exactamente `[1..6]` y toda la secuencia debe estar en orden ascendente.

## Evidencia

- `mvn test` completo en verde (240 tests: 226 previos + 14 nuevos) contra Postgres 17 (Testcontainers).
- `WebhookSignatureVerifierTest` (3) y `NotificacionesWhatsAppPostgresTest` (11): envío con plantilla aprobada, plantilla rechazada (al programar y al despachar), sin plantilla, idempotencia, reintentos hasta `FALLIDA`, webhook sin firma, webhook duplicado, confirmación por botón, append-only y restricciones de la migración.
