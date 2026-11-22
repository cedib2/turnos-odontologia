# Spec Delta

## Purpose

Define la evidencia de auditoría persistente: un registro append-only con cadena de integridad, un catálogo mínimo de eventos de agenda, la instantánea inmutable del actor y el rechazo de `metadata` sensible.

## ADDED Requirements

### Requirement: Registro append-only con restricción en base de datos
El sistema SHALL conservar cada evento de auditoría en una tabla de solo inserción. El rol de aplicación MUST NOT poder ejecutar `UPDATE`, `DELETE` ni `TRUNCATE` sobre esa tabla; la restricción SHALL existir en la base de datos, no solo en la aplicación (RN-SE-02).

#### Scenario: Inserción confirmada y conservada
- **GIVEN** una operación de dominio confirmada
- **WHEN** se publica su evento de auditoría
- **THEN** existe una fila en el registro y sus valores permanecen idénticos a los publicados

#### Scenario: Intento de actualización rechazado
- **GIVEN** una fila ya registrada
- **WHEN** el rol de aplicación intenta actualizarla
- **THEN** la base de datos rechaza la sentencia y la fila original permanece intacta

#### Scenario: Intento de borrado rechazado
- **GIVEN** una fila ya registrada
- **WHEN** el rol de aplicación intenta borrarla
- **THEN** la base de datos rechaza la sentencia y la fila sigue presente

#### Scenario: Intento de vaciado rechazado
- **GIVEN** una tabla con al menos una fila
- **WHEN** el rol de aplicación intenta vaciarla con `TRUNCATE`
- **THEN** la base de datos rechaza la sentencia y conserva todas las filas

### Requirement: Cadena de integridad SHA-256 sin clave
Cada fila SHALL enlazarse a la anterior con `hash_anterior`/`hash_actual`, SHA-256 sobre una representación canónica ordenada por `id`; la primera usa el génesis. El sistema MUST NOT usar HMAC ni clave secreta en este change. **Cadena válida**: `hash_anterior` iguala exactamente el `hash_actual` de la fila inmediatamente anterior **en orden de `id`** y la primera usa el génesis. No se exigen ids consecutivos (la secuencia PostgreSQL salta por rollbacks); sí se prohíben huecos lógicos.

#### Scenario: Primera fila con valor génesis
- **GIVEN** un registro vacío
- **WHEN** se publica el primer evento
- **THEN** la fila contiene `hash_anterior` igual al valor génesis documentado y un `hash_actual` no vacío

#### Scenario: Fila enlazada a su predecesora
- **GIVEN** una última fila confirmada con su `hash_actual`
- **WHEN** se publica el evento siguiente
- **THEN** la nueva fila contiene `hash_anterior` igual al `hash_actual` de la predecesora

#### Scenario: Verificación de cadena íntegra
- **GIVEN** varias filas publicadas en secuencia
- **WHEN** se verifica la cadena completa
- **THEN** la verificación informa que la cadena es válida y continúa sin huecos

#### Scenario: Cadena válida con saltos de id y sin huecos lógicos
- **GIVEN** un registro PostgreSQL con tres filas confirmadas cuyos `id` no son consecutivos por rollbacks previos de la secuencia
- **WHEN** se verifica la cadena completa en orden de `id`
- **THEN** la verificación informa que la cadena es válida: cada `hash_anterior` iguala el `hash_actual` de la fila inmediatamente anterior por `id`, la primera usa el génesis, y no se señalan huecos lógicos pese a los saltos de `id`

#### Scenario: Detección de una alteración externa
- **GIVEN** una fila alterada fuera del canal de aplicación, con privilegios de superuser
- **WHEN** se verifica la cadena
- **THEN** la verificación detecta el desajuste e identifica la secuencia afectada

#### Scenario: Publicaciones concurrentes sin bifurcar la cadena
- **GIVEN** dos publicaciones simultáneas sobre un mismo registro inicializado en PostgreSQL 17
- **WHEN** ambas se confirman
- **THEN** existen dos filas con `id` distintos encadenadas en orden (la segunda usa el `hash_actual` de la primera), la verificación no encuentra huecos y no hay bifurcación: ninguna fila alternativa comparte la misma predecesora inmediatamente anterior

### Requirement: Catálogo mínimo y granularidad por operación
El catálogo autorizado en este change se limita a `turno.creado`, `turno.reprogramado`, `turno.sobreturno`, `bloqueo.creado` y `publictoken.historia.denegado`. Cada operación de dominio confirmada SHALL generar exactamente una fila; el sistema MUST NOT crear una fila por cambio de campo ni por operaciones rechazadas.

#### Scenario: Creación de turno
- **GIVEN** una creación de turno confirmada
- **WHEN** se consulta el registro
- **THEN** existe una sola fila de tipo `turno.creado` con entidad `turno` y su identificador

#### Scenario: Reprogramación de turno
- **GIVEN** una reprogramación confirmada
- **WHEN** se consulta el registro
- **THEN** existe una sola fila de tipo `turno.reprogramado` para el turno reprogramado

#### Scenario: Sobreturno con override
- **GIVEN** un sobreturno confirmado con motivo y actor autorizador
- **WHEN** se consulta el registro
- **THEN** existe una sola fila de tipo `turno.sobreturno` que conserva motivo y actor (RN-GL-01)

#### Scenario: Creación de bloqueo
- **GIVEN** un bloqueo de agenda confirmado
- **WHEN** se consulta el registro
- **THEN** existe una sola fila de tipo `bloqueo.creado` con entidad `bloqueo` y su identificador

#### Scenario: Operación rechazada sin fila
- **GIVEN** una operación de agenda rechazada por solapamiento
- **WHEN** se consulta el registro
- **THEN** no existe fila de auditoría asociada a esa operación

#### Scenario: Denegación de HISTORIA con token público
- **GIVEN** una solicitud de HISTORIA con `identidadVerificada=false` emitida por C-06
- **WHEN** se publica `publictoken.historia.denegado` a través del mismo `AuditEventPublisher`
- **THEN** se registra una sola fila con entidad `publictoken`, `entidad_id` nullable, sin inferir identidad verificada, y se conservan `jti`, `scope` y `pacienteId` en `metadata`

### Requirement: Rechazo de campos prohibidos en metadata
`metadata` SHALL aceptar solo claves no sensibles. El sistema MUST rechazar la publicación cuando alguna clave de la lista prohibida aparezca tras normalizarse a minúsculas sin acentos **e ignorando también los guiones bajos (`_`) y los guiones (`-`)**, incluyendo claves de objetos anidados. La lista queda definida en los escenarios, no admite valores `(TBD)` y no se amplía en este change.

#### Scenario: Metadata permitida
- **GIVEN** una `metadata` con claves como `origen`, `motivo` o `detalle_operacion`
- **WHEN** se publica el evento
- **THEN** la fila conserva esa `metadata` sin alterarla

#### Scenario: Metadata permitida de C-06
- **GIVEN** el evento `publictoken.historia.denegado` con `metadata` que contiene `jti`, `scope` y `pacienteId`
- **WHEN** se publica el evento
- **THEN** la fila conserva esas tres claves como identificadores internos o pseudónimos del flujo C-06, sin tratarlas como claves prohibidas

#### Scenario: Rechazo de credenciales y secretos
- **GIVEN** una `metadata` con alguna de las claves `password`, `password_hash`, `hash_password`, `secret`, `api_key`, `access_token`, `refresh_token`, `token`, `authorization`, `jwt` o `credentials`
- **WHEN** se intenta publicar el evento
- **THEN** la publicación se rechaza y no se inserta ninguna fila

#### Scenario: Rechazo de datos personales
- **GIVEN** una `metadata` con alguna de las claves `dni`, `documento_numero`, `telefono`, `email`, `direccion` o `fecha_nacimiento`
- **WHEN** se intenta publicar el evento
- **THEN** la publicación se rechaza y no se inserta ninguna fila

#### Scenario: Rechazo de datos clínicos, firma y diagnóstico
- **GIVEN** una `metadata` con alguna de las claves `firma`, `signature`, `diagnostico`, `historia_clinica` o `ficha_clinica`
- **WHEN** se intenta publicar el evento
- **THEN** la publicación se rechaza y no se inserta ninguna fila

#### Scenario: Rechazo anidado y variantes de caso
- **GIVEN** una `metadata` con la clave prohibida dentro de un objeto anidado o con variante de mayúsculas, acento o separadores (`_`/`-`)
- **WHEN** se intenta publicar el evento
- **THEN** la publicación se rechaza con el mismo criterio que si la clave estuviera en la raíz

#### Scenario: Rechazo de variantes con guion bajo, guion y camel case
- **GIVEN** una `metadata` con las claves `accessToken` y `Password-Hash`
- **WHEN** se intenta publicar el evento
- **THEN** la publicación se rechaza y no se inserta ninguna fila: al normalizar a minúsculas sin acentos y sin `_` ni `-`, resultan `accesstoken` y `passwordhash`, que corresponden a `access_token` y `password_hash` de la lista cerrada de 22 claves

### Requirement: Instantánea inmutable del actor
El registro SHALL conservar una instantánea del actor con identificador de usuario, nombre, rol y `jti`, más un `modo` entre `REAL`, `SIMULADO`, `SYSTEM` y `PACIENTE`, tomada del proveedor de actor vigente. La instantánea MUST NOT cambiar cuando el usuario se modifique con posterioridad (RN-SE-04).

#### Scenario: Actor real persistido
- **GIVEN** una operación ejecutada por un usuario autenticado
- **WHEN** se publica su evento
- **THEN** la fila conserva identificador, nombre, rol y `jti` del usuario y su `modo` es `REAL`

#### Scenario: Modos del proveedor
- **GIVEN** operaciones ejecutadas con los modos `SIMULADO`, `SYSTEM` y `PACIENTE` sin usuario asociado
- **WHEN** se publican sus eventos
- **THEN** cada fila conserva el modo correspondiente con `actor_id` nulo para los tres modos, sin inferir identidad verificada

#### Scenario: Instantánea ante cambios posteriores
- **GIVEN** una fila publicada por un actor
- **WHEN** el usuario cambia de nombre, rol o deja de estar activo
- **THEN** la instantánea de la fila sigue siendo la original y no se sobrescribe

### Requirement: Trazabilidad mínima de cada fila
Cada fila SHALL registrar `fecha`, `entidad`, `entidad_id`, actor, `modo`, `ip` nullable, `dispositivo` opcional, `motivo`, `metadata` y `correlationId`. `entidad_id` SHALL ser nullable solo para eventos sin entidad agregada concreta, como `publictoken.historia.denegado`. El `correlationId` SHALL repetirse en las filas de una misma operación y MUST NOT repetirse entre operaciones independientes.

#### Scenario: Fila con todos los campos de trazabilidad
- **GIVEN** una operación confirmada con dirección de origen conocida
- **WHEN** se publica su evento
- **THEN** la fila contiene todos los campos exigidos, con `ip` y `dispositivo` cuando estén disponibles

#### Scenario: Correlación entre operaciones
- **GIVEN** dos operaciones de dominio independientes
- **WHEN** se publican sus eventos
- **THEN** cada operación mantiene su propio `correlationId` y no comparten identificador

### Requirement: Transacción compartida con la operación de negocio
Evento y operación de dominio SHALL compartir la **misma transacción de base de datos** (mismo `@Transactional`/conexión local). En C-05 el sistema MUST NOT usar `REQUIRES_NEW`, async ni outbox. Si falla el INSERT de auditoría: rollback completo, sin fila de negocio ni de auditoría. Si el negocio hace rollback tras publicar el evento, la fila de auditoría MUST NOT persistir (RN-SE-02, RN-SE-04).

#### Scenario: Fallo de escritura de auditoría revierte la operación de negocio
- **GIVEN** una operación de dominio en curso sobre PostgreSQL 17 con su evento listo para publicarse
- **WHEN** el INSERT en `auditoria_log` falla
- **THEN** la transacción se revierte por completo y no queda ninguna fila de negocio ni de auditoría

#### Scenario: Rollback de negocio no deja fila de auditoría
- **GIVEN** un evento de auditoría publicado dentro de la transacción de una operación de dominio
- **WHEN** la operación de negocio hace rollback después de esa publicación
- **THEN** no queda ninguna fila en `auditoria_log` correspondiente a ese evento
