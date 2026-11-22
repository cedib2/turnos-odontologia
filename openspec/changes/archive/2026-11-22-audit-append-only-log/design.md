# Design

## Context

Ver `proposal.md` para la motivación y la decisión de alcance del PO (2026-11-22). Estado observable hoy: `AuditEventPublisher` es una interfaz con un adaptador en memoria `InMemoryAuditEventRecorder` (`@Profile("test")`) y un adaptador JDBC activo en dev y producción (`AuditPersistenceConfig`, `@Profile("!test")`); `AuditEvent` contiene `tipo`, `entidad`, `entidadId`, `actor` (`ActorSimulado`), `occurredAt`, `motivo`, `metadata` y `correlationId`; los servicios de agenda publican `turno.creado`, `turno.reprogramado`, `turno.sobreturno` y `bloqueo.creado`, y C-06 (ya archivado) publica `publictoken.historia.denegado` (`PublicTokenService.java:125`) a través del mismo puerto. C-04 ya entrega `AuthenticatedActor`, `ModoActor` y `RealActorProvider` con `REAL`/`SIMULADO`/`SYSTEM`/`PACIENTE`. Existen V001–V006, con V005/V006 ya aplicadas; `application-dev.yml` declara `spring.flyway.out-of-order: true`.

La gobernanza es CRITICO: este documento y el resto de los artefactos son planeación; no autorizan apply ni escritura de código.

## Goals / Non-Goals

**Goals:**
- Hacer que los cuatro eventos ya emitidos por la agenda y el evento de denegación de HISTORIA de C-06 produzcan evidencia persistente e inmutable en PostgreSQL.
- Garantizar la integridad de la cadena con SHA-256 sin clave y documentar su límite.
- Conservar la instantánea del actor de C-04 y la correlación ya contratada.
- Rechazar `metadata` con claves sensibles mediante una lista cerrada.
- Mantener los 157 tests verdes y las guardias de C-04/C-06 sin rebajarlas.

**Non-Goals:**
- Sin endpoint REST de consulta, sin permisos de lectura nuevos y sin filtros/paginación.
- Sin HMAC, sin gestor de secretos y sin variables de entorno nuevas con valores reales.
- Sin job de retención, archivado, borrado ni política de 10 años implementada.
- Sin eventos nuevos de firma clínica, cobro, exportación/respaldo/restore, roles ni configuración.
- Sin reabrir C-04: la superficie de agenda sigue sin `Authorization`, `Bearer`, `SecurityContext`, `@RequestHeader` ni `/auth`.
- Sin modificar `openspec/specs/**`, `CHANGES.md` ni el estado `[ ]` de C-08.
- Sin UI: `Decision: no (no UI)`.

## Decisions

### 1. Migración V003 y límites de inmutabilidad

`V003__audit_append_only_log.sql` crea `auditoria_log` con secuencia `auditoria_log_id_seq` y columnas: `id`, `fecha TIMESTAMPTZ`, `actor_id BIGINT NULL` (FK a `usuarios.id`), `actor_snapshot` inmutable (nombre, rol, `jti`), `modo VARCHAR(16)` con `CHECK`, `accion`, `entidad`, `entidad_id BIGINT NULL` (solo ausente para eventos sin entidad agregada concreta, como `publictoken.historia.denegado`), `ip INET NULL`, `dispositivo VARCHAR`, `motivo`, `metadata JSONB`, `correlation_id`, `hash_anterior`, `hash_actual`. Índices por `fecha`, `(entidad, entidad_id)`, `actor_id` y `correlation_id`.

Inmutabilidad en capa de datos: triggers `BEFORE UPDATE` y `BEFORE DELETE` que lanzan excepción, más un trigger **BEFORE TRUNCATE de nivel de sentencia** (`FOR EACH STATEMENT`): PostgreSQL no admite triggers `TRUNCATE` por fila. Se complementa con `REVOKE UPDATE, DELETE, TRUNCATE` al rol de aplicación. `INSERT` queda disponible. Alternativa descartada: solo validación en aplicación, porque no sobrevive a escrituras directas (RN-SE-02).

Límites documentados explícitamente en la evidencia: el `REVOKE` aplica al **rol de aplicación**; un superuser, el dueño de la tabla o cualquier rol con privilegios DDL puede sortearlo (desactivar triggers, `ALTER TABLE`, `DROP`) y `flyway repair` también puede eludirlo. Un vacuum/reindex no está cubierto por el trigger. Por eso el log se presenta como evidencia demostrable **dentro de la configuración descrita**, no como inmutabilidad absoluta ni garantía criptográfica absoluta; este es el límite de evidencia declarado (RN-SE-02/03).

`ip INET` se trata como **dato personal** conforme a la Ley 25.326: es nullable, se persiste solo cuando corresponda, no se copia a `metadata`, no se expone por API (la consulta REST está diferida) y no se loguea.

### 2. Hash encadenado SHA-256 sin clave

Orden canónico por `id` de secuencia, nunca por `fecha` (dos filas pueden tener la misma marca de tiempo). El payload canónico incluye `id`, `fecha`, `actor_snapshot`, `modo`, `accion`, `entidad`, `entidad_id`, `correlation_id`, `motivo`, `metadata` serialized de forma determinística y `hash_anterior`. Primera fila: `hash_anterior` = valor génesis documentado (cadena de ceros) y `hash_actual = SHA256(payload || genesis)`. Las siguientes usan el `hash_actual` inmediatamente anterior.

**Cadena válida**: para cada fila, `hash_anterior` es exactamente el `hash_actual` de la fila inmediatamente anterior **en orden de `id`**; la primera usa el génesis. No se exigen ids consecutivos: las secuencias de PostgreSQL tienen saltos por rollbacks y esos saltos son válidos siempre que no haya huecos lógicos (ninguna fila existente omitida en el orden por `id`).

**Serialización confirmada**: antes de leer la última fila por `id` y hacer el INSERT, la publicación adquiere `pg_advisory_xact_lock` con una clave fija, **dentro de la misma transacción** de la operación de negocio. El lock se libera automáticamente en commit o rollback. Así dos publicaciones concurrentes no pueden leer al mismo tiempo la misma cabeza de la cadena: la segunda espera a que la primera confirme (o revierta) y recién entonces lee el `hash_actual` ya insertado como su `hash_anterior`. Sin ese lock, ambas transacciones podrían leer la misma última fila y escribir dos sucesoras de la misma cabeza, bifurcando la cadena; la asignación de `id` por secuencia por sí sola no evita esa lectura concurrente. No se usa `REQUIRES_NEW`, async ni outbox en C-05.

La verificación recorre las filas ordenadas por `id`, recalcula el hash y falla en la primera discrepancia. Es un **servicio interno** (`AuditoriaIntegridadService` o equivalente), sin endpoint HTTP: ni `/audit` ni `/auditoria`, sin permisos de lectura nuevos.

Alternativa descartada: HMAC con gestor de secretos. No hay gestor aprobado en el proyecto; se usa SHA-256 y se documenta el límite: cualquiera con acceso de escritura privilegiada puede reconstruir una cadena coherente, por lo que el hash detecta alteraciones accidentales o no autorizadas dentro del rol de aplicación, pero no prueba autoría frente a un superuser.

### 3. Catálogo mínimo de eventos y granularidad

Catálogo cerrado en cinco eventos, con los nombres exactos ya presentes en el código y en `openspec/specs/audit-event-emission/spec.md`:

| Nombre exacto | Entidad | Operación de dominio |
|---|---|---|
| `turno.creado` | `turno` | Creación de turno (`AuditEvent.creacionTurno`) |
| `turno.reprogramado` | `turno` | Reprogramación (`ReprogramacionService`) |
| `turno.sobreturno` | `turno` | Sobreturno con override y motivo/actor (`SobreturnoService`) |
| `bloqueo.creado` | `bloqueo` | Creación de bloqueo (`BloqueoService`) |
| `publictoken.historia.denegado` | `publictoken` | Denegación de HISTORIA por identidad no verificada en C-06 (`PublicTokenService`) |

Granularidad: **una fila por operación de dominio**, no por cambio de campo. El `motivo` y los datos variables de la operación viajan en `motivo`/`metadata`, nunca como filas adicionales.

**Régimen de eventos nuevos (decisión PO 2026-11-22)**: cualquier parte del grupo que necesite registrar un evento nuevo debe proponerlo antes de implementarlo en `docs/decisiones/`, actualizar `spec.md`, `design.md` y `tasks.md` con `openspec-update-change` y agregar su test. Queda prohibido inventar eventos durante el apply. El catálogo de C-05 sigue cerrado en los cinco eventos de la tabla salvo decisión posterior del PO.

`login` y `logout` de C-04 quedan **fuera del catálogo de C-05**: no se auditan en este change y no se modifica la spec de C-04 para ese fin. Si se requiriera auditarlos, se trataría en un change posterior con su propia decisión PO.

El quinto evento no es nuevo: ya es observable en el código archivado de C-06 y usa el mismo `AuditEventPublisher`. Debe persistirse para no romper la denegación de HISTORIA de C-06 cuando C-05 reemplace el puerto por uno real. `AuditEvent.de(..., "publictoken", null, ...)` emite `entidad_id` nulo, por lo que la columna SHALL ser nullable para este caso sin entidad agregada concreta. Su semántica de identidad no verificada se conserva: ni el evento ni la fila infieren autenticación ni RBAC.

### 4. Instantánea del actor y proveedor real

La fila guarda `actor_id` (FK nullable) **más** una copia inmutable de nombre, rol y `jti` y el `modo`. La copia evita que cambios posteriores en `usuarios` alteren la evidencia. El proveedor real de C-04 (`AuthenticatedActor` / `RealActorProvider`) entrega los cuatro modos; en dev/test se conserva `ActorSimulado` con modo `SIMULADO` para `recepcionista()`. Las fábricas `ActorSimulado.system()` y `ActorSimulado.pacienteSinUsuario()` producen actores con `usuarioId = null` y modos `SYSTEM` y `PACIENTE` respectivamente; `ActorSimulado.paciente(usuarioId, nombre)` sigue disponible para el caso con usuario. `AuditoriaActorSnapshotTest.modosDelProveedor()` verifica los tres modos `SIMULADO`, `SYSTEM` y `PACIENTE` sin usuario con `actor_id` nulo en las tres filas. No se reintroduce lectura de headers ni seguridad en la superficie de agenda: el actor sigue llegando por `CurrentActorProvider` y su equivalente real; el ajuste de aserciones de modo en `AuthenticatedActorTest` y `CurrentActorProviderTest` refleja la decisión del PO sin rebajar las guardias.

### 5. Reemplazo del publicador y contrato MODIFIED

`AuditEventPublisher` pasa a tener un adaptador real que inserta en `auditoria_log` en la misma transacción de negocio. Se conservan `correlationId`, los campos obligatorios del evento y los tipos del catálogo. **Exclusividad de beans por perfil (decisión PO 2026-11-22)**: `JdbcAuditEventPublisher` está activo en dev y producción (`AuditPersistenceConfig`, `@Profile("!test")`) y `InMemoryAuditEventRecorder` solo en test (`@Profile("test")`); ya no conviven en dev. `AuditEventPublisherPerfilDevTest` carga un contexto mínimo con perfil `dev` y afirma exactamente un bean `AuditEventPublisher`, de tipo JDBC. Los tests H2 siguen usando el recorder en el límite del puerto, ahora inyectado por configuración de test; el publicador real se prueba con PostgreSQL/Testcontainers. La cobertura de integración queda en `Auditoria*Test` (`AuditoriaLogAppendOnlyTest`, `AuditoriaHashChainTest`, `AuditoriaTransaccionTest`, `AuditoriaCatalogoTest`, `AuditoriaMetadataTest`).

`AuditEventPublisherTest.noPersisteAuditoriaNiExponeApiDeConsulta()` se trata como **MODIFIED**: pasa a verificar persistencia real y ausencia de API de consulta, manteniendo la ausencia de superficie `/audit`. `PublicDeltaInvariantsTest` también debe ajustarse en el apply: hoy exige que la spec principal contenga `SIMULADO` y `MUST NOT crear tabla append-only` y que el test de auditoría contenga `CREATE TABLE AUDIT` y `/audit`. Como la spec principal solo se sincroniza en archive, esas dos cadenas siguen presentes hasta ese momento; la nueva guardia debe comprobar el contrato invertido leyendo siempre `openspec/specs/**`, nunca `openspec/changes/<change-activo>/`.

### 6. Metadata prohibida

Normalización: minúsculas, sin acentos, **ignorando también los guiones bajos (`_`) y los guiones (`-`)**, con comparación recursiva en objetos anidados. Lista cerrada de 22 claves, sin `(TBD)` y sin ampliación en este change:

- Credenciales/secretos: `password`, `password_hash`, `hash_password`, `secret`, `api_key`, `access_token`, `refresh_token`, `token`, `authorization`, `jwt`, `credentials`
- PII: `dni`, `documento_numero`, `telefono`, `email`, `direccion`, `fecha_nacimiento`
- Clínica/firma/diagnóstico: `firma`, `signature`, `diagnostico`, `historia_clinica`, `ficha_clinica`

La validación SHALL ocurrir antes de insertar: ante una clave prohibida se rechaza la publicación y no queda fila. Se validan claves, no texto libre, para mantener la lista acotada y predecible.

Caso permitido documentado: el evento de C-06 `publictoken.historia.denegado` ya emite `jti`, `scope` y `pacienteId` en `metadata`. Son identificadores internos o pseudónimos del flujo de tokens públicos, no credenciales ni datos clínicos; SHALL aceptarse sin modificar la lista de 22 claves prohibidas. `jti` en `metadata` no se confunde con `jwt` ni con `token`, que sí están prohibidos.

Con esta normalización, `accessToken` → `accesstoken` y `Password-Hash` → `passwordhash`, que coinciden con `access_token` → `accesstoken` y `password_hash` → `passwordhash`: ambos se rechazan sin agregar entradas a la lista.

### 7. Out-of-order y configuración

`spring.flyway.out-of-order: true` ya existe en `src/main/resources/application-dev.yml`. El apply debe verificarlo y usarlo tal cual; no se agrega una segunda declaración en `application.yml`, `application-test.yml` ni `@DynamicPropertySource`, y no se introduce base de producción con V006 registrada.

### 8. Sin delta de `agenda`

El único requisito de `agenda` vinculado a este change es «Sobreturno como excepción auditada», que exige un evento de auditoría emitido; el mismo evento se emite y ahora se persiste. No hay cambio de comportamiento observable en la agenda, por lo que no se crea `specs/agenda/spec.md` en este delta. Se evita además reactivar la deuda preexistente de warnings >500 chars de ese spec.

### 9. Proceso TDD recortado

Baseline: 157 tests verdes antes de tocar nada. Un test por escenario del spec: **39 escenarios** (30 de `audit-append-only-log` y 9 de `audit-event-emission`) en 10 requirements, 9 grupos y 26 tareas. Un ciclo por grupo (escribir tests del grupo → implementar lo mínimo → correr solo esos tests con `mvn test -Dtest=...`), sin triangulación extra y sin fase REFACTOR separada. Suite completa solo al cerrar el último grupo. Antes del archive: tabla escenario→test y lista de escenarios sin test. Después del archive: `mvn test` completo. PostgreSQL/Testcontainers para migración, trigger, `REVOKE`, hash chain, concurrencia y transacción compartida (reutilizar `PostgresFixture` y `PostgresFlywayTest`); H2 solo para lógica unitaria con el doble del puerto.

### 10. Confirmaciones obligatorias del PO (2026-11-22)

Registradas en `docs/decisiones/C-05-ajustes-pre-apply-PO.md`:

1. **Transacción compartida**: evento y operación de negocio comparten transacción (mismo `@Transactional`/conexión local). Sin `REQUIRES_NEW`, sin async y sin outbox en C-05; rollback de auditoría revierte el negocio y rollback del negocio elimina la fila de auditoría.
2. **Cadena con saltos y serialización**: cadena válida por predecesora inmediata en orden de `id`, sin exigir ids consecutivos, sin huecos lógicos; `pg_advisory_xact_lock` de clave fija en la misma transacción antes de leer la cabeza y hacer el INSERT, liberado en commit/rollback, evita la bifurcación por lectura concurrente de la misma cabeza.
3. **Normalización de metadata**: minúsculas, sin acentos y sin `_` ni `-` antes de comparar con las 22 claves prohibidas; `jti`, `scope` y `pacienteId` siguen permitidos.
4. **Trigger de TRUNCATE**: BEFORE TRUNCATE de nivel de sentencia (`FOR EACH STATEMENT`); PostgreSQL no tiene TRUNCATE row-level.
5. **Límite del REVOKE**: solo rol de aplicación; superusuario, dueño de tabla/DDL y `flyway repair` pueden sortearlo. No se afirma inmutabilidad absoluta: es límite de evidencia.
6. **Verificación de cadena**: servicio interno (`AuditoriaIntegridadService` o equivalente), sin endpoint HTTP, sin `/audit` ni `/auditoria` y sin permisos de lectura nuevos.
7. **`ip INET` como dato personal** (Ley 25.326): nullable, solo cuando corresponda, no copiada a `metadata`, no expuesta por API (REST diferida) y no logueada.
8. **Convivencia de tests**: H2 mantiene `InMemoryAuditEventRecorder` en el límite del puerto; el publicador real se prueba con PostgreSQL/Testcontainers en `Auditoria*Test`. Sin reemplazo global del bean que rompa los tests H2. **Revisado por el PO el 2026-11-22**: la convivencia dev+test quedó reemplazada por exclusividad de perfiles (`!test` vs `test`) para evitar dos beans y garantizar que dev ejerza persistencia real; ver la sección 11 y `docs/decisiones/C-05-ajustes-pre-apply-PO.md`.
9. **Catálogo y eventos nuevos**: cinco eventos cerrados; `login`/`logout` de C-04 fuera del catálogo de C-05 y sin modificar la spec de C-04; todo evento nuevo requiere decisión escrita, actualización de artefactos y su test antes de implementarlo.

## Risks / Trade-offs

- [El rol de aplicación podría conservar privilegios por el usuario de conexión usado en tests] → La prueba de `REVOKE` SHALL conectarse con un rol no superuser y verificar los tres permisos prohibidos.
- [Un superuser puede reescribir la cadena] → Documentar el límite del SHA-256 sin clave y del DDL en la evidencia; no presentarlo como garantía absoluta.
- [La instantánea con `actor_id` FK podría impedir borrar usuarios] → `actor_id` nullable y FK sin `ON DELETE CASCADE`; la copia de la instantánea es la fuente de la evidencia.
- [El evento `publictoken.historia.denegado` deja de persistirse al reemplazar el puerto] → Quinto tipo autorizado del catálogo; la prueba de denegación de HISTORIA de C-06 SHALL permanecer verde y su `metadata` (`jti`, `scope`, `pacienteId`) SHALL aceptarse sin ampliar la lista prohibida.
- [Las guardias de `PublicDeltaInvariantsTest` se quedan obsoletas tras el apply] → Actualizarlas en el mismo grupo del publicador real, leyendo siempre specs principales estables.
- [V003 entra out-of-order tras V005/V006] → `out-of-order=true` ya habilitado en dev/test; verificar sin duplicar configuración y sin base de producción.
- [Metadata con PII/clínica en `motivo`] → La lista prohibida cubre `metadata`; el `motivo` de sobreturno es texto operativo ya contratado y se conserva sin contenido clínico.

## Migration Plan

1. Verificar baseline de 157 tests verdes y Docker/Testcontainers disponibles.
2. Ejecutar V003 sobre PostgreSQL 17 con `out-of-order=true`; verificar trigger, `REVOKE` y `TRUNCATE`.
3. Sustituir el adaptador del puerto `AuditEventPublisher` por el publicador real; mantener el recorder solo para tests unitarios.
4. Verificar la cadena y la concurrencia en PostgreSQL; ejecutar la suite completa.
5. Actualizar la tabla de cobertura escenario→test; solo entonces, tras aprobación explícita del PO, ejecutar archive y sincronizar deltas. No se realiza en este propose.

No hay rollback automático: la migración es append-only y no se edita V003 una vez aplicada; ante fallo se corrige con una migración nueva dentro del rango reservado V001–V009, previa decisión PO.

## 11. Correcciones del pre-check autorizadas por el PO (2026-11-22)

Sin cambiar el número de requirements ni de escenarios (10 requirements / 39 escenarios) ni el mapa 39/39:

1. **Actores sin usuario**: `ActorSimulado.system()` emite `modo = "SYSTEM"` y `usuarioId = null`; se agrega `ActorSimulado.pacienteSinUsuario()` con `modo = "PACIENTE"` y `usuarioId = null`. `R5 Modos del proveedor` ahora exige `actor_id` nulo para `SIMULADO`, `SYSTEM` y `PACIENTE`.
2. **Guardias restauradas**: `AuditEventPublisherTest` recupera los ocho asserts de campos obligatorios y los dos conteos de eventos; `operacionRechazadaNoEmiteEventoDeConfirmacion()` ejerce un rechazo real de `BloqueoService` con el recorder conectado al puerto. `PublicDeltaInvariantsTest` recupera invariantes estables de la spec principal (`consulta de auditoría`, `MUST NOT`), presentes antes y después del sync.
3. **Exclusividad dev/test**: el publicador JDBC queda activo en `!test` y el recorder solo en `test`; se elimina la condición dependiente del orden de registro y se agrega `AuditEventPublisherPerfilDevTest`.
4. **Deuda documentada**: el adaptador de `AuthenticatedActor` a `CurrentActorProvider`, condicionado a proteger la agenda, queda para un change posterior (`docs/decisiones/C-05-deuda-adaptador-actor.md`); no bloquea C-05 y C-08 sigue `[ ]`.

El registro decisorio completo está en `docs/decisiones/C-05-ajustes-pre-apply-PO.md` y `.active-orchestrator-state.json` (`opsx.changes.C-05.pre_archive_correcciones`), con estado v31 sin archive.
