# Tasks

Criterios de aceptación: `specs/precio-predecible/spec.md` (5 escenarios). Governance BAJO.

## 1. Modelo y servicio

- [x] 1.1 `ConceptoIncluido`, `EstadoPrecio` y `PrecioPublico` con invariantes (total = monto, sin cargos adicionales, sin desglose de API, sin cifra si está pendiente).
- [x] 1.2 `PrecioPublicoService` leyendo `precio.plan.*`, fail-fast ante configuración parcial o inválida y pendientes `(TBD)` explícitos.

## 2. Superficie

- [x] 2.1 `PrecioController` `GET /precio` público y de solo lectura.

## 3. Verificación

- [x] 3.1 `PrecioPublicoServiceTest`: pendiente sin cifra, consistencia total/monto, modelo plano, comisiones `(TBD)`, configuración inválida, rechazo de packs/desglose.
- [x] 3.2 `PrecioControllerTest`: respuesta publicada y pendiente, ausencia de credenciales y de nombres de `.env.example` en la respuesta y en el código.
- [x] 3.3 Suite completa en verde (ver `docs/decisiones/C-21-C-22-parte4.md`).
