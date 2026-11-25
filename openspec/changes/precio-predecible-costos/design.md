# Design — C-21

## Context
C-01 A5=2 fijó el modelo: **suscripción plana todo incluido sin desglose de costos de API**, y enmendó RN-SE-06 y este change. Monto, moneda y periodicidad no están decididos. La superficie visual está diferida (`visual-design=deferred`).

## Decisiones

### 1. Configuración, no base de datos
El plan se lee de `precio.plan.*` (`nombre`, `monto`, `moneda`, `periodicidad`, `incluye`, `vigente-desde`). Hay un único precio vigente y no hace falta historial en este change, así que no se agrega migración. Se valida al arrancar: monto positivo, moneda ISO 4217 y periodicidad `MENSUAL`/`ANUAL`. Una configuración inválida impide levantar la app, para no publicar un precio incoherente.

### 2. Estado `PENDIENTE` sin cifra
Si falta monto, moneda o periodicidad, la respuesta es `PENDIENTE`, con `monto`/`moneda` nulos y una nota `(TBD)`. Así se cumple DD-06 (ninguna cifra sin respaldo) y RN-GL-03 (sin valores por convención).

### 3. Contrato cerrado de campos
`PrecioPublico` es un record con campos comerciales únicamente; el servicio no recibe ninguna configuración de C-10/C-15. El test verifica que el JSON tenga **exactamente** esos campos y ninguna credencial, aunque haya tokens de Mercado Pago o WhatsApp en el entorno.

### 4. Banderas explícitas
`todoIncluido=true`, `desgloseCostosApi=false` y `packsAdicionales=false` son constantes del modelo A5=2 (RN-NT-04). Cambiarlas requiere una nueva decisión del PO, no configuración.

## Fuera de alcance
UI de precio, historial de precios, facturación del plan.
