# Spec Delta

## MODIFIED Requirements

### Requirement: Catálogo mínimo y granularidad por operación
Catálogo aprobado por escrito por el PO: `turno.creado`, `turno.reprogramado`, `turno.sobreturno`, `bloqueo.creado`, `publictoken.historia.denegado`, `presupuesto.creado`, `presupuesto.modificado`, cinco tipos de pago (C-15) y, desde C-16, `factura.emitida`, `factura.rechazada`, `factura.conciliada`, `factura.anulada`, `nota_credito.emitida`. Cada operación confirmada SHALL generar exactamente una fila; MUST NOT crear una fila por cambio de campo ni por operaciones rechazadas.

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
- **GIVEN** una creación de bloqueo confirmada
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

#### Scenario: Eventos de dinero confirmados
- **GIVEN** una seña con pago aprobado, conciliado y luego reembolsado
- **WHEN** se consulta el registro de auditoría
- **THEN** existen exactamente una fila por cada operación confirmada con los tipos `pago.solicitado`, `pago.aprobado`, `pago.conciliado` y `pago.reembolsado`

#### Scenario: Operación de pago rechazada sin fila
- **GIVEN** un webhook con firma inválida y un reenvío duplicado
- **WHEN** se consulta el registro de auditoría
- **THEN** no existe fila adicional de pago para esas peticiones

#### Scenario: Eventos de facturación confirmados
- **GIVEN** una factura emitida, conciliada y luego anulada por nota de crédito
- **WHEN** se consulta el registro de auditoría
- **THEN** existen exactamente una fila por cada operación confirmada con los tipos `factura.emitida`, `factura.conciliada`, `factura.anulada` y `nota_credito.emitida`

#### Scenario: Emisión rechazada sin fila
- **GIVEN** un intento de emisión con error del proveedor y un reenvío con la misma clave
- **WHEN** se consulta el registro de auditoría
- **THEN** solo existe la fila de `factura.rechazada` y no se agrega una segunda por el reenvío
