# Proposal — C-22 `adoption-metrics-dashboard`

> Parte 4 — autor: Joaquín Adrián Velázquez Carrera. Governance **BAJO** (solo lectura). Archivado el 2026-11-25 al completarse sus dependencias (C-09, C-10, C-19) en `demo-integracion`. Depende de C-09 por contrato: cuenta turnos con `token_publico` y eventos `turno.reprogramado` / `turno.cancelado` con `inicioAnterior`.

## Why

La métrica principal de riesgo es la adopción de la reserva online (SU-03). C-01 E exige que cada métrica tenga definición, fórmula, fuente y período, y que las metas queden `(TBD)` hasta el piloto.

## What Changes

- `Metrica`: no se puede construir sin definición, fórmula, fuente, zona horaria y período válido.
- **Adopción online**: `100 × turnos con token público (link/QR) / turnos creados en el período`, con desglose por `origen`.
- **Cumplimiento de ventana**: `% de reprogramaciones/cancelaciones con ≥ 48 hs hábiles` (lunes a viernes, sin feriados del calendario C-07, zona AR — C-01 C2), medido contra `inicioAnterior`; los eventos sin ese dato se excluyen y se informan.
- **Evidencia de cumplimiento**: completitud del dossier de C-18.
- **Confirmaciones** (C-10): `100 × turnos con confirmación respondida / turnos con solicitud de confirmación enviada`, desde el outbox de WhatsApp.
- **Ocupación y ausentismo** (C-19): se muestran los valores que C-19 publica por sillón y mes, con su metodología; C-22 no los agrega ni los recalcula.
- Sin datos en el período, la métrica queda `SIN_DATOS`. Sin cifras inventadas.
- C-09: la cancelación y la reprogramación por link informan `inicioAnterior` (aditivo).

> Actualizado el 2026-11-25 al integrar C-09, C-10 y C-19: la versión original informaba confirmaciones como `NO_DISPONIBLE` y ocupación/ausentismo como `DIFERIDA`, porque esas fuentes todavía no existían.
- Cada consulta queda auditada (`metricas.consultadas`).
- Cambio mínimo en C-08: `ReprogramacionService` agrega `inicioAnterior` a la metadata de `turno.reprogramado` (aditivo).

## Impact

`application/metricas/*`, `interfaces/web/MetricasController` (dev/test). Sin migración.
