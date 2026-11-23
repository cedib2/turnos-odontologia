# Tasks

## 1. Esquema y persistencia

- [x] 1.1 Crear `V020__budgets_and_cash.sql` con `presupuestos` e `items_presupuesto`, FKs a `pacientes`, `tratamientos` y `turnos`, `NUMERIC(12,2)`, checks de `estado`, `cantidad` y `precio`, `moneda` fija `ARS` e índices; verificado con `Migration020Test` sobre `PostgresFlywayTest`: la migración aplica limpia, V001–V006 quedan intactos y no existen columnas de cobro, impuesto ni caja.
- [x] 1.2 Implementar entidades y repositorios de `Presupuesto` e `Ítem` sobre `AuditableEntity`, con borrado lógico y FK opcional a `Turno`; verificado con tests de persistencia que prueban anulación con `activo=false`, varios presupuestos por turno y que el turno permanece inalterado.

## 2. Reglas de negocio

- [x] 2.1 Implementar el ítem con `tratamiento_id` existente, `precio` snapshot informado por recepción, `cantidad > 0`, `precio >= 0` y `descripción`; verificado con test que rechaza datos inválidos y con la Migración V020, que no agrega columna `precio` ni seeds a `tratamientos`.
- [x] 2.2 Implementar el cálculo `ROUND(SUM(cantidad * precio), 2)` con `HALF_UP` y `BigDecimal`, sin descuentos ni recargos; verificado con tests de varios ítems, de tercer decimal cinco y de precio cero.
- [x] 2.3 Implementar estados `emitido`/`anulado`: creación `emitido`, modificación solo si `emitido`, anulación con `activo=false` y conservación de ítems y total; verificado con tests de transición válida, modificación rechazada y consulta posterior al anulado.
- [x] 2.4 Implementar la asociación opcional al turno y el contrato de respuesta con `id`, `total`, `moneda`, `estado`, `turno_id` e ítems; verificado con tests con y sin turno, y con el guardia de superficie que prohíbe campos de cobro, comprobante ni asiento.

## 3. Superficie HTTP

- [x] 3.1 Exponer `GET /presupuestos`, `GET /presupuestos/{id}`, `POST /presupuestos`, `PUT /presupuestos/{id}` y `PATCH /presupuestos/{id}/anular` con `@Profile({"dev","test"})`, DTOs y errores `{"error":"<motivo>"}`; verificado con `PresupuestoControllerTest` sin credenciales de staff y con 400/404/409 según el caso.
- [x] 3.2 Verificado con `AgendaSurfaceTest` que `/agenda`, `/turnos`, `/bloqueos`, `/sobreturnos`, los catálogos C-07 y `/auth/**` permanecen intactos, y que no existe endpoint de paciente, PDF, caja ni cobro.

## 4. Auditoría

- [x] 4.1 Ampliar `AuditoriaCatalogo` con `presupuesto.creado` y `presupuesto.modificado` únicamente; verificado con `AuditoriaCatalogoTest`, que acepta ambos y rechaza un tipo inventado, cumpliendo la regla de C-05.
- [x] 4.2 Emitir los eventos desde creación, modificación y anulación (`motivo=anulado`) con entidad `presupuesto`, `entidadId`, actor `ActorSimulado.recepcionista()` modo `SIMULADO` y `correlationId` distinto por operación; verificado con `PresupuestoServiceIntegrationTest` y `AuditoriaCatalogoTest` contra `auditoria_log`, con una fila por operación y ninguna en operaciones rechazadas.
- [x] 4.3 Limitar la metadata a `presupuestoId`, `turnoId` opcional, `total` como texto, `moneda`, `cantidadItems` y `motivo`; verificado con `PresupuestoAuditMetadataTest`: metadata permitida, sin datos personales, tokens ni credenciales, y cadena de hash íntegra tras releer JSONB.

## 5. Verificación final

- [x] 5.1 Ejecutar la suite completa con `mvn test`: 241 tests, 0 fallos, 0 errores, 0 omitidos (219 de línea base más 22 nuevos), sin regresión en las guardias de C-04/C-06 ni en los specs de C-07/C-08/C-09.
- [x] 5.2 Ejecutar `openspec validate budgets-and-cash --strict`: salida sin errores; los deltas cubren `budgets` y `audit-event-emission` sin `MODIFIED` ni `REMOVED`.
- [x] 5.3 Confirmar que el apply no creó secretos ni hizo commits: la única migración nueva es `V020__budgets_and_cash.sql`; verificado con `git status --short` y revisión del árbol `openspec/changes/budgets-and-cash/`.

## Workflow follow-up

- C-14 marcado en `CHANGES.md` tras la verificación técnica del apply.
- Queda pendiente la aprobación del PO para archivar el change y sincronizar los specs principales.
