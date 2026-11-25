# Parte 4 — C-17, C-18, C-21 y C-22

- **Responsable**: Joaquín Adrián Velázquez Carrera (Parte 4)
- **Fecha**: 2026-11-25
- **Rama**: `feat/parte4-Joaquin`
- **Migraciones**: rango V030+ reservado; C-21 y C-22 no crean migración. Propuestos: V030 = C-17, V031 = C-18.

## Secuencia aplicada (decisión del responsable de la Parte 4, 2026-11-25)

| Change | Governance | Acción en esta rama |
|---|---|---|
| C-21 `precio-predecible-costos` | BAJO | Implementado y verificado |
| C-22 `adoption-metrics-dashboard` | BAJO | Implementado y verificado con puertos para C-09/C-10/C-18 |
| C-17 `export-backup-restore` | ALTO | Solo propuesta openspec; espera revisión del PO |
| C-18 `compliance-evidence-dossier` | ALTO | Solo propuesta openspec; espera revisión del PO |

## Decisiones de implementación (sin re-decidir C-01)

- **C-21**: aplica C-01 A5=2 (precio plano todo incluido, sin desglose de API), que enmienda el scope original de C-21. El monto, la moneda y la periodicidad quedan `(TBD)` y se configuran por `precio.plan.*`; sin ellos la superficie responde `PENDIENTE_DEFINICION`. Las comisiones de Mercado Pago sobre cada seña quedan como `(TBD)` explícito para el PO.
- **C-22**: adopción online = `turnos.token_publico IS NOT NULL` sobre el total de turnos creados por `created_at` en `[desde, hasta)` (fuente aprobada en C-01 §Métricas). Ventana de 48 hs hábiles, confirmaciones y evidencia se informan `NO_DISPONIBLE` hasta que C-09, C-10 y C-18 registren su `FuenteMetrica`. Ocupación y ausentismo quedan fuera por C-19 diferido a v0.2. Cada consulta audita `metricas.consultadas` (agregado al catálogo de C-05). La superficie queda en `@Profile({"dev","test"})` como la agenda, hasta el adaptador de protección de staff.

## Evidencia

- Línea base: 194 tests verdes.
- Después del cambio: **223 tests, 0 fallos, 0 errores** (`mvn test`). Nuevos: 29 (C-21: 11; C-22: 18).
- Verificación ejecutada con PostgreSQL 16 local en lugar de Testcontainers (el entorno de desarrollo no tenía Docker); el fixture compartido no se modificó en el repositorio.

## Pendientes para el PO

- C-17: preguntas P1–P7 en `openspec/changes/export-backup-restore/design.md`.
- C-18: preguntas Q1–Q6 en `openspec/changes/compliance-evidence-dossier/design.md` (incluye el owner legal de C-01 §D).
- C-21: monto, moneda y periodicidad del plan; tratamiento de comisiones de Mercado Pago.
- C-22: metas iniciales y período de medición tras el piloto (C-01 §E).
