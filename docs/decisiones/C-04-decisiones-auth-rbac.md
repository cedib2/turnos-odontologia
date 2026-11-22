# Decisiones del PO — C-04 `auth-rbac`

- **Fecha**: 2026-11-21
- **Decisor**: `Lautaro Ellerbach, PO` (confirmado el 2026-11-21).
- **Alcance**: `C-04 auth-rbac` (Parte 1 — Tronco Base)

## Contexto

El PO aceptó las recomendaciones del explore de C-04 con los valores indicados abajo. Los valores numéricos son **metas iniciales, no normativas**. Esta decisión autorizó el **propose** de C-04; el **apply** (CRITICO) fue aprobado por el PO el 2026-11-21 con el delta ajustado y se ejecuta con Strict TDD, deteniéndose antes del archive.

## Decisiones aprobadas

| # | Tema | Decisión |
|---|---|---|
| 1 | Paths y transporte | `/auth/*` literal; cookie `HttpOnly`/`Secure`/`SameSite` con CSRF. |
| 2 | Mecanismo | JWT access + refresh opaco persistido en V002, con rotación y detección de reutilización. |
| 3 | Expiración y logout | Access **15 min**; refresh **7 días** con rotación; logout revoca **sesión y familia**. Metas iniciales, no normativas. |
| 4 | Spring Security y guardias | **Sí** a `spring-boot-starter-security`. Ajustar `AgendaSurfaceTest` y `CurrentActorProviderTest` de forma **acotada**: permitir solo en el paquete de `auth`; mantener las guardias para agenda/catálogos. Los 93 tests vigentes de C-07/C-08 deben seguir en verde. |
| 5 | Secretos | Variables de entorno. `.env` incluido en `.gitignore` **antes** de crear cualquier archivo; `.env.example` sin valores. Ninguna clave en el repo. |
| 6 | Rate limiting | En memoria, por combinación IP + usuario: **5 intentos fallidos en 15 minutos**, respuesta **429** con header `Retry-After`. Documentar que el límite es **en memoria** (no distribuido), se esquiva rotando de IP, no se comparte entre réplicas y se pierde al reiniciar el proceso. |
| 7 | Contrato de actor | Aprobar `modo ∈ {REAL, SIMULADO, SYSTEM, PACIENTE}` con snapshot inmutable y proveedor real separado que conserva `CurrentActorProvider` simulado. |
| 8 | V002 y Flyway | Revocación por sesión **y** `jti`; habilitar `spring.flyway.out-of-order=true` en dev/test antes del apply. |

## Ajustes del PO previos al apply (2026-11-21)

Solicitados por el PO antes de autorizar el apply; ya reflejados en `openspec/changes/auth-rbac/`:

| # | Tema | Ajuste |
|---|------|--------|
| 9 | Hash de contraseñas | Algoritmo adaptativo **BCrypt**, confirmado por el PO el 2026-11-21 (RN-GL-03); prohibido texto plano y logs con contraseñas/hashes. |
| 10 | Operación protegida y ámbitos | Referencia 403: `GET /auth/me` (`propio` = `sub` del token coincide con el recurso) y `GET /auth/usuarios/{id}` (`todos`, solo identidad de staff sin datos clínicos). **C-04 entrega infraestructura RBAC pero aún no protege la agenda**; esa protección queda para un change posterior. |
| 11 | Credenciales inválidas y CSRF en login | Usuario inexistente, contraseña incorrecta e inactivo → misma **401** indistinguible. Antes del login, `GET /auth/csrf` emite cookie CSRF anónima de doble envío; `POST /auth/login` exige `X-CSRF-Token`. La cookie CSRF no autoriza y se rota al login; la cookie de sesión sigue siendo `HttpOnly`/`Secure`/`SameSite`. |
| 12 | Rate limiting | Conteo por **combinación IP + usuario** (no contadores sumados); login exitoso **reinicia** el contador; ventana desde el primer fallo. El escenario «Límite no distribuido» pasa a verificación documental. El límite es en memoria y no distribuido: **se esquiva rotando de IP** (cada IP nueva con el mismo usuario tiene contador propio), no se comparte entre réplicas y se pierde al reiniciar el proceso. |
| 13 | Hash de contraseñas = BCrypt | **BCrypt** confirmado por el PO el 2026-11-21; sin decisión pendiente sobre el algoritmo (RN-GL-03). |
| 14 | Alta de usuarios de staff | Se usa el servicio interno existente `UsuarioService.registrar(...)` con `hash_password` generado por BCrypt; **no se agrega endpoint de registro** (por ejemplo, `POST /auth/registro`). Los fixtures de test usan ese servicio. |
| 15 | Uniformidad temporal del login | Usuario inexistente, contraseña incorrecta e inactivo ejecutan la verificación BCrypt; para inexistente/inactivo se usa un hash dummy, sin retorno anticipado, de modo que la 401 no filtre la causa por tiempo de respuesta. |

`openspec validate auth-rbac --strict` quedó válido tras los ajustes. Ningún checkbox ni código fue modificado.

## Tratamiento de la superficie C-08 con Spring Security activo

- La cadena de seguridad se limita con `securityMatcher("/auth/**")` (o equivalente): **no** se instala una cadena global que proteja `/agenda`, `/turnos`, `/bloqueos`, `/sobreturnos` ni los catálogos de C-07.
- En los slices MVC de agenda/catálogos se excluye la auto-configuración de seguridad o se provee una cadena de test permisiva; la semántica vigente de esas rutas en dev/test (**sin autenticación**) no cambia.
- Las guardias se estrechan: dejan de escanear `pom.xml` y el paquete `auth`, y siguen verificando que ningún controlador de agenda/catálogo lea identidad, exponga `/auth`, `@RequestHeader`, `@CookieValue`, `Authorization` o `Bearer`.
- Se agrega un test positivo: `/auth/**` existe solo en el paquete de auth y la cadena está restringida por `securityMatcher`.
- Cualquier cambio en tests guardia es el mínimo necesario y debe conservar los 93 tests verdes; no se evaden literales ni se debilitan las garantías de agenda.

## Archivos impactados

- `.gitignore` (creado en esta sesión con `.env`, `.env.*` y excepción `!.env.example`).
- `CHANGES.md`: nota de decisión en C-04.
- `.active-orchestrator-state.json`: v21 con propose completado y ajustes previos al apply registrados; el apply fue aprobado posteriormente el 2026-11-21.
- `openspec/changes/auth-rbac/specs/auth-rbac/spec.md`, `specs/agenda/spec.md`, `design.md`, `tasks.md` y `proposal.md`: delta ajustado y validado.

## Gobernanza

El **apply** de C-04 fue aprobado por el PO (`Lautaro Ellerbach`) el 2026-11-21 con el delta ajustado. Su ejecución se hace con Strict TDD y se detiene antes del archive; esta actualización es solo documental y no habilita escritura de código ni marca checkboxes.

Ajuste de proceso por tiempos: ver C-04-ajuste-proceso-tiempos.md (mismo decisor/fecha).

---

**Decisor**: `Lautaro Ellerbach, PO` · **Fecha**: 2026-11-21 · **Apply aprobado**: 2026-11-21 (delta ajustado)
