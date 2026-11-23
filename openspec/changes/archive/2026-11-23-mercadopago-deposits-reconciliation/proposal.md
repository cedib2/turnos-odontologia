# Proposal

## Why

C-14 entrega presupuestos con total verificable, pero el consultorio todavía no puede cobrar la seña, confirmar el pago recibido ni dejar evidencia de la conciliación: no existe la entidad `Seña/PagoMP`, la integración con Mercado Pago, la verificación de firma del webhook ni los eventos de auditoría de dinero. El PO aprobó el 2026-11-23 las decisiones `MP-1`..`MP-12` de `docs/decisiones/C-15-decisiones.md`, que cierran los `(TBD)` que bloqueaban este change. El apply sigue sujeto a la gobernanza **CRITICO** del change: requiere aprobación humana explícita adicional antes de escribir código.

## What Changes

- Migración **`V021__mercadopago_deposits.sql`** de la Parte 3: entidad `Seña/PagoMP` con `turno_id`, `presupuesto_id` opcional, `monto NUMERIC(12,2)`, `moneda ARS`, `estado`, `proveedor="mercadopago"`, `external_reference`, `idempotency_key`, `fecha_aprobacion` y `conciliado`. No se editan V001–V020; el número histórico `010` del scope quedó supersedido por la tabla de reservas.
- Solicitud de seña por REST con `RestTemplate` (dependencia ya presente) y `POST /v1/preferences`, con `external_reference` derivado de un UUID interno y `X-Idempotency-Key`. Sin SDK ni dependencias nuevas; superficie `@Profile({"dev","test"})` sin RBAC nuevo.
- Webhook `POST /publico/v1/webhooks/mercadopago` autenticado por **firma HMAC-SHA256 local** con `MERCADOPAGO_WEBHOOK_SECRET`; firma ausente o inválida → `400` sin efectos. Idempotencia reutilizando `public_idempotencia` (V004) con operación `pago.webhook`, en la misma transacción que el cambio de estado; reenvío → `200` sin segundo efecto.
- Estados `pendiente`, `aprobado`, `rechazado`, `cancelado`, `reembolsado` con mapping documentado desde Mercado Pago, transiciones solo desde `pendiente` y eventos tardíos sobre estados terminales descartados sin efectos.
- Conciliación verificable turno–pago mediante `conciliado` y su evento; **no se crean entidades de caja, asiento ni arqueo** en este change (pendiente explícito por falta de definición en la KB).
- Reembolso **total** y liberación inmediata del hueco según C-01 A4, ejecutado por recepción sobre pagos aprobados; pago rechazado conserva el turno confirmado y avisa a recepción por un puerto NO-OP, sin modificar C-09.
- Auditoría de cada movimiento de dinero con cinco tipos aprobados por acta PO (`pago.solicitado`, `pago.aprobado`, `pago.rechazado`, `pago.reembolsado`, `pago.conciliado`), metadata sanitizada y actores con modo explícito.
- Precio plano todo incluido según la enmienda **C-01 A5**: este change no incorpora desglose de costos de API. Facturación ARCA permanece en C-16.
- Tests con Testcontainers/PostgreSQL para webhook duplicado, pago rechazado, conciliación, reembolso y auditoría; más tests unitarios de firma, mapping y transiciones inválidas.

## Capabilities

### New Capabilities

- `deposit-payments`: ciclo de vida observable de la seña — solicitud idempotente, recepción y verificación del webhook, estados del pago, conciliación turno–pago y reembolso.

### Modified Capabilities

- `audit-event-emission`: incorpora los eventos de dinero y su metadata, y acota el alcance del cambio de presupuestos que hoy afirma que no hay eventos de pago.
- `audit-append-only-log`: amplía el catálogo cerrado de tipos con los cinco eventos de pago aprobados por el PO, conservando granularidad de una fila por operación.

## Impact

- **Código**: nuevos paquetes de dominio/application/infrastructure/interfaces para señas y webhook sobre la arquitectura DDD de `com.turnosodontologia`; reutilización de `public_idempotencia` (V004), `AuditoriaCatalogo`, `AuditoriaMetadataValidator`, `AuditableEntity`, `Presupuesto` (V020) y `RestTemplate` de `spring-boot-starter-web`. Sin cambios en `src/` durante el propose.
- **API**: `POST /senias`, `POST /senias/{id}/reembolsar` y `POST /publico/v1/webhooks/mercadopago`, en perfiles `dev`/`test`; sin endpoints de consulta de auditoría ni nuevos permisos RBAC.
- **Esquema**: solo V021; V020 queda intacta y V021+ continúa libre para C-16.
- **Configuración**: solo los nombres ya documentados en `.env.example`: `MERCADOPAGO_ACCESS_TOKEN`, `MERCADOPAGO_WEBHOOK_SECRET` y `APP_BASE_URL`; sin valores reales ni gestor de secretos nuevo.
- **Dependencias**: C-05, C-09 y C-14 (completadas). No modifica C-09, C-10 ni la cadena `/auth/**`.
- **Gobernanza**: CRITICO — el propose es análisis aprobado; el apply requiere confirmación humana explícita posterior.
