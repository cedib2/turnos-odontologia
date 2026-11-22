# Tasks

Criterios de aceptación: `specs/auth-rbac/spec.md` y el requisito aditivo de `specs/agenda/spec.md`. Detalle técnico: `design.md`. El apply de C-04 fue **aprobado por el PO el 2026-11-21** con el delta ajustado; las tareas fueron ejecutadas, verificadas y marcadas `[x]`, sin reabrir el apply.

> Proceso vigente a partir del ajuste de proceso por tiempos del PO (2026-11-21, `Lautaro Ellerbach, PO`; ver `docs/decisiones/C-04-ajuste-proceso-tiempos.md`): (1) un test por escenario del spec, sin casos de triangulación extra salvo para **rate limiting** y **concurrencia del refresh**; (2) un ciclo por grupo — escribir tests, implementar, correr una vez — sin fases `REFACTOR` separadas; (3) durante el desarrollo correr solo los tests del grupo con `mvn test -Dtest=...`, y la suite completa al cerrar **T10** y en **T11**; (4) compartir un único contenedor PostgreSQL entre las clases de test, ya disponible en `PostgresFixture` sin acción correctiva pendiente. H2 solo para lógica unitaria; PostgreSQL/Testcontainers para V002, unicidad, rotación y concurrencia. Este proceso rige para cualquier retrabajo de C-04 desde ahora: **el apply ya ejecutado (T1..T11) no se rehace** y sus checkboxes permanecen `[x]`. Invariantes: cobertura de cada escenario del spec, 93 tests previos en verde en T11 y `openspec validate auth-rbac --strict` válido. `AuditEventPublisherTest.noPersisteAuditoriaNiExponeApiDeConsulta()` debe permanecer verde hasta C-05.

## 1. T1 — Baseline, configuración y secretos

- [x] 1.1 RED: escribir tests de inventario que fallen si `.env.example` no existe o contiene valores, y si `.gitignore` no ignora `.env`/`.env.*` con excepción `!.env.example`; casos: (a) archivo ausente, (b) variable con valor asignado; verificar rojo.
- [x] 1.2 GREEN: crear `.env.example` solo con nombres de variables sin valores, manteniendo el `.gitignore` ya existente; verificar los tests del grupo en verde.
- [x] 1.3 TRIANGULATE: agregar casos de ausencia de contraseñas, JWT, claves de firma y hashes de refresh en logs de login/refresh/logout, y de rechazo si aparece un literal de clave en `src/main`; verificar rojo→verde.
- [x] 1.4 REFACTOR: centralizar los nombres de variables de entorno y la sanitización de logs sin cambiar el comportamiento; verificar la suite del grupo en verde.

## 2. T2 — Migración V002 y Flyway out-of-order

- [x] 2.1 RED: escribir tests PostgreSQL/Testcontainers que fallen por ausencia de constraints ante (a) `jti` duplicado y (b) `hash_token` duplicado en `auth_tokens_refresh`; verificar rojo.
- [x] 2.2 GREEN: crear `V002__auth_rbac.sql` con `auth_sesiones`, `auth_tokens_refresh` y tabla de `jti` revocados, con FK a `usuarios`, índices, checks y sin borrado físico; verificar rojo→verde.
- [x] 2.3 TRIANGULATE: agregar casos de (a) base dev/test con V006 registrada donde `spring.flyway.out-of-order=true` aplica V002, (b) base nueva que aplica V002 en orden y (c) revocación que registra sesión y `jti` sin `DELETE`; verificar.
- [x] 2.4 REFACTOR: revisar la migración frente a V001/V006 y la configuración dev/test; verificar que la suite de migraciones existente y la nueva pasan con PostgreSQL real.

## 3. T3 — Login, hash adaptativo, cookie segura y CSRF

- [x] 3.1 RED: escribir tests web con H2 para (a) login válido con CSRF que emite access JWT y refresh opaco ligados a una sesión, (b) usuario inexistente, (c) contraseña incorrecta y (d) usuario inactivo; las tres causas inválidas deben dar la misma 401, sin tokens, sin sesión y sin revelar la causa; verificar rojo.
- [x] 3.2 GREEN: implementar `GET /auth/csrf`, `POST /auth/login` con exigencia de `X-CSRF-Token` y la emisión mínima dentro del paquete `auth`; verificar rojo→verde.
- [x] 3.3 TRIANGULATE: agregar casos de (a) `GET /auth/csrf` sin sesión que emite cookie CSRF anónima de doble envío, distinta de la de sesión, legible pero sin valor autorizativo, (b) login sin CSRF y con CSRF incorrecto que responde 403 sin tokens ni sesión, (c) login válido con CSRF que emite el par y rota/reemplaza la cookie CSRF rechazando el valor anterior, (d) cookie de sesión con `HttpOnly`, `Secure` y `SameSite` no legible desde JS, y (e) `POST /auth/refresh` sin CSRF que responde 403 sin rotar; verificar.
- [x] 3.4 RED (hash): escribir tests para (a) alta de usuario de staff vía `UsuarioService.registrar(...)` que persiste un valor no plano con formato BCrypt y (b) login válido que autentica contra ese hash sin reemplazarlo por texto plano; verificar rojo.
- [x] 3.5 GREEN (hash): implementar el hash adaptativo con **BCrypt**, confirmado por el PO el 2026-11-21 (RN-GL-03), generando `hash_password` en `UsuarioService.registrar(...)`; verificar rojo→verde.
- [x] 3.6 TRIANGULATE (hash): agregar casos de (a) una misma contraseña que produce hashes BCrypt distintos y (b) logs de login/refresh/logout sin contraseñas ni hashes de contraseña; verificar.
- [x] 3.7 REFACTOR: extraer la construcción de la respuesta, las cookies y el hash sin alterar el contrato; verificar la suite del grupo en verde.
- [x] 3.8 Verificar que no existe endpoint de registro de usuarios (por ejemplo, `POST /auth/registro`) y que los fixtures de test crean usuarios de staff con `UsuarioService.registrar(...)` y `hash_password` BCrypt; verificar.
- [x] 3.9 RED/GREEN/TRIANGULATE: instrumentar la verificación de credenciales para que usuario inexistente, contraseña incorrecta y usuario inactivo ejecuten BCrypt (los dos primeros casos sin usuario activo usan hash dummy), sin retorno anticipado que filtre la causa por tiempo de respuesta; verificar con contador/espía, no con medición de milisegundos.

## 4. T4 — Access token expirado

- [x] 4.1 RED: escribir tests unitarios H2 para (a) access con `exp` vencido que responde 401 y (b) refresh opaco presentado como access que responde 401; verificar rojo.
- [x] 4.2 GREEN: implementar la validación de expiración y el rechazo de refresh como access; verificar rojo→verde.
- [x] 4.3 TRIANGULATE: agregar casos de (a) token con firma inválida y (b) access sin `jti`, ambos rechazados sin efectos; verificar.
- [x] 4.4 REFACTOR: unificar la validación de access en un único punto del paquete auth; verificar la suite del grupo en verde.

## 5. T5 — Refresh con rotación y detección de reutilización

- [x] 5.1 RED: escribir tests unitarios H2 y de integración PostgreSQL/Testcontainers para (a) rotación exitosa que emite nuevo par, marca `used_at` y conserva la familia y (b) refresh vencido que responde 401 sin emitir tokens; verificar rojo.
- [x] 5.2 GREEN: implementar `POST /auth/refresh` con rotación atómica y persistencia del hash; verificar rojo→verde.
- [x] 5.3 TRIANGULATE: agregar casos de (a) reuso de un refresh ya rotado que revoca la familia y (b) dos rotaciones concurrentes sobre el mismo token donde solo una se confirma; verificar en PostgreSQL.
- [x] 5.4 REFACTOR: simplificar la transición de estados del refresh sin debilitar la detección; verificar la suite del grupo en verde.

## 6. T6 — Logout con revocación de sesión y familia

- [x] 6.1 RED: escribir tests H2/PostgreSQL para (a) logout que revoca sesión, familia de refresh y `jti` del access y (b) uso posterior del access que responde 401; verificar rojo.
- [x] 6.2 GREEN: implementar `POST /auth/logout` con CSRF y las revocaciones persistentes; verificar rojo→verde.
- [x] 6.3 TRIANGULATE: agregar casos de (a) logout sin CSRF que no revoca nada y (b) logout sobre sesión ajena que no afecta otra familia; verificar.
- [x] 6.4 REFACTOR: consolidar la revocación en una sola operación; verificar la suite del grupo en verde.

## 7. T7 — Denegación por rol y por ámbito

- [x] 7.1 RED: escribir tests unitarios H2 sobre la matriz sembrada para (a) rol sin permiso que responde 403 en `GET /auth/me` (caso `propio`), (b) `GET /auth/me` permitido con `propio` porque el identificador coincide con el `sub` del token, (c) ámbito `propio` insuficiente en `GET /auth/usuarios/{id}` de otro titular que responde 403 sin revelar la identidad y (d) ámbito `todos` suficiente que autoriza `GET /auth/usuarios/{id}` y devuelve solo identidad de staff, sin datos clínicos; verificar rojo.
- [x] 7.2 GREEN: implementar la evaluación de `rol_permiso` y de ámbitos sin herencia con las definiciones de `propio` (coincidencia con el `sub` del access token), `todos` y `config`; verificar rojo→verde.
- [x] 7.3 TRIANGULATE: agregar casos de (a) un rol que no hereda permiso de otro en `GET /auth/usuarios/{id}`, (b) rol `paciente` sin `Usuario` que no puede hacer login de staff y (c) permiso `config` que no autoriza la consulta de identidad ajena; verificar.
- [x] 7.4 REFACTOR: aislar la decisión de autorización en un componente del paquete auth; verificar la suite del grupo en verde.

## 8. T8 — Rate limiting en memoria

- [x] 8.1 RED: escribir tests unitarios H2 para (a) 5 fallos en 15 minutos para la misma combinación IP + usuario que responden 429 con `Retry-After`, (b) ventana medida desde el primer fallo, tras la cual se vuelven a evaluar credenciales, y (c) 4 fallos seguidos de un login exitoso que reinician el contador, de modo que 4 nuevos fallos no producen 429; verificar rojo.
- [x] 8.2 GREEN: implementar en el paquete auth el contador en memoria por combinación IP + usuario, sin contadores independientes que se suman ni regla de "cualquiera por separado", con reinicio en login exitoso; verificar rojo→verde.
- [x] 8.3 TRIANGULATE: agregar casos de (a) misma IP con usuario distinto y (b) distinta IP con mismo usuario, ambos con contadores separados que no heredan fallos, y (c) verificación documental: GIVEN el documento de diseño/decisiones, WHEN se inspecciona, THEN declara explícitamente que el límite es en memoria y no distribuido, que se esquiva rotando de IP (cada IP nueva con el mismo usuario tiene contador propio), que no se comparte entre réplicas y que se pierde al reiniciar el proceso (sin test de dos instancias); verificar.
- [x] 8.4 REFACTOR: extraer la configuración de umbral/ventana sin cambiar el comportamiento; verificar la suite del grupo en verde.

## 9. T9 — Contrato de actor con modo y snapshot

- [x] 9.1 RED: escribir tests unitarios H2 para (a) snapshot `REAL` inmutable con identificador, nombre, rol, `jti` y modo tras login y (b) proveedor simulado vigente que sigue devolviendo `recepcionista`/`system` con `modo=SIMULADO`; verificar rojo.
- [x] 9.2 GREEN: incorporar el tipo de actor con `modo ∈ {REAL, SIMULADO, SYSTEM, PACIENTE}` y el proveedor real separado; verificar rojo→verde.
- [x] 9.3 TRIANGULATE: agregar casos de (a) modos `SYSTEM` y `PACIENTE` representables sin habilitar login de staff y (b) `AuditEventPublisher` inalterado en modo `SIMULADO`; verificar.
- [x] 9.4 REFACTOR: ordenar los proveedores por perfil sin tocar `CurrentActorProvider`; verificar la suite del grupo en verde.

## 10. T10 — Cadena acotada, guardias y preservación de C-07/C-08

- [x] 10.1 RED: escribir tests positivos que fallen hoy para (a) cadena de seguridad restringida con `securityMatcher("/auth/**")` y ausencia de cadena global y (b) mappings `/auth/**` existentes solo en el paquete auth; verificar rojo.
- [x] 10.2 GREEN: agregar `spring-boot-starter-security`, registrar la cadena acotada y excluir `SecurityAutoConfiguration` o proveer cadena permisiva en slices MVC de agenda/catálogos; verificar rojo→verde de los tests nuevos.
- [x] 10.3 TRIANGULATE: estrechar `AgendaSurfaceTest` y `CurrentActorProviderTest` al mínimo (dejar de escanear `pom.xml` y el paquete auth), conservando los literales prohibidos para agenda/catálogos; verificar los 2 casos de superficie sin autenticación y sin headers/cookies junto con `AuditEventPublisherTest`.
- [x] 10.4 REFACTOR: revisar que ninguna guardia se debilitó ni se evadió literal; verificar que la suite completa del grupo permanece en verde.

## 11. T11 — Verificación integral y cierre

- [x] 11.1 Ejecutar la suite completa con `mvn test` y verificar que los **93 tests vigentes** de C-07/C-08 siguen verdes, con la nueva suite contada aparte; registrar el conteo como evidencia.
- [x] 11.2 Ejecutar `openspec validate auth-rbac --strict` y verificar que el change queda válido, con deltas y escenarios GIVEN/WHEN/THEN completos.
- [x] 11.3 Verificar que no se modificaron C-05/C-06/C-07/C-08, `audit-event-emission` (sigue `SIMULADO`), UI ni secretos reales, y que `CHANGES.md` permanece sin marcar hasta la aprobación y verificación del apply. No editar `openspec/specs/**` durante el apply; la sincronización de los deltas ocurre en el archive.
- [x] 11.4 Verificar en los deltas y el diseño que C-04 declara que entrega infraestructura RBAC pero aún no protege la agenda, que **BCrypt** fue confirmado por el PO el 2026-11-21 y que el alta de staff usa `UsuarioService.registrar(...)` sin endpoint de registro, y que el límite en memoria no distribuido (evadible rotando de IP) se verifica por documentación; sin marcar checkboxes ni avanzar el apply.
- Evidencia: `mvn test` → 147 tests, 0 failures/errors (93 C-07/C-08 + 54 auth-rbac); `openspec validate auth-rbac --strict` → válido; `git diff --name-only -- openspec/specs` → vacío; C-08 sigue `[ ]`; `.env.example` sin valores y `.env` ignorado.

## Workflow follow-up

- El PO aprobó el apply de C-04 el 2026-11-21 con el delta ajustado; la ejecución se hace con Strict TDD y verificación técnica, sin marcar checkboxes hasta registrar evidencia.
- La ejecución se detiene antes del archive: ese paso requiere verificación técnica y una decisión posterior, sin tocar `CHANGES.md` ni los deltas versionados.
- **Nota de cierre (2026-11-21)**: apply completado 49/49 tareas marcadas `[x]`, 147 tests (93 C-07/C-08 + 54 auth-rbac), 0 failures/errors, detenido antes del archive. El ajuste de proceso por tiempos no desmarca tareas ni reabre el apply; no hay grupo en curso y T1..T11 no se rehacen.
