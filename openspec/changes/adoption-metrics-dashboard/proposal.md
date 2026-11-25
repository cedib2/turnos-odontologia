# Proposal — C-22 `adoption-metrics-dashboard`

> Parte 4 — autor: Joaquín Adrián Velázquez Carrera. Governance **BAJO** (solo lectura). Depende de C-09 por contrato: cuenta turnos con `token_publico` y eventos `turno.reprogramado` / `turno.cancelado` con `inicioAnterior`.

## Why

La métrica principal de riesgo es la adopción de la reserva online (SU-03). C-01 E exige que cada métrica tenga definición, fórmula, fuente y período, y que las metas queden `(TBD)` hasta el piloto.

## What Changes

- `Metrica`: no se puede construir sin definición, fórmula, fuente, zona horaria y período válido.
- **Adopción online**: `100 × turnos con token público (link/QR) / turnos creados en el período`, con desglose por `origen`.
- **Cumplimiento de ventana**: `% de reprogramaciones/cancelaciones con ≥ 48 hs hábiles` (lunes a viernes, sin feriados del calendario C-07, zona AR — C-01 C2), medido contra `inicioAnterior`; los eventos sin ese dato se excluyen y se informan.
- **Evidencia de cumplimiento**: completitud del dossier de C-18.
- Confirmaciones (C-10) → `NO_DISPONIBLE`; ocupación y ausentismo (C-19, diferido a v0.2) → `DIFERIDA`. Sin cifras inventadas.
- Cada consulta queda auditada (`metricas.consultadas`).
- Cambio mínimo en C-08: `ReprogramacionService` agrega `inicioAnterior` a la metadata de `turno.reprogramado` (aditivo).

## Impact

`application/metricas/*`, `interfaces/web/MetricasController` (dev/test). Sin migración.
