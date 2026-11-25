# Tasks

Referencias: `docs/decisiones/C-19-decisiones.md` (W-1..W-13), `proposal.md`, `design.md`, `specs/waitlist-auto-refill/spec.md`, `specs/occupancy-metrics/spec.md` y deltas de `agenda`, `deposit-payments`, `audit-event-emission` y `audit-append-only-log`. Gobernanza MEDIO con dos checkpoints (W-11): **Checkpoint 1** al cerrar el grupo 3 y **Checkpoint 2** al cerrar el grupo 6. Ninguna tarea autoriza commits ni cambios en `CHANGES.md` antes de la verificación técnica.

> Restricciones de todo el plan: `V001`–`V022` intactas; no reabrir C-09/C-14/C-15/C-16; sin secretos ni dependencias nuevas; `Clock` inyectado y zona fija en métricas; defaults de prueba en `application-test.yml`; línea base de 320 tests verdes.

## 1. Esquema y persistencia (V023)

- [x] 1.1 Crear `V023__waitlist_and_metrics.sql` con `lista_espera` (candidato, profesional, sillón, rango deseado, `deposito_requerido`, `senia_id` opcional sin FK, estado y auditoría) y `metricas_ocupacion` (`sillon_id`, `periodo`, `ocupacion`, `ausentismo`, `metodologia` no vacía, `calculada_en`, auditoría) con índices y checks, verificando con `Migration023Test` sobre `PostgresFlywayTest` que V023 aplica limpia, V001–V022 quedan intactas y no existen tablas de caja, asiento, arqueo ni notificación (W-2, W-5, W-9).
- [x] 1.2 Implementar las entidades `EntradaWaitlist` y `MetricaOcupacion` y sus repositorios sobre `AuditableEntity` con borrado lógico, verificando con tests de persistencia que la entrada duplicada por candidato y rango se rechaza, que `senia_id` sin FK se conserva y que la métrica sin metodología no se persiste (W-2, W-5, W-9).

## 2. Alta y ofrecimiento simulado

- [x] 2.1 Implementar el alta de entradas con validación de duplicados y emisión de `waitlist.creado`, verificando con tests que el alta no crea turno ni ocupación y que un duplicado se rechaza sin auditoría (W-10, spec `waitlist-auto-refill`).
- [x] 2.2 Definir `NotificacionWaitlistPort` y su adaptador NO-OP/simulado en `dev`/`test`, verificando con tests que el ofrecimiento emite `waitlist.ofrecido`, que no se realiza llamada de red y que no se requiere plantilla ni credencial de WhatsApp (W-3).
- [x] 2.3 Implementar la expiración con `Clock` inyectado y `waitlist.oferta.minutos`, verificando con tests deterministas que una oferta vencida emite `waitlist.expirado`, pasa al siguiente candidato y no crea seña para el expirado (W-6).

## 3. Adjudicación atómica — Checkpoint 1

- [x] 3.1 Implementar `RellenoWaitlistService.adjudicarTurnoLiberado(turnoId)` sobre turno cancelado y ocupación desactivada, verificando con tests que un hueco libre se adjudica al primer candidato, que un hueco ocupado se rechaza sin efectos y que no se modifica C-09 ni se usa `auditoria_log` como bus (W-4).
- [x] 3.2 Registrar `waitlist.adjudicar` en `public_idempotencia` (V004) con clave SHA-256 del turno liberado, crear la ocupación por el camino de servicio existente y emitir `waitlist.adjudicado` en la misma transacción, verificando con tests que el reenvío no crea otra ocupación y que un fallo de auditoría revierte clave, ocupación y estado (W-7).
- [x] 3.3 Verificar el **Checkpoint 1** (W-11): esquema V023, alta, ofrecimiento, expiración y adjudicación atómica con sus tests, sin tocar V001–V022 ni archivos de C-09/C-14/C-15/C-16; dejar constancia del resultado antes de continuar.

## 4. Depósito y conciliación

- [x] 4.1 Invocar `SeniaService.solicitar` para la adjudicación con `deposito_requerido`, heredando `turno_id` y conservando `senia_id` en la entrada, verificando con tests que dos solicitudes con la misma clave crean una sola seña `pendiente` en `ARS` y que una entrada sin depósito no crea seña (W-5).
- [x] 4.2 Reutilizar `conciliar` y `reembolsar` de C-15, verificando con tests que el flag `conciliado` se actualiza, que el reembolso es total y libera el hueco de inmediato y que no se crean caja, asiento, arqueo ni liquidación (W-8, C-01 A4, MP-3, AR-3).
- [x] 4.3 Registrar el monto productivo como `(TBD) -> forma de obtención` (PO Parte 3 y piloto) y usar en `test` solo un importe determinista no productivo, verificando con tests que ninguna cifra de negocio aparece como default y que no se agrega variable secreta (W-5, W-12).

## 5. Métricas de ocupación y ausentismo

- [x] 5.1 Implementar el cálculo de ocupación con `turnos` y `bloqueos` activos sobre horarios y feriados, bloqueos contando como ocupados, verificando con tests el ejemplo 70% y el caso de denominador cero sin cifra publicada (W-9).
- [x] 5.2 Implementar el registro de `turno.inasistencia` y el cálculo de ausentismo sobre turnos confirmados, con cancelados fuera del numerador, verificando con tests los ejemplos 15% y 5% y que un turno cancelado nunca se infiere como inasistencia (W-9, W-10).
- [x] 5.3 Persistir `metodologia` con fórmula, fuente, sillón y período `YYYY-MM` en `America/Argentina/Buenos_Aires`, obteniendo la hora de un `Clock` inyectado, verificando con tests que una publicación sin metodología o período se rechaza y que la cifra se devuelve con su método (W-9).
- [x] 5.4 Registrar las metas iniciales como `(TBD) -> forma de obtención` (baseline del piloto y aprobación del PO Parte 3), verificando con tests que la superficie devuelve cifra, metodología y período sin comparación contra una meta inventada (W-9).

## 6. Auditoría aprobada por el PO — Checkpoint 2

- [x] 6.1 Ampliar `AuditoriaCatalogo` con `waitlist.creado`, `waitlist.ofrecido`, `waitlist.adjudicado`, `waitlist.expirado`, `turno.inasistencia` y `metrica.publicada`, verificando con un test por escenario que se persiste una fila por operación confirmada con entidad `lista_espera`, `turno` o `metrica`, actores de recepción (`SIMULADO`) y `system` (`SYSTEM`), y que rechazos y reenvíos no agregan filas (W-10, Decisión 1 de C-05).
- [x] 6.2 Construir la metadata acotada a `entradaId`, `turnoId`, `seniaId`, `metricaId`, `sillonId`, `periodo`, `ocupacion`, `ausentismo`, `metodologia`, `motivo` y `correlationId`, verificando con tests que no aparecen DNI, teléfono, email, diagnóstico, historia clínica, credenciales ni secretos y que cada operación tiene un `correlationId` distinto (W-10).
- [x] 6.3 Verificar el **Checkpoint 2** (W-11): depósito, conciliación, métricas y auditoría con sus tests, sin caja/asiento/arqueo, sin WhatsApp real y sin reabrir los changes archivados; dejar constancia del resultado antes de la verificación final.

## 7. Superficie y wiring

- [x] 7.1 Exponer el alta de entradas, la consulta por candidato, la ejecución del relleno y la consulta de métricas en rutas indicativas `/lista-espera` y `/metricas` bajo `@Profile({"dev","test"})`, verificando con tests MVC que responden sin credenciales de staff, sin token de C-06, sin RBAC nuevo y sin modificar `/auth/**` (W-12).
- [x] 7.2 Registrar el paquete `com.turnosodontologia.waitlist` en `TestJpaConfig` y declarar `waitlist.oferta.minutos` y `waitlist.metricas.periodo` con defaults seguros en `application-test.yml`, verificando que el contexto `test` inicia sin secretos ni variables ausentes y que las suites de C-08, C-09, C-15 y C-16 siguen verdes (W-12).

## 8. Verificación final del change

- [x] 8.1 Ejecutar la suite completa sobre Testcontainers/PostgreSQL desde la línea base de 320 tests verdes, verificando idempotencia, adjudicación atómica, depósito, conciliación, reembolso, métricas, auditoría, `Migration023Test` y ausencia de secretos o dependencia del reloj y la zona horaria local (W-6, W-11, W-12).
- [x] 8.2 Ejecutar `openspec validate waitlist-auto-refill-metrics --strict` y `openspec status --change waitlist-auto-refill-metrics`, verificando cero errores, los cuatro artefactos completos y los seis deltas de specs (W-11).
- [x] 8.3 Revisar que `CHANGES.md` siga sin marcar C-19 hasta la verificación técnica y que los diferidos del acta y de `design.md` (cableado automático, WhatsApp, montos y metas productivas, caja/asiento/arqueo, rol `dirección`, RBAC, aplicación ejecutable y gestor de secretos) no tengan tareas en este change (W-11, W-13).

## Workflow follow-up

- Registrar el resultado de los checkpoints W-11 en `docs/decisiones/C-19-decisiones.md` durante el apply, sin reabrir el acta aprobada.
- Marcar C-19 en `CHANGES.md` solo después de la verificación técnica de 8.1 y 8.2.
- Archivar el change con el flujo de OPSX una vez cumplida la gobernanza MEDIO.
