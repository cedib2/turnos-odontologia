# Proposal

## Why

La adopción de la reserva online es la **métrica principal de riesgo** del producto (SU-03) y las métricas verificables son un diferenciador real (DD-06). C-01 §E y §Métricas aprobaron definición, fórmula y fuente para cada métrica, con período y meta inicial `(TBD)` hasta el piloto. No existe todavía ninguna consulta que calcule esas métricas con metodología explícita ni que audite quién las consulta.

## What Changes

- Nueva capability `adoption-metrics`: consulta `GET /metricas/adopcion?desde&hasta` con período `[desde, hasta)` obligatorio.
- Cada resultado lleva `codigo`, `estado` (`CALCULADA`, `SIN_DATOS`, `NO_DISPONIBLE`), numerador, denominador, porcentaje, período y metodología (definición, fórmula, fuente, meta). Sin metodología o sin período el resultado no puede construirse.
- **Adopción online** calculada sobre `turnos`: `100 × turnos con token_publico / total de turnos creados` en el período (fuente aprobada en C-01: «canal de creación por token público link/QR»).
- **Cumplimiento de ventana** (C-09), **confirmaciones** (C-10) y **evidencia de cumplimiento** (C-18) se exponen por el puerto `FuenteMetrica`: mientras esos changes no registren su implementación, se informan `NO_DISPONIBLE` con la dependencia nombrada, nunca con una cifra.
- Ocupación y ausentismo quedan fuera: dependen de C-19, diferido a v0.2 (C-01 Gap C-19).
- Cada consulta publica el evento `metricas.consultadas` (actor, período y estado de cada métrica) en el log append-only; se agrega al catálogo de C-05.

**Fuera de alcance**: presentación visual (`visual-design=deferred`), metas numéricas, protección RBAC real de la superficie (mismo adaptador pendiente que la agenda; la superficie queda en `@Profile({"dev","test"})`).

## Capabilities

### New Capabilities
- `adoption-metrics`: tablero de métricas con metodología obligatoria.

### Modified Capabilities
- `audit-append-only-log`: el catálogo admite `metricas.consultadas`.

## Impact

- Código: `application/metricas/*`, `interfaces/web/MetricasController.java`, una línea en `audit/AuditoriaCatalogo.java`. Sin migración.
- Contrato para Partes 2/3: implementar `FuenteMetrica` con el código del catálogo (`cumplimiento_ventana`, `confirmaciones`, `evidencia_cumplimiento`).
- Tests: `ResultadoMetricaTest` (8), `MetricasAdopcionIntegrationTest` (5, PostgreSQL), `MetricasAuditoriaPersistidaTest` (1, log real), `MetricasControllerTest` (4).
- Governance: BAJO — utilidad de solo lectura.
