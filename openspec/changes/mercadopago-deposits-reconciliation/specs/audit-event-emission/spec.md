# Spec Delta

## ADDED Requirements

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

## MODIFIED Requirements

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
