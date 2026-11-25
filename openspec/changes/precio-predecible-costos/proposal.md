# Proposal — C-21 `precio-predecible-costos`

> Parte 4 — autor: Joaquín Adrián Velázquez Carrera. Governance **BAJO** (solo lectura).

## Why

«Precio predecible y todo incluido» es un diferenciador real. C-01 **A5=2** enmendó RN-SE-06 y este change: **suscripción plana todo incluido, sin desglose de costos de API** para el consultorio. El scope original («costos de API detallados por separado») queda supersedido por esa decisión.

## What Changes

- `PrecioPublicoService` + `GET /precio` (dev/test; la superficie visual sigue diferida).
- Modelo fijo `SUSCRIPCION_PLANA_TODO_INCLUIDO`, `desgloseCostosApi=false`, `packsAdicionales=false` (RN-NT-04).
- Monto, moneda y periodicidad **no están decididos**: se configuran con `precio.plan.*`. Sin ellos la respuesta es `PENDIENTE` sin cifra (DD-06).
- Contrato cerrado de campos comerciales: ninguna configuración de C-10/C-15 forma parte de la respuesta.

## Impact

`application/precio/*`, `interfaces/web/PrecioController`. Sin migración.
