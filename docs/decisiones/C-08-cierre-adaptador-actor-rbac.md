# Cierre de C-08 — adaptador de actor real y protección RBAC de la agenda

- **Fecha**: 2026-11-22
- **Solicitante**: dueño de la rama `Parte-2-Donato` (pedido explícito de resolver la deuda antes de iniciar la Parte 2)
- **Change**: C-08 `agenda-core-and-overlap-prevention` · **Gobernanza**: MEDIO / CRITICO (toca la integración con Auth/RBAC de C-04)
- **Resuelve**: [`C-05-deuda-adaptador-actor.md`](C-05-deuda-adaptador-actor.md)
- **Revisión PO recomendada**: Lautaro Ellerbach (PO de la Parte 1), por tocar el paquete `auth`.

## Qué se implementó

| # | Pieza | Archivo | Perfil |
|---|---|---|---|
| 1 | Adaptador `AuthenticatedActor` → `ActorSimulado` (los cuatro modos) | `auth/AuthenticatedActorAdapter.java` | — (puro) |
| 2 | `CurrentActorProvider` real: lee el actor del contexto de seguridad; sin actor falla cerrado (`AccesoDenegadoException`), nunca cae al simulado | `auth/AuthenticatedCurrentActorProvider.java` | `!dev & !test` |
| 3 | Política RBAC de agenda y catálogos sobre `rol_permiso` (sin migración nueva) | `auth/AgendaRbacPolicy.java` | — |
| 4 | Filtro: cookie de sesión C-04 → `AccessAuthenticator` (firma, exp, emisor, `jti` no revocado) → usuario activo con el mismo rol → CSRF de doble envío en escrituras → RBAC → actor `REAL` en el contexto | `auth/AgendaAccessFilter.java` | — |
| 5 | Segunda `SecurityFilterChain` con `securityMatcher` limitado a `/agenda`, `/turnos`, `/bloqueos`, `/sobreturnos`, `/tratamientos`, `/horarios`, `/feriados` | `auth/AgendaSecurityConfig.java` | `!dev & !test` |
| 6 | Consulta derivada `existsByRol_CodigoAndPermiso_RecursoAndPermiso_Accion` | `infrastructure/persistence/RolPermisoRepository.java` | — |
| 7 | Fix C-04: firma JWT con base64 inválido devolvía 500 (`IllegalArgumentException`) en lugar de 401 | `auth/JwtService.java` | — |

Todo el código que lee identidad vive en el paquete `auth`, igual que en C-04.

## Especificación

El requisito y sus escenarios viven en `openspec/specs/agenda-rbac/spec.md`, como capacidad aparte: la spec `agenda` conserva sus 13 requisitos y 31 escenarios (invariante verificado por `PublicDeltaInvariantsTest`).

## Semántica de los cuatro modos

| Modo | Origen | Cuándo |
|---|---|---|
| `SIMULADO` | `SimulatedCurrentActorProvider` (sin cambios) | perfiles `dev`/`test`: la agenda conserva el comportamiento de C-08 sin autenticación |
| `REAL` | `AgendaAccessFilter` + `AuthenticatedCurrentActorProvider` | fuera de `dev`/`test`, request autenticada y autorizada |
| `SYSTEM` | `CurrentActorProvider.system()` en ambos proveedores | tareas internas sin request |
| `PACIENTE` | adaptador (`ActorSimulado.paciente` / `pacienteSinUsuario`) | superficie pública con token (C-09/C-13); el rol `paciente` **no** accede a la superficie de staff |

Los proveedores real y simulado son mutuamente excluyentes por perfil (verificado en `AuthenticatedCurrentActorProviderTest`).

## Matriz RBAC aplicada (seed V001)

| Operación | Requisito (alternativas) | recepcionista | odontólogo | administrador | auditor | paciente |
|---|---|---|---|---|---|---|
| `GET` agenda | `agenda:leer` **o** `turno:leer` | ✔ | ✔ | ✔ | ✘ | ✘ |
| `POST /turnos`, `POST /bloqueos` | `agenda:crear` | ✔ | ✘ | ✔ | ✘ | ✘ |
| `/sobreturnos` y demás escrituras de agenda | `agenda:actualizar` | ✔ | ✘ | ✔ | ✘ | ✘ |
| `GET` catálogos | `configuracion:leer` **o** `agenda:leer` **o** `agenda:crear` **o** `turno:leer` | ✔ | ✔ | ✔ | ✔ | ✘ |
| Escrituras de catálogos | `configuracion:leer` **y** `agenda:actualizar` | ✘ | ✘ | ✔ | ✘ | ✘ |

Respuestas: `401 token-invalido` (sin sesión, token inválido/vencido/revocado, usuario inactivo o con rol distinto al del token), `403 csrf-invalido`, `403 acceso-denegado`; mismos cuerpos que `/auth/*`.

## Supuestos y residuales `(TBD)`

- **Ámbito `propio`** (p. ej. odontólogo «agenda propia»): no hay vínculo `usuario → profesional` en el modelo; hoy el odontólogo lee toda la agenda. Filtrar por recurso propio queda `(TBD)`.
- **Auditor**: la KB lo muestra con lectura de agenda, pero el seed V001 no le concede `agenda:leer`/`turno:leer`; se respeta el seed. Cambiarlo exige migración en el rango de la Parte 1.
- **Escritura de catálogos**: no existe permiso `configuracion:actualizar`; se exige la combinación `configuracion:leer` + `agenda:actualizar` (en el seed solo la tiene el administrador). Un permiso dedicado requiere migración en el rango de la Parte 1 (V007–V009).
- **Despliegue**: los controladores de agenda siguen con `@Profile({"dev","test"})` (guardia `AgendaSurfaceTest`); habilitarlos en otro perfil es una decisión de despliegue aparte. La cadena y el proveedor real ya quedan activos fuera de `dev`/`test`, así que cualquier perfil que exponga la agenda la recibe protegida. Además, antes de este cambio no había ningún `CurrentActorProvider` fuera de `dev`/`test`.

## Guardias conservadas

`AgendaSurfaceTest`, `CurrentActorProviderTest`, `AuthSecurityChainTest` y `AuthWebTest.cadenaDeSeguridadSoloCoincideConAuth` **no se modificaron** y siguen en verde: en `dev`/`test` la única cadena es `/auth/**`.

## Evidencia

- `mvn test`: **226/226** en verde (194 previos + 32 nuevos), con Postgres 17 vía Testcontainers.
- Tests nuevos: `AuthenticatedActorAdapterTest` (6), `AuthenticatedCurrentActorProviderTest` (6), `AgendaRbacPolicyTest` (6), `AgendaRbacSeedPostgresTest` (5, seed real V001), `AgendaSecurityChainTest` (9, contexto Spring con perfil `agenda-protegida`: dos cadenas, 401/403, CSRF, `jti` revocado por logout, usuario inactivo, actor `REAL` entregado al servicio y contexto limpio después de la request).
