# Spec Delta

## ADDED Requirements

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
- **GIVEN** el catálogo de tipos de auditoría tras el cambio
- **WHEN** se inspeccionan los tipos disponibles
- **THEN** no hay tipos de caja, seña, pago, cobro, reembolso ni factura

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
