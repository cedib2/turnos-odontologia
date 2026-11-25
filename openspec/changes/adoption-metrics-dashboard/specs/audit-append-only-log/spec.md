# Spec Delta

## MODIFIED Requirements

### Requirement: Catálogo mínimo y granularidad por operación
El catálogo autorizado se limita a `turno.creado`, `turno.reprogramado`, `turno.sobreturno`, `bloqueo.creado`, `publictoken.historia.denegado` y `metricas.consultadas` (este último sujeto a aprobación del PO según la Decisión 1 de C-05). Cada operación de dominio confirmada SHALL generar exactamente una fila; el sistema MUST NOT crear una fila por cambio de campo ni por operaciones rechazadas.

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

#### Scenario: Consulta del tablero de métricas
- **GIVEN** el publicador real de auditoría
- **WHEN** se consulta el tablero de métricas de C-22
- **THEN** existe una sola fila con `accion=metricas.consultadas`, entidad `metricas`, `entidad_id` nulo y el actor de la consulta

### Requirement: Trazabilidad mínima de cada fila
Cada fila SHALL registrar `fecha`, `entidad`, `entidad_id`, actor, `modo`, `ip` nullable, `dispositivo` opcional, `motivo`, `metadata` y `correlationId`. `entidad_id` SHALL ser nullable solo para eventos sin entidad agregada concreta, como `publictoken.historia.denegado` y `metricas.consultadas`. El `correlationId` SHALL repetirse en las filas de una misma operación y MUST NOT repetirse entre operaciones independientes.

#### Scenario: Fila con todos los campos de trazabilidad
- **GIVEN** una operación confirmada con dirección de origen conocida
- **WHEN** se publica su evento
- **THEN** la fila contiene todos los campos exigidos, con `ip` y `dispositivo` cuando estén disponibles

#### Scenario: Correlación entre operaciones
- **GIVEN** dos operaciones de dominio independientes
- **WHEN** se publican sus eventos
- **THEN** cada operación mantiene su propio `correlationId` y no comparten identificador
