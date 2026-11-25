# Tasks — C-22

- [x] 1. `Metrica` con metodología obligatoria y meta `(TBD)` por defecto.
- [x] 2. `CalculadoraHorasHabiles` (C-01 C2) con feriados configurables.
- [x] 3. `MetricasService`: adopción, ventana, tablero con estados explícitos (sin cifras inventadas), auditoría por consulta.
- [x] 4. `inicioAnterior` en `turno.reprogramado` (contrato con C-09 para `turno.cancelado`).
- [x] 5. Tests `MetricasAdopcionTest` (5) y `CalculadoraHorasHabilesTest` (4); aserción nueva en `ReprogramacionIntegrationTest`.
- [x] 6. C-09 emite `turno.cancelado` y `turno.reprogramado` con `inicioAnterior` y marca `token_publico` en reservas públicas (aserciones en `PublicBookingIntegrationTest`).
- [x] 7. Confirmaciones desde el outbox de C-10 y ocupación/ausentismo desde lo publicado por C-19; tests nuevos en `MetricasAdopcionTest` (7 en total).
