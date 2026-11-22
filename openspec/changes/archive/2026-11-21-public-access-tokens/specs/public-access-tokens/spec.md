# Spec Delta

## Purpose

Define el contrato observable del acceso público por token de enlace/QR: scopes, claims, firma, vencimiento, estado de `jti`, rotación, idempotencia y transporte seguro, para que C-09 y C-13 puedan consumirlo sin credenciales de staff ni identidad verificada.

| Scope | Uso | Vinculación | Exp | Verificación |
|---|---|---|---|---|
| `RESERVAR` | consultar disponibilidad y crear turno (C-09) | sin `turnoId` al emitir | 60 min | no requiere identidad verificada |
| `GESTIONAR` | confirmar/cancelar/reprogramar (C-09) | ligado a `turnoId`, rotación permitida | 7 días | no requiere identidad verificada |
| `HISTORIA` | solo lectura/exportación de propia historia (C-13) | ligado a `pacienteId` | 24 h | `identidadVerificada=false` por defecto; C-06 nunca emite `true` |

Los valores de vigencia son metas iniciales no normativas aprobadas por el PO (2026-11-21). Claims mínimos: `jti`, `iss`, `sub`/`pacienteId`, `scope`, `iat`, `exp`, `turnoId` opcional e `identidadVerificada`. `typ` puede incluirse si el diseño lo requiere. La firma es HMAC-SHA256 con `PUBLIC_TOKEN_SECRET`, secreto distinto de `AUTH_JWT_SECRET`; solo el nombre de la variable se documenta.

Invariantes de superficie: el token viaja en path/query; la implementación pública MUST NOT usar `@RequestHeader`, `Authorization`, `Bearer ` ni la ruta `/auth`; las respuestas de superficie pública MUST incluir `Referrer-Policy: no-referrer` y `Cache-Control: no-store`; el token MUST NOT aparecer en logs. El `idempotency_key` viaja en cuerpo o path, nunca en un request header.

## ADDED Requirements

### Requirement: Contrato de scopes, claims e identidad no verificada
El sistema SHALL emitir tokens solo con los scopes y claims del contrato, con `identidadVerificada=false` por defecto. C-06 MUST NOT emitir un token `HISTORIA` con `identidadVerificada=true`; la verificación real queda para C-13. Ante un intento de uso de `HISTORIA` sin identidad verificada, el sistema MUST denegar el acceso y publicar un evento SIMULADO a través de `AuditEventPublisher`, sin persistir auditoría ni exponer API de consulta.

#### Scenario: Identidad no verificada deniega HISTORIA
- **GIVEN** un token `HISTORIA` vigente, ligado a `pacienteId` y con `identidadVerificada=false`
- **WHEN** se intenta leer o exportar la propia historia usando ese token
- **THEN** el sistema deniega la operación, no devuelve datos clínicos y publica un evento SIMULADO de denegación sin persistirlo ni crear rutas de consulta de auditoría

### Requirement: Rechazo de token vencido
El sistema MUST rechazar todo token público cuyo `exp` haya vencido, cualquiera sea su scope, sin aplicar efectos de negocio. Para C-09 el rechazo SHALL habilitar la indicación de solicitar un nuevo enlace, sin que C-06 implemente ese endpoint.

#### Scenario: Token vencido
- **GIVEN** un token firmado cuyo `exp` es anterior al momento de la validación
- **WHEN** se presenta en una operación de superficie pública
- **THEN** el sistema lo rechaza, no crea ni modifica turnos ni historia y la respuesta permite pedir un nuevo enlace según el contrato consumido por C-09

### Requirement: Rechazo de token forjado o de emisor inválido
El sistema MUST verificar la firma HMAC-SHA256 y el `iss` esperado antes de aceptar claims. Un token con firma inválida, emisor distinto o estructura manipulada SHALL ser rechazado sin distinguir públicamente la causa, sin usar secretos de autenticación de staff.

#### Scenario: Token forjado
- **GIVEN** un token con firma inválida o un `iss` distinto del configurado para la superficie pública
- **WHEN** se valida en una operación de C-09 o C-13
- **THEN** el sistema lo rechaza sin otorgar identidad ni scope, sin efectos de negocio y sin revelar el motivo exacto del fallo

### Requirement: Rechazo de jti reutilizado y rotación restringida
El sistema SHALL registrar `jti`, `used_at` y `revoked_at`, y MUST rechazar un `jti` ya usado o revocado. La rotación SHALL estar permitida solo para `GESTIONAR`: emite un nuevo `jti`, marca el anterior como usado y conserva el `turnoId`; `RESERVAR` e `HISTORIA` MUST NOT rotar.

#### Scenario: Token reutilizado
- **GIVEN** un token con `jti` ya usado o revocado y un token vigente de scope `RESERVAR` o `HISTORIA`
- **WHEN** se presenta el token reutilizado y se intenta rotar el token vigente
- **THEN** el sistema rechaza el reutilizado, no rota scopes distintos de `GESTIONAR` y solo `GESTIONAR` obtiene un nuevo `jti` con el anterior marcado como usado

### Requirement: Idempotencia ante reenvío duplicado
El sistema SHALL persistir `idempotency_key` con unicidad por operación en PostgreSQL (RN-GL-02). La clave MUST viajar en cuerpo o path, nunca en un request header. Un reenvío con la misma operación y clave MUST NOT producir un segundo efecto, y operaciones distintas SHALL poder reutilizar la misma clave.

#### Scenario: Reenvío duplicado
- **GIVEN** una operación crítica ya ejecutada con una `idempotency_key` persistida
- **WHEN** llega un reenvío con la misma operación y la misma clave, verificado contra PostgreSQL
- **THEN** el sistema no produce un segundo efecto ni duplica registros, responde como repetición y la restricción de unicidad por operación permanece verificada

### Requirement: Confidencialidad del token en logs
El sistema MUST NOT escribir el token completo ni fragmentos identificables del mismo en logs de request, respuesta, error o auditoría SIMULADA, con independencia de que viaje en path o query.

#### Scenario: Token nunca aparece en logs
- **GIVEN** operaciones públicas válidas, vencidas, forjadas y rechazadas que transportan el token en path o query
- **WHEN** se inspeccionan los logs producidos por esas operaciones
- **THEN** el token completo y sus fragmentos identificables no aparecen en ningún registro, incluidos mensajes de error y eventos SIMULADOS

### Requirement: Response headers de superficie pública
El sistema SHALL fijar `Referrer-Policy: no-referrer` y `Cache-Control: no-store` en todas las respuestas de la superficie pública que acepte tokens, sin leer request headers de credenciales.

#### Scenario: Headers presentes en respuestas públicas
- **GIVEN** una respuesta de la superficie pública que consume un token por path o query
- **WHEN** se inspeccionan sus response headers, tanto en éxito como en rechazo
- **THEN** incluye `Referrer-Policy: no-referrer` y `Cache-Control: no-store`, sin exigir credenciales de staff ni leer request headers

### Requirement: Migración V004 solo de metadatos
La migración reservada `V004__public_access_tokens.sql` SHALL crear solo tablas de metadatos (`jti`/hash e idempotencia). MUST NOT duplicar `Turno.token_publico`, ya existente en V006, ni guardar el token en texto plano. En bases donde V006 ya está registrada, el escenario de test SHALL verificar `spring.flyway.out-of-order=true`.

#### Scenario: V004 sin token en texto plano
- **GIVEN** la migración V004 aplicada sobre PostgreSQL, incluido un escenario con V006 registrada previamente
- **WHEN** se inspeccionan el SQL y el esquema resultante
- **THEN** no existe una columna `token_publico` creada por V004 ni columnas con el token en claro, y V004 queda aplicada con `out-of-order=true` cuando V006 precede
