# C-10 — Conexión de la reserva pública y despacho programado del outbox

- **Fecha**: 2026-11-26
- **Pedido**: conectar la reserva pública con WhatsApp y programar el despachador del outbox.
- **Alcance**: reserva pública (C-09) y scheduler del outbox (C-10). No incluye el adaptador real de WhatsApp Business API, que sigue `(TBD)`.

## Qué cambia

| Pieza | Comportamiento |
|---|---|
| `NotificacionWhatsAppReserva` (`publicbooking`) | Implementa `NotificacionPublicaPort`. Corre dentro de la transacción de la operación del turno (outbox): si la operación se revierte, los avisos también. |
| `turno.crear` | Confirmación inmediata y recordatorio `anticipacion` antes del inicio. Si el turno empieza antes de esa anticipación, solo sale la confirmación. |
| `turno.reprogramar` | Anula los avisos pendientes (`BLOQUEADA`, `turno-reprogramado`) y programa los del nuevo horario. |
| `turno.cancelar` | Anula los avisos pendientes (`BLOQUEADA`, `turno-cancelado`). |
| `turno.confirmar` | Sin aviso nuevo: el recordatorio programado se mantiene. |
| `NotificacionService.anularPendientes` | Cierra el outbox de cada aviso pendiente del turno y audita `notificacion.anulada` (tipo nuevo en `AuditoriaCatalogo`). |
| `OutboxDispatcher` | Guardia nueva: si el turno ya no está activo, el aviso queda `BLOQUEADA` (`turno-inactivo`) y no se envía. |
| `OutboxDispatcherProgramado` | Invoca `OutboxDispatcher.procesar` cada `notificaciones.dispatcher.intervalo-ms` (60 s por defecto), con un lote de `notificaciones.dispatcher.lote` (50). `FOR UPDATE SKIP LOCKED` evita envíos dobles entre instancias. |
| `SimulatedWhatsAppGateway` | Registra en el log cada envío simulado, con el teléfono enmascarado. |

## Configuración

| Propiedad | dev | test / otros perfiles |
|---|---|---|
| `notificaciones.reserva.whatsapp` | `true` | ausente → `NotificacionSimulada` (en memoria, como antes) |
| `notificaciones.dispatcher.habilitado` | `true` (cada 30 s) | ausente → no hay despacho programado |
| `notificaciones.recordatorio.anticipacion-horas` | `24` (default **no productivo**) | ausente → solo confirmación, sin recordatorio (RN-GL-03) |

Fuera de dev todo queda apagado hasta que exista el adaptador real: con `WhatsAppNoConfiguradoGateway` cada envío terminaría `FALLIDA` tras los reintentos.

## Sigue abierto

- Adaptador real de WhatsApp Business API, credenciales, plantillas aprobadas y ruta HTTP del webhook: `(TBD)`.
- Anticipación real del recordatorio: `(TBD)`, se define con el PO.
- **Turnos creados, cancelados o reprogramados por el staff** (`/turnos`, C-08) todavía no programan avisos. Conectarlos cambia la cantidad de eventos de auditoría que fijan los tests de C-08 (ver [C-10-whatsapp-notifications-outbox.md](C-10-whatsapp-notifications-outbox.md)).

## Evidencia

- `NotificacionWhatsAppReservaTest` (6) y `OutboxDispatcherGuardiaTest` (2), sin Docker.
- Prueba de punta a punta contra la app en dev y PostgreSQL 16: link de reserva → disponibilidad → reserva (confirmación `ENVIADA` por el gateway simulado y recordatorio agendado 24 h antes) → reprogramación (recordatorio viejo anulado y avisos nuevos) → cancelación (todo lo pendiente anulado), con `notificacion.programada`, `notificacion.enviada` y `notificacion.anulada` en `auditoria_log`.
