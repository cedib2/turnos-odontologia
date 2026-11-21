# Tasks

Criterios de aceptación: `specs/agenda/spec.md` y `specs/audit-event-emission/spec.md`. Detalle técnico: `design.md`.

> TDD implícito en cada tarea de implementación: escribir el test que falla (rojo), implementar lo mínimo (verde) y refactorizar. Ningún checkbox se marca `[x]` hasta que su verificación se haya ejecutado y registrado evidencia. Los checkpoints MEDIO (T3 y T7) son revisión humana y quedan sin marcar hasta que el PO/equipo los acepte. `C-08` en `CHANGES.md` sigue `[ ]` hasta la verificación técnica; C-04 y C-05 permanecen pendientes, por lo que no hay despliegue a producción ni acreditación de cumplimiento.

## 0. T0 — Infraestructura de prueba y extensiones

- [x] 0.1 Agregar la dependencia/servicio de PostgreSQL real aprobado para tests de concurrencia y verificar que `CREATE EXTENSION btree_gist` funciona en el entorno de CI; evidencia: test de integración en rojo sin la extensión y en verde con ella.
- [x] 0.2 Configurar el perfil dev/test con la conexión PostgreSQL y verificar que Flyway aplica una migración limpia desde cero; evidencia: arranque del contexto de test sin errores de migración.

## 1. T1 — Slice real de C-07 (catálogo mínimo)

- [x] 1.1 TDD de la migración 005 con `tratamientos` (duración positiva configurable), `horarios`, `feriados` y zona horaria `America/Argentina/Buenos_Aires`; verificar con test de migración en PostgreSQL y test unitario de duración no positiva rechazada.
- [x] 1.2 TDD del servicio de consulta de configuración de scheduling usado por disponibilidad; verificar que sus tests devuelven duración, horario y feriado sin inventar seeds `(TBD)`.

## 2. T2 — Migración 006 y ocupación

- [x] 2.1 TDD de la migración 006: `turnos` y `bloqueos` con auditoría, soft delete, índices `(profesional_id, inicio, fin)`, `(sillon_id, inicio, fin)`, `estado`, `token_publico`, `es_sobreturno`, y `estado`/`origen` `VARCHAR` no vacío sin enum; verificar con test de esquema Flyway.
- [x] 2.2 TDD de la tabla de ocupación y las exclusion constraints para profesional, sillón y la relación turno ↔ bloqueo con `btree_gist`/`tstzrange`; verificar con tests de integración en rojo antes de la constraint y en verde después, incluyendo intervalos contiguos permitidos.

## 3. T3 — Modelo de dominio y repositorios

- [x] 3.1 TDD de entidades `Turno`/`Bloqueo` y repositorios con mapeo explícito `snake_case` (D4), `AuditableEntity` y sin borrado físico (D5); verificar con tests de persistencia sobre PostgreSQL.
- [x] 3.2 CHECKPOINT MEDIO 1 — revisar migraciones 005/006, modelo y constraint anti-solapamiento con el equipo/PO; verificar que los tests de integración pasan y que la revisión queda registrada antes de continuar.

## 4. T4 — Puertos de actor simulado y emisión de eventos

- [x] 4.1 TDD de `CurrentActorProvider` con actor fijo `recepcionista`/`system` solo en dev/test; verificar con tests y con un inventario de rutas que confirme ausencia de headers, tokens, cookies y `/auth/*`.
- [x] 4.2 TDD de `AuditEventPublisher` con recorder en memoria/no-op y del contrato de evento (tipo, entidad/id, actor, `modo=SIMULADO`, `occurredAt`, motivo/metadata, `correlationId`); verificar con tests unitarios que no hay persistencia ni API de consulta.

## 5. T5 — Creación de turno con duración variable

- [x] 5.1 TDD del caso de uso de creación de turno que deriva la duración del catálogo y calcula `fin = inicio + duracion_minutos`; verificar con tests unitarios y de integración que el turno aparece en la agenda.
- [x] 5.2 TDD de validaciones de duración no positiva y de `estado`/`origen` no vacíos sin valores inventados; verificar con tests que rechazan la operación sin persistir el turno.

## 6. T6 — Bloqueos y disponibilidad

- [x] 6.1 TDD de creación de bloqueos con motivo obligatorio y al menos un recurso (profesional o sillón); verificar con tests que rechazan bloqueos sin recurso y que impiden turnos solapados.
- [x] 6.2 TDD de disponibilidad que excluye bloqueos, feriados e intervalos fuera de horario; verificar con tests que esos huecos no se ofrecen como reservables.

## 7. T7 — Sobreturno con override

- [x] 7.1 TDD del sobreturno como desplazamiento atómico del turno existente, exigiendo motivo y actor y manteniendo la constraint activa; verificar con tests de integración que el destino se valida, que sin motivo/actor se rechaza y que no queda turno modificado ante fallo.
- [x] 7.2 CHECKPOINT MEDIO 2 — revisar el sobreturno, su motivo/actor y el evento emitido frente a IN-04/RN-GL-01; verificar que la evidencia de la revisión esté registrada antes de continuar.

## 8. T8 — Reprogramación atómica y write-back interno

- [x] 8.1 TDD de reprogramación que libera el hueco original y ocupa el nuevo en una sola transacción; verificar con tests de PostgreSQL que un destino ocupado deja el turno original intacto y que dos operaciones simultáneas no generan doble ocupación.
- [x] 8.2 TDD del write-back real: la consulta posterior a crear, reprogramar o bloquear refleja el estado nuevo sin réplica externa; verificar con tests de integración sobre la agenda.

## 9. T9 — Vista día/semana y superficie HTTP dev/test

- [x] 9.1 TDD de las consultas de agenda por día y por semana, filtrables por profesional y sillón, interpretadas en `America/Argentina/Buenos_Aires`; verificar con tests que devuelven solo turnos y bloqueos del rango y recurso pedidos.
- [x] 9.2 TDD de los controladores indicativos `/agenda`, `/turnos`, `/bloqueos` y `/sobreturnos` restringidos a perfiles dev/test; verificar con tests de capa web y un inventario de rutas que no existe autenticación, RBAC ni lectura de identidad de la request.

## 10. T10 — Verificación integral y cierre

- [x] 10.1 Ejecutar la suite completa de integración contra PostgreSQL y la suite unitaria con H2 con el comando de build del repositorio; verificar que no hay fallos y que la cobertura incluye concurrencia, bloqueos, reprogramación atómica, sobreturno y emisión de eventos.
- [x] 10.2 Ejecutar `openspec validate` para el change y verificar que los cuatro artefactos están completos y que los deltas cubren todos los requerimientos de las specs.
- [x] 10.3 Registrar la evidencia de cierre y verificar explícitamente que no se marcan `[x]` los checkpoints sin revisión, que `C-08` permanece pendiente en `CHANGES.md` y que no se declaró despliegue a producción ni cumplimiento.

## Workflow follow-up

- Marcar `C-08` en `CHANGES.md` recién después de la verificación técnica y con C-04/C-05 resueltos si se pretende despliegue o cumplimiento.
- Archive the change after the project's review requirements are satisfied.
- Verify the archived result.
