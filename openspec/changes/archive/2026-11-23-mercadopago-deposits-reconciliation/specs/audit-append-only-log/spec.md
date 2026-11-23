# Spec Delta

## MODIFIED Requirements

### Requirement: Catálogo mínimo y granularidad por operación
El catálogo autorizado se limita a `turno.creado`, `turno.reprogramado`, `turno.sobreturno`, `bloqueo.creado`, `publictoken.historia.denegado`, `presupuesto.creado`, `presupuesto.modificado` y, desde C-15, `pago.solicitado`, `pago.aprobado`, `pago.rechazado`, `pago.reembolsado` y `pago.conciliado`, todos aprobados por escrito por el PO. Cada operación de dominio confirmada SHALL generar exactamente una fila; el sistema MUST NOT crear una fila por cambio de campo ni por operaciones rechazadas.

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
