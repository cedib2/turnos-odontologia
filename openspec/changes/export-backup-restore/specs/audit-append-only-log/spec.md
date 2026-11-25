# Spec Delta (propuesta)

> Parte del texto vigente del requirement tal como queda después del archive de C-22 (`adoption-metrics-dashboard`). Si C-22 no se archiva antes, este delta se reescribe sobre `openspec/specs/` en el momento del apply.

## MODIFIED Requirements

### Requirement: Catálogo mínimo y granularidad por operación
El catálogo autorizado se limita a `turno.creado`, `turno.reprogramado`, `turno.sobreturno`, `bloqueo.creado`, `publictoken.historia.denegado`, `metricas.consultadas`, `exportacion.solicitada`, `exportacion.completada`, `exportacion.vencida`, `exportacion.fallida`, `respaldo.completado`, `respaldo.fallido`, `restore.aislado.verificado`, `restore.produccion.rechazado`, `restore.produccion.ejecutado` (los eventos nuevos quedan sujetos a aprobación del PO según la Decisión 1 de C-05). Cada operación de dominio confirmada SHALL generar exactamente una fila; el sistema MUST NOT crear una fila por cambio de campo ni por operaciones rechazadas.

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

#### Scenario: Exportación trazable
- **GIVEN** una exportación completa confirmada
- **WHEN** se consulta el registro
- **THEN** existen una fila `exportacion.solicitada` y una fila `exportacion.completada` para la misma exportación

#### Scenario: Fallo de respaldo auditado
- **GIVEN** un respaldo programado que falla
- **WHEN** se consulta el registro
- **THEN** existe una sola fila `respaldo.fallido` con el error en `metadata`, sin datos sensibles

#### Scenario: Restore a producción rechazado auditado
- **GIVEN** un intento de restore sobre producción sin restore aislado verificado
- **WHEN** se rechaza
- **THEN** existe una sola fila `restore.produccion.rechazado`
