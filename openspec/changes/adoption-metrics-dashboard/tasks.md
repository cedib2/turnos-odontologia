# Tasks

Criterios de aceptación: `specs/adoption-metrics/spec.md` (6 escenarios) y `specs/audit-append-only-log/spec.md` (2 requirements MODIFIED). Governance BAJO.

## 1. Modelo

- [x] 1.1 `Periodo`, `MetodologiaMetrica`, `EstadoMetrica` y `ResultadoMetrica` con metodología y período obligatorios.
- [x] 1.2 `CatalogoMetricas` con las definiciones de C-01 §Métricas y meta `(TBD)`.

## 2. Cálculo y auditoría

- [x] 2.1 `AdopcionOnlineFuente` (conteo por token público y `created_at`).
- [x] 2.2 `MetricasAdopcionService` con `NO_DISPONIBLE` para fuentes ausentes y evento `metricas.consultadas`.
- [x] 2.3 Agregar `metricas.consultadas` a `AuditoriaCatalogo`.

## 3. Superficie

- [x] 3.1 `MetricasController` `GET /metricas/adopcion` con 400 ante período inválido.

## 4. Verificación

- [x] 4.1 Tests unitarios, de integración PostgreSQL, de log persistido y de controlador.
- [x] 4.2 Suite completa en verde (ver `docs/decisiones/C-21-C-22-parte4.md`).
- [x] 4.3 `MetricasAdopcionService` con `@Transactional`: el evento comparte transacción con la consulta (requirement «Transacción compartida» de C-05).

## Follow-up

- Conectar `FuenteMetrica` de C-09, C-10 y C-18 cuando existan (responsables: Partes 3, 2 y 4). No bloquea el archive de C-22.
- Aprobación del PO del evento `metricas.consultadas` (Decisión 1 de C-05): ver `docs/decisiones/C-22-propuesta-evento-metricas-consultadas.md`. **Sí bloquea el archive.**
