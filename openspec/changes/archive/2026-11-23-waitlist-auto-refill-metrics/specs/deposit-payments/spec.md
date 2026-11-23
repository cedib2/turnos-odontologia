# Spec Delta

## ADDED Requirements

### Requirement: Depósito de waitlist sobre el ciclo de seña existente

El sistema SHALL soportar un depósito opcional por entrada de waitlist reutilizando el ciclo de seña de Mercado Pago ya definido: la entrada conserva `deposito_requerido` y `senia_id` opcional sin FK física, y el cobro SHALL crear una sola seña por turno adjudicado. El sistema MUST NOT crear tablas de pago, proveedor nuevo, caja, asiento, arqueo ni liquidación contable. El monto productivo permanece `(TBD) -> forma de obtención` (PO Parte 3 y medición del piloto).

#### Scenario: Depósito requerido con seña única

- **GIVEN** una entrada de waitlist con depósito requerido y un turno adjudicado
- **WHEN** se solicita el depósito dos veces con la misma clave
- **THEN** se crea una sola seña `pendiente` en `ARS` y el segundo intento no genera otra

#### Scenario: Entrada sin depósito

- **GIVEN** una entrada de waitlist sin depósito requerido
- **WHEN** se adjudica el hueco
- **THEN** la entrada conserva `senia_id` vacío y no se solicita ninguna seña

#### Scenario: Conciliación por flag y reembolso total

- **GIVEN** una seña de waitlist aprobada y conciliada que luego se cancela
- **WHEN** se completa la conciliación y se procesa el reembolso
- **THEN** se actualiza el flag `conciliado`, se reembolsa el total y se libera el hueco de inmediato, sin crear asiento ni caja
