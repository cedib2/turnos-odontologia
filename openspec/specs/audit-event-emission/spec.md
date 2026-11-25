# audit-event-emission Specification

## Purpose

Definir el contrato observable del evento de auditoría que emite la agenda para operaciones relevantes, dejando explícito que es una emisión simulada en memoria y no una evidencia de cumplimiento.

## Requirements

### Requirement: Contrato del evento emitido
El sistema SHALL emitir un evento con los campos: tipo de evento, entidad afectada e identificador, actor, modo del actor, `occurredAt`, motivo/metadata y `correlationId`. Todos los campos MUST estar presentes en cada evento emitido.

#### Scenario: Evento de creación de turno
- **GIVEN** una creación de turno confirmada
- **WHEN** se inspecciona el evento emitido
- **THEN** el evento contiene tipo, entidad `turno`, su identificador, actor, modo, `occurredAt`, motivo/metadata y `correlationId`

### Requirement: Actor con modo simulado explícito
El evento SHALL identificar al actor suministrado por el proveedor vigente de C-04 y MUST conservar su `modo` entre `REAL`, `SIMULADO`, `SYSTEM` y `PACIENTE`. En perfiles dev/test el proveedor simulado sigue produciendo `SIMULADO`; fuera de esos perfiles el modo `REAL` SHALL corresponder a un usuario autenticado. El modo SHALL propagarse a la instantánea persistida por `audit-append-only-log`.

#### Scenario: Actor fijo de dev/test
- **GIVEN** el proveedor de actor simulado en dev/test
- **WHEN** se emite un evento por una operación de agenda
- **THEN** el evento registra el actor fijo correspondiente y su modo `SIMULADO`, y la fila persistida conserva ese mismo modo

#### Scenario: Sin identidad verificada
- **GIVEN** un evento con modo `SIMULADO` o `PACIENTE`
- **WHEN** se interpreta su campo actor
- **THEN** el sistema no lo trata como prueba de identidad ni como resultado de una autorización

#### Scenario: Actor real de C-04
- **GIVEN** una operación ejecutada por un usuario autenticado
- **WHEN** se emite y persiste su evento
- **THEN** la instantánea registra identificador, nombre, rol y `jti` del usuario y su modo es `REAL`

### Requirement: Emisión para operaciones relevantes de agenda
El sistema SHALL emitir y persistir un evento por la creación de turno, la reprogramación, el sobreturno con override y la creación de bloqueos, y también SHALL persistir `publictoken.historia.denegado` cuando C-06 lo emite, sin cambiar su semántica de identidad no verificada. En el sobreturno, el evento MUST incluir el `motivo` y el `actor` que autorizaron la excepción (RN-GL-01). Cada evento SHALL generar una sola fila en `auditoria_log`. El catálogo SHALL añadir los nombres inmutables `EXPORT_REQUESTED`, `EXPORT_COMPLETED`, `EXPORT_FAILED`, `BACKUP_EXECUTED`, `BACKUP_FAILED`, `RESTORE_DRY_RUN_VERIFIED`, `RESTORE_EXECUTED`, con `correlationId`, actor, `occurredAt`, motivo y resultado.

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

### Requirement: Correlación de eventos
Cada evento SHALL llevar un `correlationId` que permita agrupar los eventos generados por una misma operación de agenda. El `correlationId` MUST ser distinto entre operaciones independientes.

#### Scenario: Dos operaciones independientes
- **GIVEN** dos creaciones de turno independientes
- **WHEN** se emiten sus eventos
- **THEN** cada evento tiene un `correlationId` propio y no comparten identificador de correlación

### Requirement: Emisión sin persistencia ni valor de cumplimiento
El sistema SHALL implementar `AuditEventPublisher` como publicador real que persiste cada evento en `auditoria_log` con la instantánea del actor de C-04. El sistema MUST NOT exponer una API REST de consulta de auditoría ni crear permisos de lectura nuevos en este change, y MUST NOT presentar retención, archivado ni respaldo como implementados: esos tratamientos quedan `(TBD)` para un change posterior (RN-SE-02).

#### Scenario: Recorder en memoria
- **GIVEN** una operación de agenda confirmada en dev/test
- **WHEN** se emite el evento
- **THEN** el evento queda persistido en `auditoria_log` con su cadena de integridad y no solo en un recorder en memoria

#### Scenario: Sin evidencia de cumplimiento
- **GIVEN** eventos emitidos y persistidos por la agenda
- **WHEN** se evalúa la superficie HTTP y la evidencia de cumplimiento
- **THEN** no existe endpoint de consulta de auditoría ni permiso de lectura nuevo, y los eventos no acreditan retención, archivado ni respaldo implementados
