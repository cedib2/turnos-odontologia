# Proposal

## Why

C-08 emite eventos de auditoría en memoria y C-04 entrega un proveedor de actor real, pero el sistema todavía no produce evidencia de cumplimiento: no existe registro append-only ni cadena de integridad (RN-SE-02, RN-SE-04, US-014). El orden operativo aprobado por el PO (`C-04 → C-06 → C-05`) habilita ahora reemplazar el puerto `AuditEventPublisher` simulado por un publicador real sobre la migración reservada `V003__audit_append_only_log.sql`. La decisión de alcance recortado del 2026-11-22 (decisor: Lautaro Ellerbach, PO) fija qué se implementa y qué se difiere. El PO autorizó el apply **solo de forma condicional** el 2026-11-22: primero los ajustes pre-apply registrados en `docs/decisiones/C-05-ajustes-pre-apply-PO.md` y la verificación de cobertura; este documento es **solo planeación** y no ejecuta apply.

## What Changes

- Migración `V003__audit_append_only_log.sql`: tabla `auditoria_log` append-only, trigger `BEFORE UPDATE OR DELETE` y `REVOKE` de `UPDATE`/`DELETE`/`TRUNCATE` al rol de aplicación, con documentación explícita de los límites (superuser, DDL, `flyway repair`).
- Hash encadenado **SHA-256** (sin HMAC ni gestor de secretos): orden canónico por secuencia `id`, valor `hash_anterior`/`hash_actual` con génesis y verificación de cadena válida aunque haya saltos de `id` por rollbacks de secuencia, sin huecos lógicos. Concurrencia serializada con `pg_advisory_xact_lock` de clave fija dentro de la misma transacción, antes de leer la cabeza por `id` y hacer el INSERT; se documenta el límite del hash sin clave.
- Catálogo **mínimo y cerrado** de eventos: `turno.creado`, `turno.reprogramado`, `turno.sobreturno`, `bloqueo.creado` y `publictoken.historia.denegado`. Una fila por operación de dominio, nunca por cambio de campo; se conservan los nombres exactos ya emitidos. El quinto evento proviene de C-06, ya archivado, y se conserva para no romper su denegación de HISTORIA al persistir el mismo puerto `AuditEventPublisher`.
- Snapshot inmutable del actor (identificador, nombre, rol, `jti`) más `modo` (`REAL` / `SIMULADO` / `SYSTEM` / `PACIENTE`), alimentado por el proveedor real de C-04. No se reabre C-04 ni se introduce `Authorization`, `Bearer`, `SecurityContext`, `@RequestHeader` ni `/auth` en la superficie de agenda.
- Reemplazo del publicador simulado/no-op por un publicador real que persiste en `auditoria_log` usando el actor de C-04. Se mantiene la correlación, los campos del evento y la ausencia de API de consulta.
- Validación de `metadata` con lista explícita y acotada de 22 campos prohibidos (PII, datos clínicos, secretos, tokens, firma, diagnóstico y credenciales), normalizando claves a minúsculas sin acentos e ignorando también `_` y `-`, con test de rechazo.
- Publicación del evento y operación de negocio en la **misma transacción** (mismo `@Transactional`/conexión local): fallo del INSERT de auditoría revierte todo; rollback de negocio no deja fila de auditoría. Sin `REQUIRES_NEW`, async ni outbox en C-05.
- Verificación y documentación de `spring.flyway.out-of-order=true` en dev/test, sin duplicar configuración.

**BREAKING**: el contrato observable de `audit-event-emission` deja de ser «emisión simulada sin persistencia» y pasa a ser «emisión persistida en log append-only». `AuditEventPublisherTest.noPersisteAuditoriaNiExponeApiDeConsulta()` se trata como **MODIFIED**.

**Diferido (fuera de alcance, `(TBD)` para un change posterior)**: consulta REST de solo lectura para auditoría (sin endpoints ni permisos de lectura nuevos), HMAC con gestor de secretos, y archivado/retención de 10 años (sin job ni borrado).

**Riesgo CRITICO**: cambio de gobernanza CRITICO; requiere aprobación humana explícita del PO antes de cualquier apply. Los datos sensibles en `metadata` y los límites reales de inmutabilidad se tratan con validación y documentación, no con suposiciones.

## Capabilities

### New Capabilities

- `audit-append-only-log`: persistencia append-only del registro de auditoría, cadena de integridad SHA-256, catálogo mínimo de eventos, instantánea del actor, rechazo de campos prohibidos en `metadata` y atomicidad con la operación de negocio. El catálogo sigue cerrado en cinco eventos: `login`/`logout` de C-04 quedan fuera y cualquier evento nuevo debe decidirse por escrito antes del apply.

### Modified Capabilities

- `audit-event-emission`: se reemplaza el contrato `SIMULADO`/sin persistencia por el publicador real con actor de C-04; se conservan correlación y campos del evento, y el escenario que verificaba «no persiste» pasa a verificar persistencia real sin API de consulta.

`agenda` no se modifica: su único requisito relacionado («Sobreturno como excepción auditada») sigue satisfaciéndose con el mismo evento de dominio, ahora persistido. La justificación completa está en `design.md`.

## Impact

- Código: `AuditEventPublisher`, `AuditEvent`, `ActorSimulado`, proveedores de actor de C-04, servicios de agenda que publican eventos, nuevo publicador y validador de `metadata`.
- Persistencia: nueva migración `V003__audit_append_only_log.sql` (rango reservado de la Parte 1), ejecutada out-of-order detrás de V005/V006.
- Tests: `AuditEventPublisherTest` (MODIFIED) y guardia externa `PublicDeltaInvariantsTest` (actualización del contrato SIMULADO a real), más nuevos tests PostgreSQL/Testcontainers para trigger, `REVOKE`, cadena de hashes, concurrencia y transacción compartida (`AuditoriaTransaccionTest`); H2 solo para lógica unitaria con el doble `InMemoryAuditEventRecorder` en el límite del puerto.
- Sin impacto en superficie HTTP: no se crean endpoints ni permisos de lectura; se conservan las guardias de C-04/C-06.
- Cobertura planificada: 10 requirements / 39 escenarios (7 ADDED / 30 en `audit-append-only-log` y 3 MODIFIED / 9 en `audit-event-emission`), 9 grupos / 26 tareas, un test por escenario y tabla escenario→test antes del archive.
- Aprobación: decisión de alcance y ajustes pre-apply del 2026-11-22, Lautaro Ellerbach (PO). Apply autorizado condicionalmente; no ejecutado en este propose.

---

**Decisor**: Lautaro Ellerbach, PO · **Fecha**: 2026-11-22 · **Gobernanza**: CRITICO — solo planeación; apply condicionado a los ajustes y la verificación de cobertura.
