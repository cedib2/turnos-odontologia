# C-10 — Avisos de WhatsApp de turnos (reserva pública y staff) y despacho programado del outbox

- **Fecha**: 2026-11-26
- **Pedido**: conectar la reserva pública y los turnos del staff con WhatsApp y programar el despachador del outbox.
- **Alcance**: reserva pública (C-09), operaciones de staff (C-08), reembolso de seña (C-15) y scheduler del outbox (C-10). No incluye el adaptador real de WhatsApp Business API, que sigue `(TBD)`.

## Qué cambia

| Pieza | Comportamiento |
|---|---|
| `AvisosTurno` / `AvisosTurnoWhatsApp` | Puerto único de avisos del ciclo de vida del turno (`creado`, `reprogramado`, `cancelado`). Corre dentro de la transacción de la operación (outbox): si la operación se revierte, los avisos también. Sin WhatsApp habilitado se usa `AvisosTurno.NINGUNO`. |
| `NotificacionWhatsAppReserva` (`publicbooking`) | Traduce las operaciones de la reserva pública (`NotificacionPublicaPort`) a `AvisosTurno`. |
| Staff (C-08) | `CreacionTurnoService` → `creado`; `ReprogramacionService` y `SobreturnoService` → `reprogramado`; `CancelacionTurnoService` → `cancelado`. |
| Reembolso de seña (C-15) | `SeniaService.reembolsar` cancela el turno asociado → `cancelado`. |
| Creación | Confirmación inmediata y recordatorio `anticipacion` antes del inicio. Si el turno empieza antes de esa anticipación, solo sale la confirmación. |
| Reprogramación y sobreturno | Anula los avisos pendientes (`BLOQUEADA`, `turno-reprogramado`) y programa los del nuevo horario. |
| Cancelación | Anula los avisos pendientes (`BLOQUEADA`, `turno-cancelado`). |
| Confirmación por el paciente | Sin aviso nuevo: el recordatorio programado se mantiene. |
| `NotificacionService.anularPendientes` | Cierra el outbox de cada aviso pendiente del turno y audita `notificacion.anulada` (tipo nuevo en `AuditoriaCatalogo`). |
| `OutboxDispatcher` | Guardia nueva: si el turno ya no está activo, el aviso queda `BLOQUEADA` (`turno-inactivo`) y no se envía. |
| `OutboxDispatcherProgramado` | Invoca `OutboxDispatcher.procesar` cada `notificaciones.dispatcher.intervalo-ms` (60 s por defecto), con un lote de `notificaciones.dispatcher.lote` (50). `FOR UPDATE SKIP LOCKED` evita envíos dobles entre instancias. |
| `SimulatedWhatsAppGateway` | Registra en el log cada envío simulado, con el teléfono enmascarado. |

## Configuración

| Propiedad | dev | test / otros perfiles |
|---|---|---|
| `notificaciones.turnos.whatsapp` | `true` | ausente → reserva con `NotificacionSimulada` (en memoria) y staff con `AvisosTurno.NINGUNO`: los tests de C-08/C-09 no cambian |
| `notificaciones.dispatcher.habilitado` | `true` (cada 30 s) | ausente → no hay despacho programado |
| `notificaciones.recordatorio.anticipacion-horas` | `24` (default **no productivo**) | ausente → solo confirmación, sin recordatorio (RN-GL-03) |

Fuera de dev todo queda apagado hasta que exista el adaptador real: con `WhatsAppNoConfiguradoGateway` cada envío terminaría `FALLIDA` tras los reintentos.

## Sigue abierto

- Adaptador real de WhatsApp Business API, credenciales, plantillas aprobadas y ruta HTTP del webhook: `(TBD)`.
- Anticipación real del recordatorio: `(TBD)`, se define con el PO.
- **Relleno de lista de espera (C-19)**: no se conecta. `RellenoWaitlistService.adjudicarTurnoLiberado` reactiva el turno cancelado con `Turno.reasignar`, que **no cambia el paciente**: el aviso le llegaría a quien canceló y no al candidato adjudicado. Hay que resolver primero la reasignación del paciente.

## Evidencia

- `AvisosTurnoWhatsAppTest` (6), `NotificacionWhatsAppReservaTest` (2) y `OutboxDispatcherGuardiaTest` (2), sin Docker. Los tests de servicios de C-08 reciben `AvisosTurno.NINGUNO`.
- Prueba de punta a punta contra la app en dev y PostgreSQL 16: link de reserva → disponibilidad → reserva (confirmación `ENVIADA` por el gateway simulado y recordatorio agendado 24 h antes) → reprogramación (recordatorio viejo anulado y avisos nuevos) → cancelación (todo lo pendiente anulado), con `notificacion.programada`, `notificacion.enviada` y `notificacion.anulada` en `auditoria_log`. Lo mismo desde el staff: `POST /turnos`, `/turnos/{id}/reprogramacion`, `/sobreturnos` y `/turnos/{id}/cancelacion`.
