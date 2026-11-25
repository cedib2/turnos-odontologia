# Proposal

## Why

US-016 y RN-SE-06 exigen un precio **único, público, predecible y todo incluido**: es uno de los cuatro diferenciadores reales del producto (DD-06). C-01 A5=2 (PO, 2026-11-20) fijó el modelo como suscripción plana **sin desglose de costos de API** para el consultorio y enmendó el scope original de C-21, que pedía detallar Meta/Mercado Pago/ARCA por separado. Hoy no existe ninguna superficie que publique ese precio ni que garantice que no aparecen cargos sorpresa (RN-NT-04).

## What Changes

- Nueva capability `precio-predecible`: consulta pública y de solo lectura `GET /precio`.
- Modelo `PLANO_TODO_INCLUIDO`: lista de capacidades incluidas (agenda, reserva link/QR, WhatsApp con mensajería de Meta, Mercado Pago, ARCA según A1, cumplimiento), todas con cargo adicional 0; `cargosAdicionales` siempre vacío; `desgloseCostosApi=false`.
- El monto, la moneda y la periodicidad **no se inventan** (RN-GL-03): se leen de `precio.plan.monto|moneda|periodicidad`. Sin configuración la superficie responde `PENDIENTE_DEFINICION` sin cifra; configuración parcial o inválida falla al arrancar.
- Pendiente explícito `(TBD)`: tratamiento de las comisiones que Mercado Pago cobra sobre cada seña (contrato consultorio–MP), a confirmar por el PO.
- La superficie no lee credenciales ni variables de `.env.example`.

**Fuera de alcance**: UI/visual (`visual-design=deferred`), facturación del plan, integración real con C-10/C-15 (el modelo A5 ya no necesita sus costos), migraciones.

## Capabilities

### New Capabilities
- `precio-predecible`: contrato observable de la consulta del precio del plan.

## Impact

- Código: `application/precio/*`, `interfaces/web/PrecioController.java`. Sin migración (no usa el rango V030+).
- Tests: `PrecioPublicoServiceTest` (7) y `PrecioControllerTest` (4).
- Governance: BAJO — consulta de solo lectura; autonomía si pasan los tests.
