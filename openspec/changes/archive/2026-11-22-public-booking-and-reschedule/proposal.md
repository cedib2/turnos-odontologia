# Proposal

## Why

C-06 entrega el contrato de tokens públicos y C-08 la agenda con anti-solapamiento, pero todavía no existe superficie HTTP que permita a un paciente reservar, confirmar, cancelar o reprogramar sin cuenta de usuario: los únicos controladores están en perfiles `dev`/`test` y no consumen tokens. El PO aprobó el 2026-11-22 las 17 decisiones de `docs/decisiones/C-09-decisiones.md`, con lo que se cierran los `(TBD)` que bloqueaban este change y se habilita su propuesta; el apply requiere aprobación explícita posterior.

## What Changes

- Nueva superficie HTTP pública versionada bajo `/publico/v1/turnos`, separada de `/turnos` y del resto de la superficie `dev`/`test`, sin autenticación, RBAC ni cabeceras de identidad de staff.
- Consulta de disponibilidad y creación de turno con scope `RESERVAR`; confirmación, cancelación y reprogramación con scope `GESTIONAR` ligado al `turnoId` del token. No se crea un scope nuevo.
- Escritura con `write-back` real: el turno público se persiste con `estado=reservado` y `origen=link`, su `jti` de gestión y la fila de ocupación correspondiente; la cancelación desactiva turno y ocupación en la misma transacción y libera el hueco.
- Idempotencia de las cuatro operaciones `POST` mediante `idempotency_key` en el cuerpo, con nombres de operación `turno.crear`, `turno.confirmar`, `turno.cancelar` y `turno.reprogramar`; reenvío duplicado sin segundo efecto.
- Ventana exacta de 48 hs hábiles: 2 días hábiles completos en `America/Argentina/Buenos_Aires`, omitiendo fines de semana y feriados del catálogo; fuera de ventana se deriva a recepción **sin liberar** el hueco.
- Hueco tomado entre consulta y escritura: rechazo atómico con hasta 3 alternativas del mismo profesional y tratamiento; sin waitlist (diferida a v0.2 por C-01).
- Eventos de auditoría `turno.confirmado` y `turno.cancelado` con actor modo `PACIENTE` y metadata sin token ni datos personales; notificación de WhatsApp como puerto simulado, sin afirmar entrega (C-10 lo reemplazará).
- Cableado de los servicios de C-06 en el nuevo paquete público, configuración de `public.surface.prefixes=/publico/v1` para que apliquen los response headers obligatorios, y nombre —sin valor— de `PUBLIC_TOKEN_SECRET` en `.env.example`.
- Deltas aditivos: un requisito nuevo en `agenda` para el ciclo de vida público y un requisito nuevo en `audit-event-emission` para los eventos de la superficie pública. Ningún `MODIFIED` ni `REMOVED`.

**Fuera de alcance**: código en `src/`, commits o push; emisión/reemisión de enlaces y QR; RBAC de agenda y deuda del adaptador de actor de C-05; seeds de catálogo y anticipación mínima/máxima (RN-AG-04); webhooks reales de WhatsApp (C-10) y Mercado Pago (C-15); waitlist (C-19, v0.2); nueva migración Flyway (se usa V004/V005/V006; cualquier necesidad futura se reserva para V020+); UI o mockups (cambio no visual).

## Capabilities

### New Capabilities

- `public-booking`: comportamiento observable de la reserva pública por enlace — consulta de huecos, creación, confirmación, cancelación y reprogramación con token, con errores, scopes por operación, idempotencia, alternativas, ventana hábil y notificación simulada.

### Modified Capabilities

- `agenda`: requisito `ADDED` que fija, solo para turnos de origen público y por decisión del PO, los valores `reservado`/`confirmado`/`cancelado` y la liberación atómica del hueco al cancelar. Los 13 requisitos y escenarios actuales permanecen intactos; no hay `MODIFIED` ni `REMOVED`.
- `audit-event-emission`: requisito `ADDED` para los eventos `turno.confirmado` y `turno.cancelado`, el actor modo `PACIENTE` y la metadata sanitizada de la superficie pública. Sin `MODIFIED` ni `REMOVED`.

`public-access-tokens` no se modifica: C-09 solo consume el contrato ya vigente (`RESERVAR`/`GESTIONAR`, `exp`, `jti`, headers e idempotencia).

## Impact

- Código (en el futuro apply): nuevo paquete `com.turnosodontologia.publicbooking` con controladores, guardia de scopes, servicio de ventana hábil, adaptadores de auditoría y notificación; beans de configuración para C-06 sin modificar `publictoken/`; `TurnoRepository.findByTokenPublico` para resolver el `jti` del turno. No se tocan los controladores `@Profile({"dev","test"})` ni la cadena `/auth/**`.
- API: `GET /publico/v1/turnos/disponibilidad` y `POST /publico/v1/turnos` (crear), `/confirmar`, `/cancelar`, `/reprogramar`; token en query `?token=...`; errores `{"error":"<motivo>"}`.
- Configuración: `public.surface.prefixes=/publico/v1`, `public.token.issuer` y el nombre de `PUBLIC_TOKEN_SECRET` sin valor; `.env.example` documenta el nombre de la variable.
- Esquema: ninguno. V004 (metadata e idempotencia), V005 (catálogos) y V006 (turnos/ocupaciones) bastan; no se edita migración aplicada y V020+ queda reservada según `CHANGES.md`.
- Specs: capability nueva `public-booking` y deltas `ADDED` en `agenda` y `audit-event-emission`; `openspec/specs/**` no se toca durante el propose.
- Tests (futuro apply): E2E de Flujo 1 y 2 (KB `07_flujos_principales.md`), carrera por hueco contra PostgreSQL, límite de ventana, reenvío duplicado, scopes y headers. Línea base: 157 tests verdes al cierre de C-06.
- Gobernanza: MEDIO con dos checkpoints (D12). Aprobación del PO para el propose: 2026-11-22, `docs/decisiones/C-09-decisiones.md`. Sin implementación ni commits en este propose.
