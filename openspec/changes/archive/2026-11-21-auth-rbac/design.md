# Design

## Context

C-03 dejó sembrados `Usuario`, `Rol`, `Permiso` y `rol_permiso` con permisos planos, sin herencia, y ámbitos `propio`/`todos`/`config` (D7/D8/D11; no se reabren). C-07/C-08 cerraron los catálogos y la agenda con un actor fijo `SIMULADO`, `AuditEventPublisher` en memoria y una superficie HTTP de dev/test sin autenticación; C-08 sigue pendiente en `CHANGES.md`. Ver `proposal.md` para la motivación. El raíl operativo aprobado es `C-04 → C-06 → C-05` (`docs/decisiones/C-05-secuencia-y-condiciones-PO.md`). El stack es Java 17, Spring Boot 3.5.16, Spring Data JPA, Flyway, PostgreSQL 17/Testcontainers (H2 solo unitarios), paquete base `com.turnosodontologia` y build con `mvn` (sin `mvnw`).

## Goals / Non-Goals

**Goals:**
- Aislar la autenticación en una cadena de seguridad propia de `/auth/**`, sin alterar la semántica de agenda/catálogos en dev/test.
- Emitir, rotar y revocar credenciales con evidencia persistente en V002.
- Almacenar contraseñas con hash adaptativo **BCrypt** (confirmado por el PO el 2026-11-21) y cubrir el login sin token mediante una cookie CSRF anónima de doble envío.
- Preservar los 93 tests vigentes de C-07/C-08, incluidas las guardias de superficie y el contrato `SIMULADO`.
- Dejar registrados los `(TBD)` que el PO no resolvió, sin inventar valores por convención (RN-GL-03).

**Non-Goals:**
- Ejecutar el apply: este propose no escribe código, migraciones, configuración ni tests. El apply es CRITICO y requiere aprobación humana separada.
- Auditoría persistente, consulta REST de auditoría, tokens públicos/QR/WhatsApp, UI o `design-system/`.
- Secretos reales, despliegue, rate limit distribuido, herencia de permisos, mapeo fino de permisos por endpoint y acceso del odontólogo a agenda ajena.
- Proteger `/agenda`, `/turnos`, `/bloqueos`, `/sobreturnos` ni catálogos con RBAC: C-04 entrega la infraestructura RBAC de `/auth/**` y esa protección queda para un change posterior. Tampoco agregar un endpoint de registro de usuarios: el alta de staff usa el servicio interno existente `UsuarioService.registrar(...)` con hash BCrypt.

## Decisiones del PO (2026-11-21)

Fuente: `docs/decisiones/C-04-decisiones-auth-rbac.md`. Decisor: `Lautaro Ellerbach, PO` (confirmado el 2026-11-21). Los valores numéricos son **metas iniciales, no normativas**.

| # | Decisión reflejada |
|---|---|
| 1 | Paths `/auth/*` literales; cookie de sesión `HttpOnly`/`Secure`/`SameSite` con CSRF anónimo de doble envío en `GET /auth/csrf` y header `X-CSRF-Token` en login. |
| 2 | JWT access + refresh opaco persistido en V002, con rotación y detección de reutilización. |
| 3 | Access 15 min; refresh 7 días con rotación; logout revoca sesión y familia. |
| 4 | Sí a `spring-boot-starter-security`; guardias acotadas solo al paquete `auth`; agenda/catálogos sin autenticación; 93 tests verdes. |
| 5 | Secretos por variables de entorno; `.gitignore` ya existente; `.env.example` sin valores; ninguna clave en el repo. |
| 6 | Rate limiting en memoria por combinación IP + usuario: 5 fallos / 15 min desde el primer fallo, 429 con `Retry-After`, login exitoso reinicia el contador; no distribuido, se esquiva rotando de IP (cada IP nueva con el mismo usuario tiene contador propio), no se comparte entre réplicas y se pierde al reiniciar el proceso. |
| 7 | Actor con `modo ∈ {REAL, SIMULADO, SYSTEM, PACIENTE}` y snapshot inmutable; proveedor real separado conservando `CurrentActorProvider` simulado. |
| 8 | V002 con revocación por sesión y `jti`; `spring.flyway.out-of-order=true` en dev/test antes del apply. |
| 9 | Alta de usuarios de staff con el servicio interno existente `UsuarioService.registrar(...)` y `hash_password` generado por BCrypt; sin endpoint de registro. Los fixtures de test usan ese servicio. |

## Ajustes del PO al propose (2026-11-21)

Cuatro ajustes pedidos antes del apply; no cambian el alcance ni habilitan escritura de código:

| # | Ajuste |
|---|---|
| 1 | Hash adaptativo de contraseñas **BCrypt**, confirmado por el PO el 2026-11-21 (RN-GL-03). Sin texto plano ni contraseñas/hashes en logs. |
| 2 | Operación protegida de referencia para 403: `GET /auth/me` (`propio`) y `GET /auth/usuarios/{id}` (identidad de staff, sin datos clínicos) para `todos`/administrador. Definiciones de `propio` (coincide con el `sub` del access token), `todos` y `config`. C-04 entrega infraestructura RBAC pero aún no protege la agenda. |
| 3 | `GET /auth/csrf` emite cookie CSRF anónima de doble envío distinta de la de sesión; `POST /auth/login` exige `X-CSRF-Token`. Usuario inexistente, contraseña incorrecta e inactivo dan la misma 401 sin tokens, sesión ni revelado de causa. La cookie de sesión sigue siendo `HttpOnly`/`Secure`/`SameSite`. |
| 4 | Rate limit por combinación IP + usuario (no contadores que se suman ni "cualquiera por separado"), login exitoso reinicia el contador, ventana desde el primer fallo; el escenario de "no distribuido" pasa a ser verificación documental, no un test de dos instancias. La documentación debe decir que es en memoria, se esquiva rotando de IP (cada IP nueva con el mismo usuario tiene contador propio), no se comparte entre réplicas y se pierde al reiniciar el proceso. |
| 5 | Alta de usuarios de staff mediante `UsuarioService.registrar(...)` con `hash_password` BCrypt, sin endpoint de registro; los fixtures de test usan el servicio interno. |

## Decisions

- **Cadena de seguridad acotada (PO, 2026-11-21).** Registrar un `SecurityFilterChain` con `securityMatcher("/auth/**")` en el paquete `auth`, sin cadena global. No se añaden filtros ni `OncePerRequestFilter` que intercepten `/agenda`, `/turnos`, `/bloqueos`, `/sobreturnos` ni catálogos. En slices MVC de agenda/catálogos se excluye `SecurityAutoConfiguration` o se provee una cadena de test permisiva, de modo que la semántica dev/test sin autenticación no cambie. Alternativa descartada: cadena global con `permitAll` para agenda; añade filtros a rutas hoy inocuas y amplía la superficie a auditar.
- **Guardias y los 93 tests (PO, 2026-11-21).** `AgendaSurfaceTest` deja de leer `pom.xml` y restringe el escaneo de `src/main/java` excluyendo `com/turnosodontologia/auth`; mantiene intacta la lista de literales prohibidos para el resto del código. `CurrentActorProviderTest` quita `pom.xml` de sus raíces y excluye el paquete `auth` del escaneo, conservando el resto de prohibidos sobre agenda/catálogos y resources. Se agrega un test positivo: los mappings `/auth/**` existen solo en el paquete auth y la cadena está restringida por `securityMatcher`. El cambio es el mínimo necesario: no se eliminan literales ni se debilita la garantía de agenda; `AuditEventPublisherTest.noPersisteAuditoriaNiExponeApiDeConsulta()` debe seguir verde (sin `CREATE TABLE AUDIT`, `/auditoria` ni `/audit`). La línea base de 93 tests se verifica antes y después del apply; los tests nuevos se cuentan aparte.
- **Secretos y logs (PO, 2026-11-21).** Clave de firma y cualquier credencial se leen de variables de entorno; `.env.example` contiene solo nombres de variables, sin valores; `.gitignore` ya cubre `.env`, `.env.*` y `!.env.example`. Se prohíbe registrar contraseñas, tokens, claves o hashes de refresh en logs y mensajes de error. Alternativa descartada: valores por defecto en `application*.properties` o gestor de secretos externo (no aprobado, costo adicional).
- **Hash adaptativo de contraseñas (PO, 2026-11-21).** Las contraseñas se persisten con **BCrypt**, confirmado por el PO el 2026-11-21 (RN-GL-03); se proscribe el texto plano y cualquier escritura de contraseñas o hashes en logs, mensajes de error o excepciones. Los tests verifican el formato de hash BCrypt del valor persistido. Alternativa descartada: hash rápido de propósito general (MD5/SHA), por no ser adaptativo.
- **Alta de usuarios de staff (PO, 2026-11-21).** El alta se hace con el servicio interno existente `UsuarioService.registrar(...)`, generando `hash_password` con BCrypt; **no se agrega endpoint de registro** (por ejemplo, `POST /auth/registro`). Los fixtures de test crean usuarios con ese servicio.
- **CSRF anónimo para login (PO, 2026-11-21).** Como no existe sesión antes del login, `GET /auth/csrf` emite una cookie CSRF anónima de doble envío, legible por el cliente y distinta de la cookie de sesión; `POST /auth/login` exige el valor reflejado en `X-CSRF-Token` (403 sin emisión de tokens si falta o no coincide). Un login válido rota/reemplaza el valor CSRF. La cookie de sesión posterior mantiene `HttpOnly`/`Secure`/`SameSite` y sigue siendo ilegible desde JS. Usuario inexistente, contraseña incorrecta y usuario inactivo comparten la misma respuesta 401, sin tokens, sesión ni pista sobre la causa.
- **Uniformidad temporal del login (PO, 2026-11-21).** Para que la 401 no filtre si el usuario existe, el login ejecuta la verificación BCrypt también cuando el usuario no existe o está inactivo, usando un hash dummy fijo y la misma contraseña enviada. No hay `return` anticipado antes de la comparación de hash. El test verifica estructuralmente esa llamada (por ejemplo, instrumentando el `PasswordEncoder` o un contador de verificaciones), no una medición de milisegundos frágil. Alternativa descartada: comparar solo el cuerpo/estado de la respuesta, que no cubre timing side-channel.
- **Rate limit en memoria (PO, 2026-11-21).** Contador **por combinación IP + usuario**, no contadores independientes que se suman ni la regla "cualquiera por separado": misma IP con usuario distinto y distinta IP con mismo usuario mantienen contadores separados. Ventana de 15 minutos medida desde el primer fallo de esa combinación, umbral de 5 fallos y 429 con `Retry-After`; un login exitoso reinicia ese contador. Se documenta explícitamente en specs/design/decisiones como no distribuido: **se esquiva rotando de IP** (cada IP nueva con el mismo usuario tiene contador propio), no se comparte entre réplicas y se pierde al reiniciar el proceso. El escenario "Límite no distribuido" verifica esa documentación, no el comportamiento de dos instancias. Alternativa descartada: Redis u otro store compartido (servicio nuevo, fuera de presupuesto y de este change).
- **Contrato de actor (PO, 2026-11-21).** Tipo común con `modo` enumerado y snapshot inmutable (identificador, nombre, rol, `jti`, modo) construido al autenticar. Proveedor real separado del `CurrentActorProvider` simulado; este último y `ActorSimulado` siguen intactos para C-08. `PACIENTE` queda reservado para C-06 y no habilita login de staff; el modo `SIMULADO` sigue sin acreditar identidad ni autorización hasta C-05.
- **Persistencia V002 (PO, 2026-11-21).** `auth_sesiones` (`usuario_id`, `jti` UNIQUE, `estado`, `ip_origen`, `user_agent`, `created_at`, `expires_at`, `last_used_at`, `revoked_at`), `auth_tokens_refresh` (`hash_token` UNIQUE, `jti`, `issued_at`, `expires_at`, `used_at`, `rotated_from_id`, `familia_id`) y tabla opcional de `jti` revocados. Sin borrado físico; índices, checks y FK coherentes con V001/V006. Solo se persiste el hash del refresh, nunca el valor en claro. `spring.flyway.out-of-order=true` en dev/test porque V005/V006 ya están registradas. Alternativa descartada: numerar V007, porque `CHANGES.md` reserva V002 para C-04.
- **RBAC sobre datos existentes (C-03 D7/D8/D11).** Se consume `rol_permiso` tal cual; sin herencia, sin nuevos roles y sin cuenta de usuario para el rol `paciente`. Deny por rol y por ámbito se resuelve contra la matriz sembrada, sin reabrir decisiones cerradas. `propio` = identificador del recurso igual al `sub` del access token; `todos` = mismo `recurso`/`accion` sobre cualquier titular; `config` = permisos de configuración. Operación protegida de referencia para los 403: `GET /auth/me` en el caso `propio` y `GET /auth/usuarios/{id}` (identidad de otro usuario de staff, sin datos clínicos) para `todos`/administrador. C-04 entrega la infraestructura RBAC de `/auth/**` pero aún NO protege la agenda; `/agenda`, `/turnos`, `/bloqueos`, `/sobreturnos` y catálogos siguen sin autenticación en dev/test y su protección queda para un change posterior.
- **Superficie HTTP.** Endpoints indicativos `GET /auth/csrf`, `POST /auth/login`, `POST /auth/refresh`, `POST /auth/logout`, `GET /auth/me` y `GET /auth/usuarios/{id}` (solo identidad de staff, sin datos clínicos), con paths literales bajo `/auth/*`. Respuestas de error simples y consistentes con los controladores actuales; no se introduce un formato global nuevo.

## Risks / Trade-offs

- **[Spring Security altera slices MVC] → Mitigación:** cadena con `securityMatcher`, exclusión de auto-configuración en slices y verificación explícita de los 93 tests más tests de inventario de rutas.
- **[Guardias demasiado amplias o demasiado débiles] → Mitigación:** excluir solo `pom.xml` y el paquete auth, conservar los literales prohibidos para agenda/catálogos y agregar un test positivo de confinamiento.
- **[V002 se clasifica IGNORED] → Mitigación:** habilitar out-of-order en dev/test antes del apply y verificar con PostgreSQL/Testcontainers sobre una base con V006 registrada.
- **[Fuga de secretos por logs o excepciones] → Mitigación:** variables de entorno, `.env.example` sin valores, sanitización de logs y test de ausencia de literales sensibles, incluidas contraseñas y hashes de contraseña.
- **[Hash adaptativo] → Mitigación:** BCrypt confirmado por el PO el 2026-11-21 (RN-GL-03); el test verifica el formato BCrypt del valor persistido y que el alta usa `UsuarioService.registrar(...)` sin endpoint de registro.
- **[CSRF anónimo mal implementado] → Mitigación:** cookie de doble envío distinta de la de sesión, exigencia de `X-CSRF-Token` en login y rotación tras el éxito; la cookie de sesión conserva `HttpOnly`/`Secure`/`SameSite`.
- **[Reutilización de refresh no detectada] → Mitigación:** unicidad de hash, marca `used_at` atómica y revocación de familia; pruebas de concurrencia/rotación en PostgreSQL.
- **[Límite no distribuido] → Mitigación:** documentarlo en specs/design como en memoria, no distribuido y evadible rotando de IP (cada IP nueva con el mismo usuario tiene contador propio), sin compartir entre réplicas ni conservar tras reiniciar el proceso; no presentarlo como protección de producción multi-instancia. La verificación del escenario es documental, no un test de dos instancias.
- **[C-05 se vea afectado] → Mitigación:** no tocar `audit-event-emission` ni introducir literales de auditoría persistente; el actor real se agrega como contrato separado sin cambiar el emisor simulado.

## Migration Plan

El apply fue aprobado por el PO el 2026-11-21 con el delta ajustado. Para su ejecución: (1) bloquear el baseline de 93 tests verdes; (2) agregar dependencia, configuración acotada y `.env.example`; (3) crear V002 y verificar Flyway out-of-order en PostgreSQL/Testcontainers; (4) desarrollar con TDD estricto la lógica de hash BCrypt (confirmada por el PO), el alta de staff vía `UsuarioService.registrar(...)` sin endpoint de registro, CSRF anónimo, tokens, RBAC, rate limit y actor; (5) ajustar las guardias al mínimo y ejecutar la suite completa + `openspec validate auth-rbac --strict`; (6) registrar evidencia sin marcar `C-08` ni tocar C-05/C-06, deteniéndose antes del archive. No existe base de producción; rollback en dev/test implica recrear la base, sin borrado físico de datos de usuario.

## Open Questions

- Esquema exacto de claims del access, algoritmo de firma y rotación de claves `(TBD)`.
- Versionado y verbos definitivos de los endpoints más allá de los paths indicativos `(TBD)`.
- Nombre concreto de la cookie CSRF y valor específico de `SameSite` `(TBD)`; el mecanismo (doble envío, `GET /auth/csrf`, `X-CSRF-Token`, rotación en login) ya está definido.
- Mapeo fino de permisos por endpoint y acceso del odontólogo a agenda ajena `(TBD)`; no se inventan. La protección RBAC de agenda queda para un change posterior.
- Estrategia futura de rate limit distribuido `(TBD)`, fuera de este change.
