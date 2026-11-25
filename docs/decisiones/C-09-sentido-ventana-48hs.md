# C-09 — Sentido de la ventana de 48 hs hábiles

- **Fecha**: 2026-11-25
- **Decisor**: José Donato Fornasari Donadel
- **Reglas**: RN-AG-05, DD-07, C-01 C2
- **Changes**: C-09 (reserva pública) y C-22 (métrica de ventana)

## Contexto

C-01 C2 definió cómo se cuentan las 48 hs hábiles (2 días hábiles completos, sin fines de semana ni feriados, en `America/Argentina/Buenos_Aires`), pero no en qué sentido se aplica la ventana. La implementación y la spec de C-09 aceptaban confirmar, cancelar o reprogramar por link **solo cuando faltaban menos** de 2 días hábiles (`now >= vencimiento`). En cambio:

- el escenario «Cancelación fuera de ventana» de la misma spec rechazaba los turnos a menos de 2 días hábiles;
- el error `fuera_de_ventana` deriva a recepción, que es lo esperable cuando el paciente llega tarde;
- C-22 mide como cumplimiento los cambios hechos con al menos 48 hs hábiles de anticipación.

## Decisión

1. **Cancelar y reprogramar por link se puede hasta el vencimiento inclusive** (`now <= vencimiento`, 23:59:59 del segundo día hábil anterior al turno). Después, el cambio lo hace recepción (`409`, `derivarARecepcion=true`).
2. **Confirmar la asistencia no depende de la ventana:** se puede hasta el inicio del turno. Si el turno ya empezó, responde `409` con `derivarARecepcion=true`.

## Cambios

- `VentanaHabilesService.dentroDeVentana` invierte la condición; se agrega `antesDelInicio` para la confirmación.
- `PublicBookingService.confirmar` deja de usar la ventana.
- Tests: `VentanaHabilesServiceTest` (límite inclusive y un segundo después, confirmación) y `PublicBookingIntegrationTest` (los turnos de prueba se crean a 10 días; el caso fuera de ventana usa el próximo día hábil).
- Spec `public-booking`: requisitos de la ventana y de la confirmación.
- Frontend: los textos vuelven a indicar «hasta 48 horas hábiles antes».
- C-22: la inconsistencia anotada en su diseño queda resuelta. Los cambios por link siempre cumplen la ventana; la métrica sirve para controlar los que hace el staff.
