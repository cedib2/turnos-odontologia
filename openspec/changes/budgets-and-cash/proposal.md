# Proposal

## Why

C-05, C-07 y C-08 ya entregan auditoría append-only, catálogos y agenda con anti-solapamiento, pero el sistema todavía no puede emitir un presupuesto con total verificable: no hay entidades, esquema, cálculo ni superficie financiera, y el catálogo de C-07 no tiene precio. El PO aprobó el 2026-11-23 las 15 decisiones de `docs/decisiones/C-14-decisiones.md`, que cierran los `(TBD)` que bloqueaban este change y habilitan su propuesta; el apply se ejecutará después con verificación técnica completa.

## What Changes

- Nueva capability `budgets`: presupuestos con `id`, paciente, `turno_id` opcional, `estado`, `total` en `ARS`, `activo` y ítems con `tratamiento_id`, `cantidad`, `precio` snapshot y `descripción`.
- Cálculo determinista del total: `ROUND(SUM(cantidad * precio), 2)` con `HALF_UP`; `cantidad > 0`, `precio >= 0`, impuestos incluidos (C-01 A5), sin descuentos ni recargos.
- Precio snapshot informado por recepción: **no** se agrega `precio` a `tratamientos`, no hay seeds y no cambia la lectura de C-07.
- Estados `emitido` y `anulado` como `String`; creación `emitido`, modificación solo mientras esté `emitido`, anulación con `activo=false` y borrado lógico.
- Asociación opcional al turno con FK nullable; varios presupuestos por turno; el presupuesto no modifica el turno ni su ciclo de vida público de C-09.
- Superficie HTTP indicativa `/presupuestos` en perfiles `dev`/`test`, sin autenticación, RBAC ni cabeceras de identidad; actor de auditoría `ActorSimulado.recepcionista()` con modo `SIMULADO`.
- Migración nueva **`V020__budgets_and_cash.sql`** (Parte 3), sin editar V001–V006 y sin migración por debajo de V020.
- Deltas aditivos: capability nueva `budgets` y requisitos `ADDED` en `audit-event-emission` para `presupuesto.creado` y `presupuesto.modificado`. Sin `MODIFIED` ni `REMOVED`.

**Fuera de alcance**: código en `src/`, commits o push durante el propose; asiento de caja, arqueo y conciliación (C-15); cobro real, Mercado Pago, webhooks, reembolso y waitlist (C-15/C-19); factura ARCA (C-16); notificaciones (C-10); lectura por paciente y PDF (diferidos por decisión del PO); UI o mockups (cambio no visual); permisos financieros nuevos y protección RBAC de agenda (deuda de C-05).

## Capabilities

### New Capabilities

- `budgets`: comportamiento observable de la emisión, consulta, modificación y anulación de presupuestos con ítems a precio snapshot, total en ARS, estados, asociación opcional al turno y contrato estable para los changes de cobro y facturación.

### Modified Capabilities

- `audit-event-emission`: requisitos `ADDED` para los eventos `presupuesto.creado` y `presupuesto.modificado`, su actor modo `SIMULADO` y su metadata acotada. Los 7 requisitos actuales permanecen intactos; no hay `MODIFIED` ni `REMOVED`.

## Impact

- Código (en el futuro apply): nuevos paquetes de dominio, aplicación, persistencia e interfaz web para presupuestos e ítems; reutilización de `AuditableEntity`, `ActorSimulado`, `AuditEvent` y `CurrentActorProvider`. No se modifica `publictoken/`, la cadena `/auth/**` ni los controladores `dev`/`test` existentes.
- API: `GET /presupuestos`, `GET /presupuestos/{id}`, `POST /presupuestos`, `PUT /presupuestos/{id}`, `PATCH /presupuestos/{id}/anular`, con respuestas `{"error":"<motivo>"}` ante validación, inexistencia o transición inválida.
- Esquema: `V020__budgets_and_cash.sql` crea `presupuestos` e `items_presupuesto` con FK a `pacientes`, `tratamientos` y `turnos`, columnas `NUMERIC(12,2)` y borrado lógico. No se editan migraciones aplicadas.
- Auditoría: ampliación de `AuditoriaCatalogo` con dos tipos nuevos, propuesta por escrito en `docs/decisiones/C-14-decisiones.md` según la regla de C-05.
- Specs: capability nueva `budgets` y deltas `ADDED` en `audit-event-emission`; `openspec/specs/**` no se toca durante el propose.
- Tests (futuro apply): migración Flyway, cálculo y redondeo, validación de ítems, transiciones, asociación opcional, contrato HTTP, catálogo y metadata de auditoría. Línea base: 219 tests verdes al cierre de C-09.
- Gobernanza: BAJO según `CHANGES.md:460` y D2 del acta. Aprobación del PO para el propose: 2026-11-23. Sin implementación ni commits en este propose.
