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
El sistema SHALL emitir y persistir un evento por la creación de turno, la reprogramación, el sobreturno con override y la creación de bloqueos, y también SHALL persistir `publictoken.historia.denegado` cuando C-06 lo emite, sin cambiar su semántica de identidad no verificada. En el sobreturno, el evento MUST incluir el `motivo` y el `actor` que autorizaron la excepción (RN-GL-01). Cada evento SHALL generar una sola fila en `auditoria_log`.

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

### Requirement: Eventos de la superficie pública de reserva
El sistema SHALL emitir `turno.confirmado` y `turno.cancelado` para las operaciones públicas confirmadas, y SHALL reutilizar `turno.creado` y `turno.reprogramado` cuando el turno se originó en enlace. El actor SHALL registrar modo `PACIENTE` sin identidad verificada. Una operación rechazada MUST NOT emitir evento de confirmación.

#### Scenario: Confirmación y cancelación públicas
- **GIVEN** una confirmación y una cancelación pública exitosas
- **WHEN** se consulta el registro de eventos
- **THEN** se encuentran `turno.confirmado` y `turno.cancelado`, uno por operación, con actor modo `PACIENTE`

#### Scenario: Reprogramación pública
- **GIVEN** una reprogramación pública exitosa
- **WHEN** se inspecciona el evento
- **THEN** usa el tipo existente `turno.reprogramado` y conserva el modo `PACIENTE`

#### Scenario: Operación rechazada sin evento
- **GIVEN** una operación pública rechazada por token inválido, hueco tomado, fuera de ventana o reenvío duplicado
- **WHEN** se consulta el registro de eventos
- **THEN** no existe evento de confirmación, cancelación ni creación para esa operación

### Requirement: Metadata sanitizada de eventos públicos
La metadata de los eventos públicos SHALL limitarse a `turnoId`, `canal=link`, `origen=link`, hash de la `idempotency_key` y `motivo` cuando corresponda. MUST NOT incluir el token, su `jti`, `telefono`, `email`, `dni` ni otros datos personales; el `correlationId` SHALL ser distinto por operación. No se crea tipo de auditoría para notificaciones.

#### Scenario: Metadata permitida
- **GIVEN** una operación pública confirmada
- **WHEN** se inspecciona la metadata del evento
- **THEN** contiene solo las claves permitidas y un `correlationId` propio de la operación

#### Scenario: Sin token ni datos personales
- **GIVEN** una operación pública que transporta token e `idempotency_key`
- **WHEN** se serializa el evento
- **THEN** no aparecen el token, su `jti`, la clave en claro ni datos de contacto o documento del paciente

#### Scenario: Sin evento de entrega
- **GIVEN** la solicitud de notificación simulada de una operación pública
- **WHEN** se consulta el catálogo de tipos de auditoría
- **THEN** no existe un tipo de notificación o entrega y los eventos no acreditan envío real de WhatsApp

### Requirement: Eventos de auditoría de presupuestos
El sistema SHALL emitir y persistir `presupuesto.creado` al emitir un presupuesto y `presupuesto.modificado` al modificarlo o anularlo, con entidad `presupuesto`, `entidadId` igual al identificador del presupuesto y actor `ActorSimulado.recepcionista()` con modo `SIMULADO`. Una operación rechazada MUST NOT emitir evento. No se crea tipo de evento de caja, cobro, reembolso ni facturación en este change.

#### Scenario: Emisión y modificación
- **GIVEN** una creación y una modificación exitosas de presupuestos
- **WHEN** se consulta el registro de eventos
- **THEN** se encuentran `presupuesto.creado` y `presupuesto.modificado`, uno por operación, con actor modo `SIMULADO`

#### Scenario: Anulación como modificación
- **GIVEN** un presupuesto `emitido`
- **WHEN** se anula
- **THEN** se emite `presupuesto.modificado` con `motivo=anulado`, sin tipo de evento propio para la anulación

#### Scenario: Operación rechazada sin evento
- **GIVEN** una creación o modificación rechazada por ítem inválido, presupuesto anulado o entidad inexistente
- **WHEN** se consulta el registro de eventos
- **THEN** no existe evento de presupuesto para esa operación

#### Scenario: Sin eventos de dinero
- **GIVEN** el catálogo de tipos de auditoría tras C-14
- **WHEN** se inspeccionan los tipos disponibles en ese momento
- **THEN** no hay tipos de caja, seña, pago, cobro, reembolso ni factura en el alcance de C-14; C-15 incorpora los cinco tipos de pago aprobados por el PO

### Requirement: Metadata de eventos de presupuestos
La metadata de los eventos de presupuestos SHALL limitarse a `presupuestoId`, `turnoId` cuando exista, `total`, `moneda`, `cantidadItems` y `motivo` cuando corresponda. MUST NOT incluir nombre, teléfono, email, DNI, token, credenciales ni datos de comprobante. Cada evento SHALL llevar un `correlationId` distinto por operación.

#### Scenario: Metadata permitida
- **GIVEN** una operación de presupuesto exitosa con y sin turno asociado
- **WHEN** se inspecciona la metadata del evento
- **THEN** contiene solo las claves permitidas, `ARS` como moneda y un `correlationId` propio de la operación

#### Scenario: Sin datos personales ni secretos
- **GIVEN** una operación de presupuesto que conoce los datos del paciente y del catálogo
- **WHEN** se serializa el evento
- **THEN** no aparecen datos personales, tokens, credenciales ni campos de facturación

### Requirement: Eventos de auditoría de señas y pagos

El sistema SHALL emitir y persistir `pago.solicitado`, `pago.aprobado`, `pago.rechazado`, `pago.reembolsado` y `pago.conciliado` con entidad `senia` y su identificador. El webhook SHALL usar actor `SYSTEM`; solicitud y reembolso, actor de recepción `SIMULADO`. Cada operación confirmada SHALL generar una sola fila; rechazos y duplicados MUST NOT generar una segunda.

#### Scenario: Pago aprobado y conciliado

- **GIVEN** una seña aprobada y conciliada contra un turno existente
- **WHEN** se consulta el registro de eventos
- **THEN** se encuentran `pago.aprobado` y `pago.conciliado`, uno por operación, con entidad `senia` y modo `SYSTEM`

#### Scenario: Pago rechazado

- **GIVEN** un webhook de rechazo válido para una seña pendiente
- **WHEN** se procesa el evento
- **THEN** se persiste `pago.rechazado` con modo `SYSTEM` y el turno permanece confirmado

#### Scenario: Reembolso solicitado por recepción

- **GIVEN** una seña aprobada
- **WHEN** recepción solicita y confirma el reembolso
- **THEN** se persiste `pago.reembolsado` con actor de recepción en modo `SIMULADO`

#### Scenario: Webhook duplicado o firma inválida

- **GIVEN** un webhook ya procesado y otro con firma inválida
- **WHEN** ambos se presentan al endpoint
- **THEN** no se inserta ninguna fila adicional de auditoría para esas peticiones

### Requirement: Metadata de eventos de señas y pagos

La metadata de los eventos de pagos SHALL limitarse a `seniaId`, `turnoId` y `presupuestoId` cuando existan, `monto` como texto, `moneda`, `estado`, `externalReference`, `motivo` y `correlationId`. MUST NOT incluir claves de la lista prohibida (secretos, credenciales, datos personales, clínicos ni de firma), el secreto del proveedor ni el token público. Cada evento SHALL llevar un `correlationId` distinto por operación.

#### Scenario: Metadata permitida

- **GIVEN** una operación de pago confirmada con turno y presupuesto asociados
- **WHEN** se inspecciona la metadata persistida
- **THEN** contiene solo las claves permitidas, `ARS` como moneda y un `correlationId` propio de la operación

#### Scenario: Sin secretos ni datos personales

- **GIVEN** una petición de pago que transporta firma, secreto y datos del paciente
- **WHEN** se serializa el evento
- **THEN** no aparecen la firma, el secreto, el token ni datos personales o clínicos

#### Scenario: Correlación por operación

- **GIVEN** dos eventos de pagos independientes
- **WHEN** se emiten y persisten
- **THEN** cada uno conserva un `correlationId` distinto

### Requirement: Eventos de auditoría de facturación

El sistema SHALL emitir y persistir `factura.emitida`, `factura.rechazada`, `factura.conciliada`, `factura.anulada` y `nota_credito.emitida` con entidad `comprobante` y su identificador. Emisión, rechazo, reintento, anulación y nota de crédito SHALL usar recepción `SIMULADO`; conciliación SHALL usar `system` en modo `SYSTEM`. Cada operación confirmada SHALL generar una sola fila; rechazos y reenvíos MUST NOT generar otra.

#### Scenario: Emisión y rechazo

- **GIVEN** una emisión confirmada y luego un intento rechazado
- **WHEN** se consulta el registro de eventos
- **THEN** se encuentran `factura.emitida` y `factura.rechazada`, uno por operación, con entidad `comprobante` y modo `SIMULADO`

#### Scenario: Conciliación del comprobante

- **GIVEN** un comprobante emitido cuya seña está aprobada y conciliada
- **WHEN** se completa la conciliación
- **THEN** se persiste `factura.conciliada` con actor `system` en modo `SYSTEM`

#### Scenario: Nota de crédito y anulación

- **GIVEN** una factura emitida sobre una seña reembolsada
- **WHEN** recepción confiere la nota de crédito
- **THEN** se persisten `factura.anulada` y `nota_credito.emitida`, uno por operación, con modo `SIMULADO`

#### Scenario: Reenvío y transición inválida sin fila

- **GIVEN** una emisión ya registrada, un reenvío con la misma clave y una reemisión de un comprobante anulado
- **WHEN** se procesan esas tres peticiones
- **THEN** no se inserta ninguna fila adicional de facturación

### Requirement: Metadata de eventos de facturación

La metadata SHALL limitarse a `comprobanteId`, `seniaId`, `presupuestoId` y `turnoId` cuando existan, `monto` como texto, `moneda`, `tipo`, `estado`, `motivo` cuando corresponda y `correlationId`. MUST NOT incluir CAE, DNI, documento, dirección, credenciales, secretos ni datos clínicos. Cada evento SHALL llevar un `correlationId` distinto por operación.

#### Scenario: Metadata permitida

- **GIVEN** una emisión confirmada con seña, presupuesto y turno asociados
- **WHEN** se inspecciona la metadata persistida
- **THEN** contiene solo las claves permitidas, `ARS` como moneda y un `correlationId` propio de la operación

#### Scenario: Sin CAE ni datos sensibles

- **GIVEN** una operación que conoce el CAE, las credenciales y los datos del receptor
- **WHEN** se serializa el evento
- **THEN** no aparecen el CAE, las credenciales, el DNI, la dirección ni datos clínicos

#### Scenario: Correlación por operación

- **GIVEN** dos operaciones de facturación independientes
- **WHEN** se emiten y persisten sus eventos
- **THEN** cada uno conserva un `correlationId` distinto
