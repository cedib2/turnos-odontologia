# Decisión del PO — Ajustes pre-apply de C-05 `audit-append-only-log`

- **Fecha**: 2026-11-22
- **Decisor**: Lautaro Ellerbach, PO
- **Change**: C-05 `audit-append-only-log` · **Gobernanza**: CRITICO
- **Estado**: ajustes de planeación completados; apply autorizado **condicionalmente** y detenido antes del archive

## Contexto

El PO revisó el propose de C-05 (4/4 artefactos, Engram `opsx/c-05/propose`, estado v29) y autorizó el apply solo después de ajustar la planeación y verificar la cobertura escenario→test. Esta decisión registra esos ajustes; no ejecuta apply, no escribe código de producción ni tests y no modifica specs principales ni `CHANGES.md`.

## Decisión 1 — Régimen de eventos nuevos dentro del grupo C-05

- Antes de implementar un evento nuevo, cualquier parte del grupo debe:
  1. proponerlo por escrito en `docs/decisiones/`;
  2. actualizar `spec.md`, `design.md` y `tasks.md` con `openspec-update-change`;
  3. agregar su test con un test por escenario.
- Queda **prohibido inventar eventos durante el apply**.
- El catálogo de C-05 sigue **cerrado en cinco eventos**: `turno.creado`, `turno.reprogramado`, `turno.sobreturno`, `bloqueo.creado` y `publictoken.historia.denegado`. Solo una decisión posterior del PO puede ampliarlo.

## Decisión 2 — `login` y `logout` de C-04 fuera del catálogo

- `login` y `logout` de C-04 quedan **fuera del catálogo de C-05**.
- No se auditan en este change y **no se modifica la spec de C-04 con ese fin**.
- Si se requiriera auditarlos, se tratará en un change posterior con su propia decisión PO; no se agregarán por convención durante el apply de C-05.

## Decisión 3 — Transacción compartida

- El evento de auditoría y la operación de negocio comparten la **misma transacción de base de datos**: mismo límite `@Transactional` y misma conexión local.
- Si falla el INSERT de auditoría, se revierte todo: no queda fila de negocio ni de auditoría.
- Si la operación de negocio hace rollback después de publicarse el evento, no queda fila de auditoría.
- En C-05 están prohibidos `REQUIRES_NEW`, el envío asíncrono y el patrón outbox.
- Nuevo requirement en `specs/audit-append-only-log/spec.md` con dos escenarios PostgreSQL y `AuditoriaTransaccionTest` (un test por escenario).

## Decisión 4 — Cadena válida, saltos de `id` y serialización

- «Cadena válida»: para cada fila, `hash_anterior` es exactamente el `hash_actual` de la fila inmediatamente anterior **en orden de `id`**; la primera usa el génesis.
- **No se exigen ids consecutivos**: las secuencias de PostgreSQL tienen saltos por rollbacks. Sí se exige ausencia de huecos lógicos.
- Mecanismo confirmado: `pg_advisory_xact_lock` con clave fija, adquirido **dentro de la misma transacción**, antes de leer la última fila por `id` y hacer el INSERT; se libera en commit/rollback. Evita que dos publicaciones concurrentes lean la misma cabeza y bifurquen la cadena.
- Nuevo escenario PostgreSQL de cadena válida con saltos de `id` en `AuditoriaHashChainTest`; el escenario de publicaciones concurrentes se refuerza con «sin bifurcar la cadena».

## Decisión 5 — Normalización de claves prohibidas

- Antes de comparar con la lista prohibida, las claves de `metadata` se normalizan a minúsculas sin acentos **e ignorando también guiones bajos (`_`) y guiones (`-`)**, de forma recursiva en objetos anidados.
- La lista sigue siendo la misma de 22 claves; `accessToken` y `Password-Hash` son variantes de `access_token` y `password_hash`, no entradas nuevas.
- `jti`, `scope` y `pacienteId` del evento de C-06 siguen permitidos.
- Nuevo escenario de rechazo en `AuditoriaMetadataTest`.

## Decisión 6 — Confirmaciones de diseño

1. `V003` incluye trigger **BEFORE TRUNCATE de nivel de sentencia** (`FOR EACH STATEMENT`); PostgreSQL no tiene TRUNCATE row-level. Se conservan los triggers BEFORE UPDATE/DELETE.
2. El `REVOKE` aplica al **rol de aplicación**; superusuario, dueño de tabla/DDL y `flyway repair` pueden sortearlo. No se afirma inmutabilidad absoluta: se documenta como límite de evidencia.
3. La verificación de cadena es un **servicio interno** (`AuditoriaIntegridadService` o equivalente): sin endpoint HTTP, sin `/audit` ni `/auditoria` y sin permisos de lectura nuevos.
4. `ip INET` es **dato personal** (Ley 25.326): nullable, solo cuando corresponda, no se copia a `metadata`, no se expone por API (REST diferida) y no se loguea.
5. Los tests unitarios H2 existentes siguen usando el doble `InMemoryAuditEventRecorder` en el límite del puerto; el publicador real se prueba con PostgreSQL/Testcontainers. No se reemplaza globalmente el bean de forma que rompa los tests H2; la cobertura de integración queda en `Auditoria*Test`.

## Impacto en artefactos

- `openspec/changes/audit-append-only-log/proposal.md` — alcance, atomicidad y autorización condicional.
- `openspec/changes/audit-append-only-log/specs/audit-append-only-log/spec.md` — 7 requirements ADDED / 30 escenarios (nuevo requirement de transacción compartida; hash y metadata modificados; 4 escenarios nuevos, uno de ellos por el lock/concurrencia).
- `openspec/changes/audit-append-only-log/specs/audit-event-emission/spec.md` — 3 requirements MODIFIED / 9 escenarios sin cambios.
- `openspec/changes/audit-append-only-log/design.md` — serialización, límites, dato personal y convivencia de tests; sección 10 de confirmaciones.
- `openspec/changes/audit-append-only-log/tasks.md` — 9 grupos / 26 tareas, mapa de 39 escenarios a tests, sin escenarios sin test.
- `.active-orchestrator-state.json` — v30, `opsx.changes.C-05.ajustes_pre_apply_2026_11_22`.
- Engram — `opsx/c-05/pre-apply`.

## Condición del apply

El apply queda autorizado **condicionalmente** por el PO: debe iniciarse solo cuando el orquestador haya mostrado este delta y verificado la cobertura 39/39 escenarios con un test por escenario. No se ejecuta apply, archive, sincronización de specs principales ni marcado de `CHANGES.md` como parte de esta decisión.

## Correcciones del pre-check de archive (2026-11-22)

- **Decisor**: Lautaro Ellerbach, PO · **Fecha**: 2026-11-22 · **Gobernanza**: CRITICO
- **Autorización**: el PO autorizó explícitamente estas cuatro correcciones. No autorizan archive, sincronización de specs principales, marcado de `CHANGES.md`, commits ni push.
- **Estado previo**: apply 26/26, estado v31, 193 tests con 0 failures y 0 errors; el pre-check encontró gaps verificables (Engram #72).

### Corrección 1 — actor `SYSTEM` y nulidad del actor

`ActorSimulado.system()` pasa a emitir `usuarioId = null` y `modo = "SYSTEM"` (antes emitía `SIMULADO`). Se agrega `ActorSimulado.pacienteSinUsuario()` con `usuarioId = null` y `modo = "PACIENTE"`, sin alterar `paciente(usuarioId, nombre)`. `AuditoriaActorSnapshotTest.modosDelProveedor()` cubre `SIMULADO`, `SYSTEM` y `PACIENTE` sin usuario con `assertNull(actor_id)` en los tres casos. Las aserciones de `AuthenticatedActorTest` y `CurrentActorProviderTest` que esperaban `SIMULADO` para `system()` pasan a esperar `SYSTEM` y adicionalmente verifican `usuarioId()` nulo: es un cambio de valor esperado por decisión del PO, no un debilitamiento. El escenario `R5 Modos del proveedor` del delta lo declara explícitamente; requirements y escenarios siguen siendo 10/39.

### Corrección 2 — guardias restauradas

- `AuditEventPublisherTest` recupera `assertNotNull(evento.tipo())` (reforzado con `assertEquals("turno.creado", evento.tipo())`), `entidad`, `entidadId`, `occurredAt` no nulo, `motivo`, `metadata.origen`, `correlationId` no blank y los conteos `assertEquals(1, recorder.eventos().size())` y `assertEquals(3, recorder.eventos().size())`.
- `operacionRechazadaNoEmiteEventoDeConfirmacion()` ya no es vacuo: instancia `BloqueoService` real con repositorios simulados y el recorder conectado al puerto, ejerce el rechazo por falta de profesional y sillón mediante `assertThrows(IllegalArgumentException.class, ...)` y afirma que no quedó ningún evento ni evento de confirmación `bloqueo.creado`. No se movió al catálogo porque existe producción de rechazo de operación accesible sin inventar nuevo código; `AuditoriaCatalogoTest.operacionRechazadaSinFila()` se conserva como cobertura de rechazo por solapamiento.
- `PublicDeltaInvariantsTest` recupera invariantes de la spec principal presentes hoy y en el delta a sincronizar: `consulta de auditoría` y `MUST NOT`. Se verificó con búsqueda en `openspec/specs/audit-event-emission/spec.md` y en `openspec/changes/audit-append-only-log/specs/audit-event-emission/spec.md`. Las guardias nuevas (inversión, nombre de V003, prohibición de `openspec/changes/`) se mantienen; los conteos 13/31 de `agenda` no se tocan.
- **Por qué el escaneo de persistencia sigue solo en `src/main/java`**: los recursos contienen legítimamente `V003__audit_append_only_log.sql` con `CREATE TABLE`, por lo que buscar la sentencia de creación en `src/main/resources` daría un falso positivo. La ausencia de endpoints `/audit` y `/auditoria` se verifica en código Java y la existencia de `V003__audit_append_only_log.sql` se afirma por separado. Esto está documentado con un comentario en el test.

### Corrección 3 — exclusividad dev/test (reemplaza la convivencia antes acordada)

- **Fecha**: 2026-11-22 · **Decisor**: Lautaro Ellerbach, PO.
- **Motivo**: evitar dos beans `AuditEventPublisher` y garantizar que dev ejerza persistencia real; la resolución dependiente del orden de registro (`@ConditionalOnMissingBean` frente al recorder con `@Profile({"dev","test"})`) hacía que en dev ganara el recorder.
- **Cambio**: `InMemoryAuditEventRecorder` queda con `@Profile("test")` y `AuditPersistenceConfig` sigue activo en `!test` (dev y producción), sin `@ConditionalOnMissingBean`. Ya no hay solapamiento ni dependencia del orden de registro.
- **Evidencia**: `AuditEventPublisherPerfilDevTest` carga un contexto mínimo con `@ActiveProfiles("dev")`, un `JdbcTemplate` simulado y los componentes del paquete de auditoría; afirma exactamente un bean `AuditEventPublisher`, que es `JdbcAuditEventPublisher`, y ausencia de `InMemoryAuditEventRecorder`.
- **Reemplaza**: la convivencia dev+test del punto 5 de la Decisión 6 y la sección 5/ítem 8 de `design.md`. Los tests H2 siguen usando el recorder en el límite del puerto bajo perfil `test`; solo cambia su perfil de activación.

### Corrección 4 — deuda explícita

No se construye el adaptador de `AuthenticatedActor` a `CurrentActorProvider`. Queda registrado, con decisor y fecha, en `docs/decisiones/C-05-deuda-adaptador-actor.md`; está condicionado a proteger la agenda y se difiere a un change posterior. No bloquea C-05. Impacto en C-08: sigue `[ ]` y su condición de marcado requiere la decisión PO correspondiente.

---

**Decisor**: Lautaro Ellerbach, PO · **Fecha**: 2026-11-22
