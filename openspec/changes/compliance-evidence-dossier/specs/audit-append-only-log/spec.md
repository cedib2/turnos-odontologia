# Spec Delta (propuesta)

> Parte del texto vigente del requirement después del archive de C-22 y C-17; si el orden cambia, se reescribe sobre `openspec/specs/` en el apply.

## MODIFIED Requirements

### Requirement: Catálogo mínimo y granularidad por operación
El catálogo autorizado se limita a `turno.creado`, `turno.reprogramado`, `turno.sobreturno`, `bloqueo.creado`, `publictoken.historia.denegado`, `metricas.consultadas`, `dossier.evidencia.cargada`, `dossier.descargado` (los eventos nuevos quedan sujetos a aprobación del PO según la Decisión 1 de C-05). Cada operación de dominio confirmada SHALL generar exactamente una fila; el sistema MUST NOT crear una fila por cambio de campo ni por operaciones rechazadas.

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

#### Scenario: Carga de evidencia auditada
- **GIVEN** un administrador que carga un documento de evidencia
- **WHEN** se confirma la carga
- **THEN** existe una sola fila `dossier.evidencia.cargada` con el ítem y el checksum, sin el contenido del documento

#### Scenario: Descarga del dossier auditada
- **GIVEN** un administrador autorizado
- **WHEN** descarga el dossier
- **THEN** existe una sola fila `dossier.descargado` con el estado del dossier
