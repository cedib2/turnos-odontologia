# audit-event-emission Spec Delta

## Purpose

Extender el catálogo cerrado de `audit-event-emission` con los siete nombres inmutables aprobados por el PO para exportación, respaldo y restore, conservando el contrato de correlación y sin abrir superficie HTTP ni permisos nuevos. La spec principal NO se edita en este change: el sync ocurre recién en archive.

## MODIFIED Requirements

### Requirement: Emisión para operaciones relevantes de agenda
El sistema SHALL emitir y persistir un evento por creación, reprogramación, sobreturno, bloqueo y `publictoken.historia.denegado`. El sobreturno MUST incluir `motivo` y `actor` (RN-GL-01); cada evento SHALL generar una fila en `auditoria_log`. El catálogo SHALL añadir los nombres inmutables `EXPORT_REQUESTED`, `EXPORT_COMPLETED`, `EXPORT_FAILED`, `BACKUP_EXECUTED`, `BACKUP_FAILED`, `RESTORE_DRY_RUN_VERIFIED`, `RESTORE_EXECUTED`, con `correlationId`, actor, `occurredAt`, motivo y resultado.

#### Scenario: Evento de sobreturno
- **GIVEN** un sobreturno confirmado con motivo y actor
- **WHEN** se consulta el registro de auditoría persistido
- **THEN** existe un evento de sobreturno que contiene el motivo, el actor y el modo suministrado por el proveedor

#### Scenario: Operación rechazada
- **GIVEN** una operación de agenda rechazada por solapamiento
- **WHEN** se consulta el registro de auditoría persistido
- **THEN** no se ha emitido ni persistido evento de confirmación para esa operación

#### Scenario: Los cuatro tipos de operación
- **GIVEN** una creación de turno, una reprogramación, un sobreturno y un bloqueo confirmados
- **WHEN** se consulta el registro de auditoría persistido
- **THEN** se encuentran `turno.creado`, `turno.reprogramado`, `turno.sobreturno` y `bloqueo.creado`, uno por operación

#### Scenario: Evento de denegación de HISTORIA
- **GIVEN** una solicitud de HISTORIA rechazada por C-06 por identidad no verificada
- **WHEN** se emite `publictoken.historia.denegado` a través del publicador real
- **THEN** se persiste una sola fila con entidad `publictoken` y `entidad_id` nullable, sin tratar el actor como identidad verificada

#### Scenario: Eventos inmutables de exportación
- **GIVEN** una exportación solicitada y completada por un actor
- **WHEN** se emiten sus eventos a través de `AuditEventPublisher`
- **THEN** se persisten exactamente `EXPORT_REQUESTED` y `EXPORT_COMPLETED`, cada uno con `correlationId`, actor, `occurredAt`, motivo/metadata y resultado, sin endpoint REST ni permiso de lectura nuevo

#### Scenario: Evento de fallo de exportación
- **GIVEN** una exportación que termina en error
- **WHEN** se emite el evento de resultado
- **THEN** se persiste `EXPORT_FAILED` con el motivo del error, la correlación y el resultado fallido, sin fila de éxito

#### Scenario: Eventos de respaldo ejecutado y fallido
- **GIVEN** un respaldo ejecutado y un respaldo que falla por indisponibilidad del destino
- **WHEN** se emiten sus eventos
- **THEN** se persisten `BACKUP_EXECUTED` y `BACKUP_FAILED` como nombres exactos e inmutables, con motivo/metadata y resultado

#### Scenario: Eventos de restore verificado y ejecutado
- **GIVEN** un restore en entorno aislado con cadena y conteos verificados
- **WHEN** se emiten los eventos de verificación y ejecución
- **THEN** se persisten `RESTORE_DRY_RUN_VERIFIED` y `RESTORE_EXECUTED` con la misma correlación y `occurredAt`

#### Scenario: Correlación distinta entre operaciones de exportación y respaldo
- **GIVEN** una exportación y un respaldo independientes
- **WHEN** se emiten sus eventos del catálogo ampliado
- **THEN** cada operación conserva su propio `correlationId` y no se reutiliza entre eventos ni operaciones
