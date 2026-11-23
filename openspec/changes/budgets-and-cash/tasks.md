# Tasks

## 1. Esquema y persistencia

- [ ] 1.1 Crear `V020__budgets_and_cash.sql` con `presupuestos` e `items_presupuesto`, FKs a `pacientes`, `tratamientos` y `turnos`, `NUMERIC(12,2)`, checks de `estado`, `cantidad` y `precio`, `moneda` fija `ARS` e índices; verificar con `PostgresFlywayTest` que la migración aplica limpia, que no se edita V001–V006 y que no existen columnas de cobro, impuesto ni caja.
- [ ] 1.2 Implementar entidades y repositorios de `Presupuesto` e `Ítem` sobre `AuditableEntity`, con borrado lógico y FK opcional a `Turno`; verificar tests de persistencia que prueban anulación con `activo=false`, varios presupuestos por turno y que el turno permanece inalterado.

## 2. Reglas de negocio

- [ ] 2.1 Implementar el ítem con `tratamiento_id` existente, `precio` snapshot informado por recepción, `cantidad > 0`, `precio >= 0` y `descripción`; verificar test que rechaza datos inválidos y test que confirma que `tratamientos` no gana columna `precio` ni seeds.
- [ ] 2.2 Implementar el cálculo `ROUND(SUM(cantidad * precio), 2)` con `HALF_UP` y `BigDecimal`, sin descuentos ni redondeo por ítem; verificar tests de varios ítems, de tercer decimal cinco y de precio cero.
- [ ] 2.3 Implementar estados `emitido`/`anulado`: creación `emitido`, modificación solo si `emitido`, anulación con `activo=false` y conservación de ítems y total; verificar tests de transición válida, modificación rechazada y consulta posterior al anulado.
- [ ] 2.4 Implementar la asociación opcional al turno y el contrato de respuesta con `id`, `total`, `moneda`, `estado`, `turno_id` e ítems; verificar tests con y sin turno, y que la respuesta no expone campos de cobro, comprobante ni asiento.

## 3. Superficie HTTP

- [ ] 3.1 Exponer `GET /presupuestos`, `GET /presupuestos/{id}`, `POST /presupuestos`, `PUT /presupuestos/{id}` y `PATCH /presupuestos/{id}/anular` con `@Profile({"dev","test"})`, DTOs y errores `{"error":"<motivo>"}`; verificar tests Web MVC sin credenciales de staff y con 400/404/409 según el caso.
- [ ] 3.2 Verificar con un test de superficie que `/agenda`, `/turnos`, `/bloqueos`, `/sobreturnos`, los catálogos C-07 y `/auth/**` permanecen intactos, y que no existe endpoint de paciente, PDF, caja ni cobro.

## 4. Auditoría

- [ ] 4.1 Ampliar `AuditoriaCatalogo` con `presupuesto.creado` y `presupuesto.modificado` únicamente; verificar test de catálogo que acepta ambos y rechaza un tipo inventado, cumpliendo la regla de C-05.
- [ ] 4.2 Emitir los eventos desde creación, modificación y anulación (`motivo=anulado`) con entidad `presupuesto`, `entidadId`, actor `ActorSimulado.recepcionista()` modo `SIMULADO` y `correlationId` distinto por operación; verificar test que consulta `auditoria_log` y comprueba una fila por operación y ninguna en operaciones rechazadas.
- [ ] 4.3 Limitar la metadata a `presupuestoId`, `turnoId` opcional, `total`, `moneda`, `cantidadItems` y `motivo`; verificar test de metadata permitida y test que no aparecen datos personales, tokens ni credenciales.

## 5. Verificación final

- [ ] 5.1 Ejecutar la suite completa con `mvn test`; verificar 219 tests de línea base más los nuevos en verde y sin regresión en las guardias de C-04/C-06 ni en los specs de C-07/C-08/C-09.
- [ ] 5.2 Ejecutar `openspec validate budgets-and-cash --strict`; verificar salida sin errores y que los deltas cubren `budgets` y `audit-event-emission` sin `MODIFIED` ni `REMOVED`.
- [ ] 5.3 Confirmar que el propose no escribió en `src/`, no creó secretos ni hizo commits, y que la única migración planificada es `V020__budgets_and_cash.sql`; verificar con `git status --short` y revisión del árbol `openspec/changes/budgets-and-cash/`.

## Workflow follow-up

- No marcar `[x]` en `CHANGES.md` para C-14 hasta completar la verificación técnica del apply.
- Solicitar la aprobación del PO para archivar el change y sincronizar los specs principales.
