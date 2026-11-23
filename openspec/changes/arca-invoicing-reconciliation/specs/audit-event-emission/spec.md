# Spec Delta

## ADDED Requirements

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
