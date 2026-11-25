# cumplimiento-rbac Specification

## Purpose
Protección de la superficie de cumplimiento y métricas de la Parte 4 (C-17, C-18, C-22) con la sesión y el RBAC de C-04, con el mismo esquema que `agenda-rbac`. Reutiliza la matriz `rol_permiso` de V001 sin migración nueva. Los controladores siguen siendo superficie indicativa de `dev`/`test`, como el resto del proyecto; esta cadena queda activa fuera de esos perfiles para cuando se productivicen.

## Requirements

### Requirement: Protección RBAC de cumplimiento y métricas fuera de dev/test
Fuera de los perfiles `dev`/`test`, el sistema SHALL proteger `/cumplimiento/**` y `/metricas/**` con una cadena de seguridad propia que exige la sesión de C-04 (access JWT válido con `jti` no revocado y usuario activo con el mismo rol), CSRF de doble envío en métodos que modifican estado y permiso RBAC según `rol_permiso`. `/precio` (C-21) es público y MUST NOT pertenecer a esta cadena. En `dev`/`test` la superficie sigue sin autenticación, igual que la de agenda.

#### Scenario: Request sin sesión válida
- **GIVEN** un perfil distinto de `dev`/`test`
- **WHEN** se invoca una ruta de cumplimiento o métricas sin sesión válida
- **THEN** responde 401 `token-invalido` sin invocar el servicio

#### Scenario: Escritura sin CSRF
- **GIVEN** una sesión válida de un rol con permiso
- **WHEN** una escritura llega sin CSRF válido
- **THEN** responde 403 `csrf-invalido`

### Requirement: Matriz de permisos de cumplimiento
Exportar, descargar una exportación, respaldar, restaurar y escribir en el dossier SHALL exigir `exportacion:crear`, que en el seed de V001 solo tiene el administrador. Leer el dossier, las exportaciones vencidas y las métricas SHALL exigir `auditoria:leer` (administrador y auditor). Una escritura sobre `/metricas` MUST negarse. El rol `paciente` MUST NOT acceder.

#### Scenario: Auditor solo lee
- **GIVEN** una sesión válida de rol `auditor`
- **WHEN** consulta las métricas o el dossier, o intenta respaldar o descargar una exportación
- **THEN** las lecturas se permiten y el respaldo y la descarga responden 403 `acceso-denegado`

#### Scenario: Staff sin rol de supervisión
- **GIVEN** una sesión válida de rol `recepcionista` u `odontologo`
- **WHEN** invoca cualquier ruta de cumplimiento o métricas
- **THEN** responde 403

#### Scenario: Administrador con actor real
- **GIVEN** una sesión válida de rol `administrador` y CSRF válido
- **WHEN** solicita un respaldo
- **THEN** el servicio recibe un actor en modo `REAL` con el `usuarioId`, el rol y el `jti` del token
