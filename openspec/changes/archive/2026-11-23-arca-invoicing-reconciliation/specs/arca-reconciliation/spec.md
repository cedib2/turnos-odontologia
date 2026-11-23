# Spec Delta

## Purpose

Define el criterio verificable que declara conciliado un comprobante ARCA frente a la seña aprobada y ya conciliada, sin introducir contabilidad de caja ni alterar el contrato de pagos entregado por C-15.

## ADDED Requirements

### Requirement: Criterio de conciliación comprobante–seña

El sistema SHALL fijar `conciliado=true` solo cuando el comprobante está `emitido` con CAE, su `monto` es igual al de la seña vinculada y esa seña está `aprobado` **y** `conciliado` según C-15. Cualquier otra combinación SHALL dejar `conciliado=false` y MUST NOT emitir `factura.conciliada`.

#### Scenario: Conciliación completa

- **GIVEN** una factura `emitida` con CAE, igual monto que su seña y una seña `aprobado` y `conciliado`
- **WHEN** se ejecuta la conciliación
- **THEN** el comprobante queda `conciliado=true` y se registra `factura.conciliada`

#### Scenario: Seña aún no conciliada

- **GIVEN** una factura `emitida` con CAE y monto coincidente sobre una seña `aprobado` pero no `conciliado`
- **WHEN** se ejecuta la conciliación
- **THEN** el comprobante queda `conciliado=false` y no se emite `factura.conciliada`

#### Scenario: Montos divergentes

- **GIVEN** una factura `emitida` cuyo monto difiere del de la seña
- **WHEN** se ejecuta la conciliación
- **THEN** el comprobante no queda conciliado y la divergencia se informa sin modificar la seña

#### Scenario: Comprobante no emitido

- **GIVEN** un comprobante `pendiente` o `rechazado`
- **WHEN** se ejecuta la conciliación
- **THEN** no se fija `conciliado` y no se emite evento de conciliación

### Requirement: Conciliación sin entidades contables ni efectos en pagos

El sistema SHALL limitar la conciliación a leer la seña, el presupuesto y el comprobante, y a actualizar el flag `conciliado` del comprobante. No SHALL crear turnos, presupuestos, señas, caja, asiento ni arqueo, y MUST NOT modificar el flag `conciliado` de la seña ni reabrir el contrato de pagos de C-15.

#### Scenario: Esquema sin contabilidad

- **GIVEN** el esquema resultante de la migración V022
- **WHEN** se inspeccionan sus tablas
- **THEN** no existen tablas de caja, asiento ni arqueo y solo se agregó `comprobantes`

#### Scenario: Referencia de seña inexistente

- **GIVEN** un comprobante cuya `senia_id` no resuelve a una seña
- **WHEN** se ejecuta la conciliación
- **THEN** no se crea ninguna entidad, el comprobante queda sin conciliar y se informa la inconsistencia

#### Scenario: Seña inalterada

- **GIVEN** un comprobante conciliado
- **WHEN** se inspecciona la seña vinculada
- **THEN** su estado, importe y flag `conciliado` permanecen exactamente como los dejó C-15
