# Proposal

## Why

C-03 sembró `Usuario`, `Rol` y `Permiso` con ámbitos `propio`/`todos`/`config`, pero C-08 solo ofrece un actor fijo `SIMULADO` y una superficie HTTP de dev/test sin identidad verificada, sesiones ni autorización. Sin C-04 no existe el actor real que necesitan C-06 (acceso público) y C-05 (auditoría con identidad demostrable), y el sistema no puede operar fuera de dev/test. El PO aprobó el **propose** el 2026-11-21 con valores numéricos como metas iniciales no normativas y aprobó también el **apply** CRITICO con el delta ajustado; su ejecución se hace con Strict TDD y se detiene antes del archive.

## What Changes

- Nueva capability `auth-rbac` con endpoints indicativos `GET /auth/csrf`, `POST /auth/login`, `POST /auth/refresh`, `POST /auth/logout`, `GET /auth/me` y `GET /auth/usuarios/{id}` (solo identidad de staff, sin datos clínicos) bajo paths literales `/auth/*`, cookie de sesión `HttpOnly`/`Secure`/`SameSite` y CSRF anónimo de doble envío (`X-CSRF-Token` en login, rotado tras el éxito). Usuario inexistente, contraseña incorrecta e inactivo producen la misma 401.
- Contraseñas con hash adaptativo **BCrypt**, confirmado por el PO el 2026-11-21 (RN-GL-03), sin texto plano ni contraseñas/hashes en logs. El alta de usuarios de staff usa el servicio interno existente `UsuarioService.registrar(...)` con `hash_password` BCrypt; no se agrega endpoint de registro.
- Access token JWT de 15 minutos y refresh token opaco persistido de 7 días con rotación y detección de reutilización; logout revoca la sesión y toda su familia de tokens.
- `spring-boot-starter-security` con una cadena limitada a `/auth/**` mediante `securityMatcher`: **no** se instala una cadena global que proteja `/agenda`, `/turnos`, `/bloqueos`, `/sobreturnos` ni los catálogos. Autorización sobre la matriz de roles/permisos planos (sin herencia) y ámbitos `propio` (recurso = `sub` del access token, referencia `GET /auth/me`), `todos` (referencia `GET /auth/usuarios/{id}`) y `config`. C-04 entrega la infraestructura RBAC pero aún NO protege la agenda; su protección queda para un change posterior.
- Contrato de actor común con `modo ∈ {REAL, SIMULADO, SYSTEM, PACIENTE}` y snapshot inmutable, más un proveedor real separado; se conservan `CurrentActorProvider` simulado y `AuditEventPublisher` en modo `SIMULADO` de C-08.
- Migración `V002__auth_rbac.sql`: sesiones (con `jti`), refresh tokens con hash único, familia/rotación y revocación por sesión y por `jti`; sin borrado físico; `spring.flyway.out-of-order=true` en dev/test donde V005/V006 ya están registradas.
- Secretos exclusivamente por variables de entorno; `.env.example` sin valores sobre un `.gitignore` ya existente (`.env`, `.env.*`, `!.env.example`). Ninguna clave en el repositorio ni en logs.
- Rate limiting **en memoria** por combinación IP + usuario (no contadores que se suman ni "cualquiera por separado"): 5 intentos fallidos en 15 minutos medidos desde el primer fallo, respuesta `429` con `Retry-After`, login exitoso reinicia el contador; se documenta explícitamente que es en memoria, no distribuido y se esquiva rotando de IP (cada IP nueva con el mismo usuario tiene contador propio), sin compartir entre réplicas ni conservar tras reiniciar el proceso; ese extremo se verifica por documentación.
- Ajuste mínimo de `AgendaSurfaceTest` y `CurrentActorProviderTest`: dejar de escanear `pom.xml` y el paquete `auth`, mantener las prohibiciones sobre agenda/catálogos y agregar un test positivo de que `/auth/**` vive solo en el paquete auth y la cadena está restringida por `securityMatcher`.
- Verificación con TDD estricto (mínimo dos casos por comportamiento), H2 para lógica unitaria y PostgreSQL/Testcontainers para V002, unicidad y rotación; los 93 tests vigentes de C-07/C-08 deben permanecer en verde.

**Fuera de alcance**: cualquier código de producción en este paso (el apply ya está aprobado, pero se ejecuta por separado con Strict TDD); C-05 (auditoría append-only, que sigue `SIMULADO`), C-06 (tokens públicos/QR/WhatsApp), UI/`design-system`, secretos reales o despliegue, rate limit distribuido, herencia de permisos, mapeo fino de permisos por endpoint, protección RBAC de la agenda (change posterior) y acceso del odontólogo a agenda ajena (todos `(TBD)` no resueltos). Tampoco se agrega un endpoint de registro de usuarios: el alta de staff se hace con `UsuarioService.registrar(...)` y BCrypt.

## Capabilities

### New Capabilities
- `auth-rbac`: autenticación y autorización de staff: login/refresh/logout/`me`, sesiones y tokens con rotación y revocación, contrato de actor con `modo`, deny por rol y ámbito, rate limiting y aislamiento de la superficie `/auth/**`.

### Modified Capabilities
- `agenda`: un único requisito `ADDED` y acotado que explicita el aislamiento de la superficie autenticada (`/auth/**` solo en el paquete auth; agenda/catálogos conservan su semántica dev/test sin autenticación). Los 11 requisitos y todos sus escenarios actuales permanecen intactos; no hay `MODIFIED` ni `REMOVED`. `audit-event-emission` **no se modifica** y permanece en modo `SIMULADO` hasta C-05.

## Impact

- Código: nuevo paquete `com.turnosodontologia.auth` (web/application/domain/persistence), dependencia `spring-boot-starter-security` en `pom.xml`, configuración dev/test con `spring.flyway.out-of-order=true`, `.env.example` sin valores y ajuste acotado de dos tests guardia.
- Esquema: `src/main/resources/db/migration/V002__auth_rbac.sql`, con índices, unicidad y checks coherentes con V001/V006.
- Specs: capability nueva `auth-rbac`; delta aditivo en `agenda`; `audit-event-emission` sin cambios.
- Tests: los 93 tests vigentes más la nueva suite de autenticación; PostgreSQL/Testcontainers para migración/rotación/unicidad, H2 solo para unitarios.
- Gobernanza: apply CRITICO aprobado por el PO el 2026-11-21; se ejecuta con Strict TDD y se detiene antes del archive; `CHANGES.md` y C-08 permanecen sin marcar hasta la verificación técnica posterior.
