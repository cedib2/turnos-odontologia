# Spec Delta — precio-publico

## ADDED Requirements

### Requirement: Precio único todo incluido
El sistema SHALL exponer un único precio público con modelo `SUSCRIPCION_PLANA_TODO_INCLUIDO` (C-01 A5=2), sin desglose de costos de API ni packs adicionales (RN-SE-06, RN-NT-04). Mientras monto, moneda o periodicidad no estén aprobados, MUST responder `PENDIENTE` sin cifra.

#### Scenario: Precio sin aprobar
- **GIVEN** que no hay monto configurado
- **WHEN** se consulta el precio
- **THEN** el estado es `PENDIENTE` y no se publica ninguna cifra

#### Scenario: Precio publicado
- **GIVEN** monto, moneda y periodicidad configurados
- **WHEN** se consulta el precio
- **THEN** la respuesta coincide con lo configurado e indica todo incluido sin desglose

### Requirement: Superficie sin credenciales
La respuesta MUST contener solo los campos comerciales del contrato y MUST NOT exponer tokens, secretos ni configuración de integraciones.

#### Scenario: Integraciones configuradas
- **GIVEN** credenciales de Mercado Pago y WhatsApp presentes en el entorno
- **WHEN** se consulta `/precio`
- **THEN** la respuesta solo tiene los campos del contrato y no contiene credenciales
