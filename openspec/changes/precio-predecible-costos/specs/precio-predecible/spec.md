# Spec Delta

## Purpose

Publicar el precio único, predecible y todo incluido del producto (US-016, RN-SE-06 enmendada por C-01 A5) sin cifras inventadas ni credenciales expuestas.

## ADDED Requirements

### Requirement: Precio plano todo incluido
El sistema SHALL exponer `GET /precio` con modelo `PLANO_TODO_INCLUIDO`, la lista de capacidades incluidas con cargo adicional cero y `desgloseCostosApi=false`. El sistema MUST NOT publicar cargos adicionales ni packs dentro del plan (RN-NT-04).

#### Scenario: Consistencia entre precio publicado y detalle
- **GIVEN** monto, moneda y periodicidad configurados por el PO
- **WHEN** se consulta `GET /precio`
- **THEN** `estado=PUBLICADO`, `total` es igual a `monto`, todos los conceptos incluidos tienen cargo adicional 0 y `cargosAdicionales` está vacío

#### Scenario: Intento de construir un plan con pack o desglose
- **GIVEN** un plan con un cargo adicional, un concepto con cargo distinto de cero o `desgloseCostosApi=true`
- **WHEN** se intenta construir la respuesta
- **THEN** el sistema lo rechaza

### Requirement: Sin cifras no decididas
Si el PO no configuró el monto, el sistema SHALL responder `PENDIENTE_DEFINICION` sin `monto` ni `total`, con el pendiente `(TBD)` explícito. Una configuración parcial o inválida MUST impedir el arranque.

#### Scenario: Monto no configurado
- **GIVEN** `precio.plan.*` vacío
- **WHEN** se consulta `GET /precio`
- **THEN** `estado=PENDIENTE_DEFINICION`, sin `monto` y con el pendiente de monto `(TBD)`

#### Scenario: Configuración inválida
- **GIVEN** monto sin moneda, monto no positivo o moneda que no es ISO 4217
- **WHEN** arranca el servicio
- **THEN** falla con un error de configuración

### Requirement: Sin credenciales en la superficie
La respuesta y el código de la superficie MUST NOT contener credenciales ni nombres de variables de `.env.example`.

#### Scenario: Ausencia de credenciales
- **GIVEN** la superficie de precio
- **WHEN** se inspecciona la respuesta y el código del paquete
- **THEN** no aparecen términos como `token`, `secret` o `password` ni variables de `.env.example`
