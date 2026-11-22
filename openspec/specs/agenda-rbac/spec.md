# agenda-rbac Specification

## Purpose
Protección de la superficie de agenda y catálogos con la sesión y el RBAC de C-04, y adaptación del actor autenticado al puerto `CurrentActorProvider`. Cierra la deuda registrada en C-05 y completa C-08. No modifica los requisitos de `agenda`: en `dev`/`test` rige «Aislamiento de la superficie autenticada».

## Requirements

### Requirement: Protección RBAC de la agenda fuera de dev/test
Fuera de los perfiles `dev`/`test`, el sistema SHALL proteger `/agenda`, `/turnos`, `/bloqueos`, `/sobreturnos`, `/tratamientos`, `/horarios` y `/feriados` con una cadena de seguridad propia, definida en el paquete de autenticación, que exige la sesión de C-04 (access JWT válido con `jti` no revocado y usuario activo con el mismo rol), CSRF de doble envío en métodos que modifican estado y permiso RBAC según `rol_permiso`. El `CurrentActorProvider` real SHALL adaptar el `AuthenticatedActor` a la instantánea de actor conservando `usuarioId`, nombre, rol, `jti` y modo, y MUST fallar cerrado si no hay actor autenticado. En `dev`/`test` se conserva el requisito «Aislamiento de la superficie autenticada» sin cambios.

#### Scenario: Request sin sesión válida
- **GIVEN** un perfil distinto de `dev`/`test`
- **WHEN** se invoca una ruta de agenda sin sesión, con token inválido, vencido o revocado, o con usuario inactivo
- **THEN** responde 401 `token-invalido` sin invocar el servicio

#### Scenario: Rol sin permiso o escritura sin CSRF
- **GIVEN** una sesión válida
- **WHEN** el rol no tiene el permiso requerido o una escritura llega sin CSRF válido
- **THEN** responde 403 `acceso-denegado` o `csrf-invalido` respectivamente

#### Scenario: Actor real entregado a los servicios
- **GIVEN** una sesión válida con permiso
- **WHEN** un servicio de agenda consulta `CurrentActorProvider.current()`
- **THEN** recibe un actor en modo `REAL` con el `usuarioId`, el rol y el `jti` del token, y el contexto de seguridad se limpia al terminar la request

#### Scenario: Paciente fuera de la superficie de staff
- **GIVEN** un token de rol `paciente`
- **WHEN** invoca cualquier ruta de agenda o catálogo
- **THEN** responde 403; el paciente opera solo por la superficie pública con token
