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
El evento SHALL identificar al actor suministrado por `CurrentActorProvider` y MUST marcar su modo como `SIMULADO`. El modo SIMULADO significa que no hay autenticación, identidad verificada ni RBAC detrás del actor; los valores admitidos en dev/test son `recepcionista` y `system`.

#### Scenario: Actor fijo de dev/test
- **GIVEN** el proveedor de actor simulado en dev/test
- **WHEN** se emite un evento por una operación de agenda
- **THEN** el evento registra el actor fijo correspondiente y su modo `SIMULADO`

#### Scenario: Sin identidad verificada
- **GIVEN** un evento con modo `SIMULADO`
- **WHEN** se interpreta su campo actor
- **THEN** el sistema no lo trata como prueba de identidad ni como resultado de una autorización

### Requirement: Emisión para operaciones relevantes de agenda
El sistema SHALL emitir un evento por la creación de turno, la reprogramación, el sobreturno con override y la creación de bloqueos. En el sobreturno, el evento MUST incluir el `motivo` y el `actor` que autorizaron la excepción (RN-GL-01).

#### Scenario: Evento de sobreturno
- **GIVEN** un sobreturno confirmado con motivo y actor
- **WHEN** se consulta el recorder de eventos
- **THEN** existe un evento de sobreturno que contiene el motivo, el actor y el modo `SIMULADO`

#### Scenario: Operación rechazada
- **GIVEN** una operación de agenda rechazada por solapamiento
- **WHEN** se consulta el recorder de eventos
- **THEN** no se ha emitido evento de confirmación para esa operación

### Requirement: Correlación de eventos
Cada evento SHALL llevar un `correlationId` que permita agrupar los eventos generados por una misma operación de agenda. El `correlationId` MUST ser distinto entre operaciones independientes.

#### Scenario: Dos operaciones independientes
- **GIVEN** dos creaciones de turno independientes
- **WHEN** se emiten sus eventos
- **THEN** cada evento tiene un `correlationId` propio y no comparten identificador de correlación

### Requirement: Emisión sin persistencia ni valor de cumplimiento
El sistema MUST implementar `AuditEventPublisher` como emisor en memoria o no-op en dev/test. El sistema MUST NOT crear tabla append-only, hash encadenado, política de retención ni API de consulta de auditoría, y MUST NOT presentar estos eventos como evidencia de cumplimiento ni como sustituto de C-05 (RN-SE-02).

#### Scenario: Recorder en memoria
- **GIVEN** una operación de agenda confirmada en dev/test
- **WHEN** se emite el evento
- **THEN** el evento solo está disponible en el recorder en memoria y no se persiste en base de datos

#### Scenario: Sin evidencia de cumplimiento
- **GIVEN** eventos emitidos por la agenda
- **WHEN** se evalúa la evidencia de cumplimiento del sistema
- **THEN** estos eventos no acreditan auditoría, retención ni cumplimiento normativo; C-05 sigue pendiente
