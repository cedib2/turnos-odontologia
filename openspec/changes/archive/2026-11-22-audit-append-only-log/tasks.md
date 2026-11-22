# Tasks

> Gobernanza CRITICO: estos tasks son un plan. El PO autorizó el apply solo de forma condicional (ajustes pre-apply + verificación de cobertura); no ejecutar apply sin verificar esa condición. Fuente estable de escenarios: `openspec/specs/**`; ningún test debe leer `openspec/changes/<change-activo>/`.
>
> Correcciones del pre-check de archive autorizadas por el PO el 2026-11-22 (registro en `docs/decisiones/C-05-ajustes-pre-apply-PO.md` y `docs/decisiones/C-05-deuda-adaptador-actor.md`): modos de actor con `actor_id` nulo, guardias restauradas, exclusividad dev/test del publicador y deuda del adaptador de actor. No cambian el número de requirements ni de escenarios: 10 requirements / 39 escenarios, mapa 39/39. Sin archive, sync, marcado de `CHANGES.md`, commits ni push.

## 1. Baseline y precondiciones

- [x] 1.1 Ejecutar `mvn test` sin cambios previos y registrar el baseline de 157 tests, 0 failures y 0 errors; verificación: salida del comando guardada y sin contenedor en fallo
- [x] 1.2 Confirmar `spring.flyway.out-of-order: true` en `src/main/resources/application-dev.yml` y verificar que no se duplica en otra configuración; verificación: búsqueda en `src/main/resources` y en `@DynamicPropertySource` de tests
- [x] 1.3 Verificar que `src/test/java` no contiene referencias a `openspec/changes/`; verificación: 0 coincidencias y guardias leyendo solo `openspec/specs/**`

## 2. V003 y registro append-only — 4 escenarios de `audit-append-only-log` R1

- [x] 2.1 Escribir `AuditoriaLogAppendOnlyTest` con PostgreSQL 17/Testcontainers (`PostgresFixture` / `PostgresFlywayTest`) para: inserción conservada, `UPDATE` rechazado, `DELETE` rechazado y `TRUNCATE` rechazado desde rol no superuser; verificación: `mvn test -Dtest=AuditoriaLogAppendOnlyTest` en rojo por migración ausente
- [x] 2.2 Implementar `V003__audit_append_only_log.sql` con tabla, secuencia, triggers `BEFORE UPDATE` y `BEFORE DELETE`, trigger **BEFORE TRUNCATE** con `FOR EACH STATEMENT` (PostgreSQL no tiene TRUNCATE row-level) y `REVOKE UPDATE, DELETE, TRUNCATE` al rol de aplicación; verificación: `mvn test -Dtest=AuditoriaLogAppendOnlyTest` en verde
- [x] 2.3 Verificar que los límites de inmutabilidad (superuser, dueño de tabla/DDL, `flyway repair`) y el carácter de dato personal de `ip INET` quedan documentados en `design.md`; verificación: búsqueda de los términos y sin documento nuevo

## 3. Cadena SHA-256 sin clave — 6 escenarios de `audit-append-only-log` R2

- [x] 3.1 Escribir `AuditoriaHashChainTest` en PostgreSQL/Testcontainers con un test por escenario: génesis, enlace con predecesora, verificación íntegra, cadena válida con saltos de `id` y sin huecos lógicos, detección de alteración por superuser y dos publicaciones concurrentes sin bifurcar la cadena; verificación: `mvn test -Dtest=AuditoriaHashChainTest` en rojo
- [x] 3.2 Implementar canonicalización por `id`, `hash_anterior`/`hash_actual` SHA-256, verificación de cadena con definición de «cadena válida» (predecesora inmediata por `id`, génesis en la primera, ids no consecutivos permitidos, sin huecos lógicos) y serialización con `pg_advisory_xact_lock` de clave fija adquirido en la misma transacción antes de leer la última fila y hacer el INSERT; verificación: `mvn test -Dtest=AuditoriaHashChainTest` en verde y cada lock liberado en commit/rollback
- [x] 3.3 Verificar que no aparece bifurcación con dos lecturas concurrentes de la misma cabeza, que la verificación es un servicio interno sin endpoint (`/audit`, `/auditoria`) y que el límite del hash sin clave queda registrado en `design.md`; verificación: `mvn test -Dtest=AuditoriaHashChainTest` en verde, 0 rutas nuevas y búsqueda de `HMAC` sin uso

## 4. Transacción compartida — 2 escenarios de `audit-append-only-log` R7

- [x] 4.1 Escribir `AuditoriaTransaccionTest` con PostgreSQL 17/Testcontainers (`PostgresFixture` / `PostgresFlywayTest`), un test por escenario: `insertAuditoriaFallaYRevierteLaOperacionDeNegocio()` y `rollbackDeNegocioTrasPublicarEventoNoDejaFilaDeAuditoria()`; verificación: `mvn test -Dtest=AuditoriaTransaccionTest` en rojo
- [x] 4.2 Implementar la publicación dentro de la misma transacción de base de datos de la operación de negocio (mismo `@Transactional`/conexión local), sin `REQUIRES_NEW`, sin async y sin outbox en C-05; verificación: `mvn test -Dtest=AuditoriaTransaccionTest` en verde, sin fila de negocio ni de auditoría tras el fallo del INSERT y sin fila de auditoría tras el rollback de negocio
- [x] 4.3 Verificar la exclusividad de beans por perfil: los unitarios H2 siguen usando el doble `InMemoryAuditEventRecorder` en el límite del puerto (solo `@Profile("test")`), mientras el publicador JDBC queda activo en dev y producción (`@Profile("!test")`); verificación: `mvn test -Dtest=AuditEventPublisherTest,AuditoriaTransaccionTest,AuditEventPublisherPerfilDevTest` en verde

## 5. Catálogo mínimo y granularidad — 6 escenarios de `audit-append-only-log` R3

- [x] 5.1 Escribir `AuditoriaCatalogoTest` para `turno.creado`, `turno.reprogramado`, `turno.sobreturno`, `bloqueo.creado`, operación rechazada sin fila y denegación de HISTORIA con token público (`entidad_id` nullable), comprobando una sola fila por operación; verificación: `mvn test -Dtest=AuditoriaCatalogoTest` en rojo
- [x] 5.2 Implementar el mapeo de los cinco tipos exactos del catálogo cerrado y la granularidad por operación, sin inventar eventos nuevos y persistiendo `publictoken.historia.denegado` de C-06; `login` y `logout` de C-04 quedan fuera del catálogo y la spec de C-04 no se modifica; verificación: `mvn test -Dtest=AuditoriaCatalogoTest,PublicTokenHistoriaDenegacionTest` en verde

## 6. Metadata prohibida — 7 escenarios de `audit-append-only-log` R4

- [x] 6.1 Escribir `AuditoriaMetadataTest` con lógica unitaria en H2 para: metadata permitida, metadata permitida de C-06 (`jti`, `scope`, `pacienteId`), rechazo de las 22 claves prohibidas agrupadas en credenciales/PII/clínica, rechazo anidado y variantes de caso/acento/separadores, y rechazo de `accessToken` y `Password-Hash` (`rechazaVariantesSinGuionBajoNiGuionDeClavesProhibidas()`); verificación: `mvn test -Dtest=AuditoriaMetadataTest` en rojo
- [x] 6.2 Implementar normalización a minúsculas sin acentos e **ignorando `_` y `-`**, con validación recursiva cerrada antes del insert y sin ampliar la lista de 22 claves; verificación: `mvn test -Dtest=AuditoriaMetadataTest` en verde y ninguna fila insertada en los casos rechazados

## 7. Instantánea del actor y trazabilidad — 5 escenarios de `audit-append-only-log` R5/R6

- [x] 7.1 Escribir `AuditoriaActorSnapshotTest` y `AuditoriaTrazabilidadTest` para actor `REAL` y los modos `SIMULADO`/`SYSTEM`/`PACIENTE`, estos dos últimos sin usuario, con `assertNull(actor_id)` para los tres modos del proveedor, inmutabilidad del snapshot ante cambios del usuario, fila con todos los campos y correlación distinta entre operaciones; verificación: `mvn test -Dtest=AuditoriaActorSnapshotTest,AuditoriaTrazabilidadTest` en rojo
- [x] 7.2 Implementar `actor_id` nullable con FK, copia inmutable de nombre/rol/`jti`, `modo`, `ip` nullable tratada como dato personal (solo cuando corresponda, nunca en `metadata`, nunca expuesta ni logueada), `dispositivo`, `motivo` y `correlation_id`; verificación: `mvn test -Dtest=AuditoriaActorSnapshotTest,AuditoriaTrazabilidadTest` en verde
- [x] 7.3 Confirmar que C-04 no se reabre; verificación: `mvn test -Dtest=AgendaSurfaceTest,CurrentActorProviderTest` en verde y 0 coincidencias de `Authorization`, `Bearer `, `SecurityContext`, `@RequestHeader` y `/auth` en superficie de agenda

## 8. Publicador real y delta MODIFIED — 9 escenarios de `audit-event-emission`

- [x] 8.1 Reescribir `AuditEventPublisherTest.noPersisteAuditoriaNiExponeApiDeConsulta()` como `persisteAuditoriaYSinApiDeConsulta()`, ajustar los dos escenarios del contrato de actor, el de operaciones y agregar el de denegación de HISTORIA; verificación: `mvn test -Dtest=AuditEventPublisherTest,PublicDeltaInvariantsTest` en rojo
- [x] 8.2 Implementar el publicador real inyectado en el puerto `AuditEventPublisher`, conservando `InMemoryAuditEventRecorder` solo como doble de tests unitarios H2 (`@Profile("test")`), con el publicador JDBC activo en dev y producción, preservando `correlationId` y campos del evento; verificación: `mvn test -Dtest=AuditEventPublisherTest,PublicDeltaInvariantsTest,AuditEventPublisherPerfilDevTest` en verde
- [x] 8.3 Verificar que no se crean rutas `/audit`, `/auditoria` ni permisos de lectura nuevos y que la verificación de cadena es solo servicio interno; verificación: búsqueda en controladores y configuración de seguridad con 0 coincidencias y escenario «Sin evidencia de cumplimiento» en verde
- [x] 8.4 Ajustar `PublicDeltaInvariantsTest` al contrato invertido leyendo siempre `openspec/specs/**` y nunca `openspec/changes/<change-activo>/`; verificación: `mvn test -Dtest=PublicDeltaInvariantsTest` en verde y 0 referencias al directorio activo del change

## 9. Suite completa y auditoría previa al archive

- [x] 9.1 Ejecutar la suite completa una sola vez al cerrar el último grupo; verificación: `mvn test` con 157 tests previos + los nuevos, 0 failures y 0 errors
- [x] 9.2 Completar el mapa escenario→test de la sección «Mapa de cobertura» y lista de escenarios sin test; verificación: 39/39 escenarios cubiertos (30 `audit-append-only-log` + 9 `audit-event-emission`) y lista vacía de escenarios sin test
- [x] 9.3 Ejecutar `openspec validate audit-append-only-log --strict` y `openspec validate --specs`; verificación: ambos válidos, con la deuda preexistente de `agenda` documentada si se ejecuta `--all --strict`

## Mapa de cobertura (a confirmar en el apply)

| Escenario del spec | Test |
|---|---|
| R1 Inserción confirmada y conservada | `AuditoriaLogAppendOnlyTest` |
| R1 Intento de actualización rechazado | `AuditoriaLogAppendOnlyTest` |
| R1 Intento de borrado rechazado | `AuditoriaLogAppendOnlyTest` |
| R1 Intento de vaciado rechazado | `AuditoriaLogAppendOnlyTest` |
| R2 Primera fila con valor génesis | `AuditoriaHashChainTest` |
| R2 Fila enlazada a su predecesora | `AuditoriaHashChainTest` |
| R2 Cadena válida con saltos de id y sin huecos lógicos | `AuditoriaHashChainTest` |
| R2 Verificación de cadena íntegra | `AuditoriaHashChainTest` |
| R2 Detección de una alteración externa | `AuditoriaHashChainTest` |
| R2 Publicaciones concurrentes sin bifurcar la cadena | `AuditoriaHashChainTest` |
| R3 Creación de turno | `AuditoriaCatalogoTest` |
| R3 Reprogramación de turno | `AuditoriaCatalogoTest` |
| R3 Sobreturno con override | `AuditoriaCatalogoTest` |
| R3 Creación de bloqueo | `AuditoriaCatalogoTest` |
| R3 Operación rechazada sin fila | `AuditoriaCatalogoTest` |
| R3 Denegación de HISTORIA con token público | `AuditoriaCatalogoTest` / `PublicTokenHistoriaDenegacionTest` |
| R4 Metadata permitida | `AuditoriaMetadataTest` |
| R4 Metadata permitida de C-06 | `AuditoriaMetadataTest` / `PublicTokenHistoriaDenegacionTest` |
| R4 Rechazo de credenciales y secretos | `AuditoriaMetadataTest` |
| R4 Rechazo de variantes con guion bajo, guion y camel case | `AuditoriaMetadataTest` |
| R4 Rechazo de datos personales | `AuditoriaMetadataTest` |
| R4 Rechazo de datos clínicos, firma y diagnóstico | `AuditoriaMetadataTest` |
| R4 Rechazo anidado y variantes de caso | `AuditoriaMetadataTest` |
| R5 Actor real persistido | `AuditoriaActorSnapshotTest` |
| R5 Modos del proveedor (SIMULADO/SYSTEM/PACIENTE sin usuario, `actor_id` nulo) | `AuditoriaActorSnapshotTest` |
| R5 Instantánea ante cambios posteriores | `AuditoriaActorSnapshotTest` |
| R6 Fila con todos los campos de trazabilidad | `AuditoriaTrazabilidadTest` |
| R6 Correlación entre operaciones | `AuditoriaTrazabilidadTest` |
| R7 Fallo de escritura de auditoría revierte la operación de negocio | `AuditoriaTransaccionTest` |
| R7 Rollback de negocio no deja fila de auditoría | `AuditoriaTransaccionTest` |
| MOD Actor fijo de dev/test | `AuditEventPublisherTest` |
| MOD Sin identidad verificada | `AuditEventPublisherTest` |
| MOD Actor real de C-04 | `AuditEventPublisherTest` |
| MOD Evento de sobreturno | `AuditEventPublisherTest` / `AuditoriaCatalogoTest` |
| MOD Operación rechazada | `AuditEventPublisherTest` / `AuditoriaCatalogoTest` |
| MOD Evento de denegación de HISTORIA | `AuditEventPublisherTest` / `PublicTokenHistoriaDenegacionTest` |
| MOD Los cuatro tipos de operación | `AuditoriaCatalogoTest` |
| MOD Recorder en memoria (persistencia real) | `AuditEventPublisherTest` |
| MOD Sin evidencia de cumplimiento (sin API de consulta) | `AuditEventPublisherTest` / `PublicDeltaInvariantsTest` |

**Escenarios sin test**: ninguno planificado; la lista se confirma en 9.2 antes del archive. Cobertura planificada: 39/39 escenarios (30 `audit-append-only-log` + 9 `audit-event-emission`).

## Workflow follow-up

- No ejecutar apply ni marcar C-05 o C-08 en `CHANGES.md` sin verificar la condición del PO (ajustes pre-apply y cobertura) y su aprobación para iniciar el apply.
- Tras el apply y la auditoría de cobertura, archivar el change y sincronizar los deltas a `openspec/specs/**` solo con aprobación del PO.
- Cuando el contrato principal pase de `SIMULADO` a real tras el sync, verificar de nuevo `PublicDeltaInvariantsTest` con el contrato invertido; el trabajo de ajuste se hace en apply (8.4), no en este propose.
- Después del archive, ejecutar `mvn test` completo y verificar que C-08 sigue `[ ]` hasta la decisión correspondiente.
- Cualquier evento nuevo del catálogo requiere decisión previa en `docs/decisiones/`, actualización de artefactos con `openspec-update-change` y su test; está prohibido inventarlo durante el apply.
