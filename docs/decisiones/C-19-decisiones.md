# C-19 — Decisiones del PO para `waitlist-auto-refill-metrics` (Parte 3)

- **Change**: `C-19 waitlist-auto-refill-metrics`
- **Governance local**: **MEDIO** — pipeline y lógica de negocio; checkpoints (W-11)
- **Decisor**: Product Owner del equipo de Parte 3
- **Fecha**: 2026-11-23
- **Estado**: aprobado — opciones recomendadas más simples, seguras y alineadas con C-01..C-16
- **Alcance de este documento**: cerrar los 13 puntos abiertos del explore (`W-1`..`W-13`) para habilitar `/opsx:propose`. No implementa código, no crea migraciones, no modifica `src/` y no autoriza commits.
- **Fuentes**: `CHANGES.md` §C-19 y §«Reservas de numeración de migraciones Flyway», `docs/decisiones/C-01-decisiones-habilitantes.md` (Gap C-19, E, A4), `C-03-decisiones-core-models.md` (D7), `C-05-ajustes-pre-apply-PO.md` (Decisión 1), `C-09-decisiones.md` (D4), `C-14-decisiones.md` (D15), `C-15-decisiones.md` (MP-3/MP-5/MP-7), `C-16-decisiones.md` (AR-3/AR-13), `knowledge-base/01,04,05,06,07,10`, `openspec/specs/{agenda,deposit-payments,audit-event-emission}` y relevamiento de `src/`.

> Estas decisiones son la respuesta exigida por RN-GL-03: ningún `(TBD)` se resuelve por convención del agente. Donde no hay cifra de negocio definida se documenta **`(TBD) -> forma de obtención`**, con decisor **PO Parte 3** y fecha **2026-11-23**. Las decisiones cerradas en C-01, C-09, C-14, C-15 y C-16 no se reabren.

## Decisiones aprobadas

### W-1 — Habilitación en v0.1 (supersede Gap C-19)

- **Decisión**: incluir C-19 en v0.1. Se **supersede** la decisión C-01 «Gap C-19» (2026-11-20) que lo difería a v0.2; el PO Parte 3 habilita el change en esta fecha con el alcance recortado de W-2..W-13. El resto de C-01 (A4 reembolso total, A5 precio plano, C2 zona horaria, E metodología) permanece vigente.
- **Justificación**: el relleno automático con depósito y las métricas verificables son diferenciadores reales; el PO asume el riesgo y fija el alcance mínimo verificable sin reabrir los changes archivados.

### W-2 — Numeración y esquema de Flyway

- **Decisión**: crear **una sola migración `V023__waitlist_and_metrics.sql`** con `lista_espera` y `metricas_ocupacion` (índices y checks). `V001`–`V022` quedan intactas; no se altera `senias`, `turnos`, `ocupaciones` ni `public_idempotencia`. La cifra histórica «013» del scope queda supersedida por la tabla de reservas.
- **Justificación**: respeta la Parte 3 (V020=C-14, V021=C-15, V022=C-16) y evita tocar migraciones aplicadas. Un solo número nuevo simplifica la verificación de `V001`–`V022` intactas.

### W-3 — Canal de ofrecimiento sin C-10

- **Decisión**: C-19 **no reabre C-10** (sigue pendiente) y **no envía WhatsApp real**. El ofrecimiento pasa por un puerto `NotificacionWaitlistPort` con implementación **NO-OP/simulada** en `dev`/`test`, igual que `NotificacionPublicaPort` (C-09 D8) y `NotificacionPagoPort` (MP-9). No se afirma entrega ni se agrega plantilla.
- **Justificación**: mantiene el acoplamiento detrás de un puerto y evita introducir outbox, plantillas Meta ni dependencias nuevas sin la decisión de C-10.

### W-4 — Disparador del relleno

- **Decisión**: no hay gancho de «hueco liberado» en `src/` y C-09 está archivado. El motor se expone como caso de uso explícito `RellenoWaitlistService.adjudicarTurnoLiberado(turnoId)`: recibe un turno ya cancelado, verifica que su `Ocupacion` esté desactivada y que no exista adjudicación previa, y adjudica al primer candidato elegible. **No se modifica C-09, no se agrega `ApplicationEventListener` ni se usa el log de auditoría como bus** (MP-5). El cableado automático al flujo de cancelación queda como **pendiente explícito** para un change posterior.
- **Justificación**: es la opción determinista y verificable sin reabrir C-09 ni inventar un bus de eventos; la entrada es un estado observable del dominio, no una señal asíncrona.

### W-5 — Modelo del depósito

- **Decisión**: **no crear tabla de pagos ni proveedor nuevo**. La entrada de waitlist tiene `deposito_requerido` (booleano configurable por recepción) y `senia_id` opcional **sin FK física** (patrón MP-3/AR-2). El cobro se hace invocando `SeniaService.solicitar` de C-15 tal cual, con `turno_id` del turno adjudicado; la distinción «depósito de waitlist» vive en `lista_espera` y en metadata de auditoría, sin alterar `senias` ni el código de C-15. **El monto del depósito es `(TBD) -> forma de obtención`**: lo fija el PO Parte 3 con el piloto; en `test` se usa un importe determinista de prueba no productivo, nunca un valor de negocio inventado.
- **Justificación**: reutiliza la integración MP ya aprobada y probada (HMAC, idempotencia, conciliación y reembolso), sin modificar un change archivado ni duplicar lógica de dinero.

### W-6 — Plazo de expiración de la oferta

- **Decisión**: el plaza se lee de `waitlist.oferta.minutos` con un **valor por defecto seguro y no productivo en `test`** para que CI sea determinista. El valor de negocio es **`(TBD) -> forma de obtención`**: medición del piloto y decisión del PO Parte 3 antes del despliegue. El reloj se inyecta (`Clock`) para que los tests no dependan de zona horaria local.
- **Justificación**: cumple RN-GL-03 (no inventar cifras) y la restricción de tests deterministas; el default de test es un valor de prueba, no una regla comercial.

### W-7 — Adjudicación atómica y doble adjudicación

- **Decisión**: la adjudicación ocurre en **una sola transacción**: se valida hueco libre con `DisponibilidadService` y las exclusion constraints de `V006`, se registra la clave en `public_idempotencia` (V004) con operación **`waitlist.adjudicar`** y hash SHA-256 del `turnoId` (un turno liberado se adjudica una sola vez), se crea la `Ocupacion` por el camino de servicio existente y se emite el evento de auditoría. Ante reenvío o doble candidato, `409` sin segundo efecto. Queda prohibido insertar directamente en `turnos`, `bloqueos` u `ocupaciones` (C-09 D14).
- **Justificación**: reúne RN-AG-07 y RN-GL-02 con el mismo patrón atómico de C-15/C-16, sin constraint nueva en V006.

### W-8 — Conciliación del depósito contra C-15

- **Decisión**: se reutiliza `SeniaService.conciliar` (flag `conciliado` de V021) y, si corresponde, `SeniaService.reembolsar` con **reembolso total y liberación inmediata** (C-01 A4). **No se crea caja, asiento, arqueo ni liquidación contable** (MP-3, AR-3). El depósito no se factura en C-19.
- **Justificación**: cierra la cadena C-15 → C-19 sin duplicar reglas de dinero ni reabrir la conciliación ya aprobada.

### W-9 — Definición de métricas verificables

- **Decisión**: se fijan fórmula, fuente y período, y se resuelven las dos ambigüedades de C-01 E:
  - **Ocupación** = `100 × minutos ocupados / minutos disponibles` por sillón y período; **los bloqueos SÍ cuentan como agenda ocupada** (resuelve la ambigüedad de C-01). Fuente: `turnos` activos + `bloqueos` activos + catálogo de C-07.
  - **Ausentismo** = `100 × turnos con inasistencia / turnos confirmados` en el período; **los cancelados (dentro o fuera de ventana) NO entran en el numerador**. La inasistencia se registra como evento `turno.inasistencia` (W-10), no se infiere de `estado=cancelado`.
  - **Período**: mensual `YYYY-MM` en `America/Argentina/Buenos_Aires` (C-01 C2).
  - La fila `metricas_ocupacion` exige `metodologia` no vacía con fórmula y fuente, además de `sillon_id`, `periodo`, `ocupacion`, `ausentismo` y `calculada_en`.
  - **Meta inicial `(TBD) -> forma de obtención`**: baseline del piloto y aprobación del PO Parte 3; no se publica cifra sin metodología y período (DD-06, SU-04).
- **Justificación**: hace la métrica auditable y resuelve por decisión humana —no por convención— las ambigüedades que C-01 dejó explícitamente abiertas.

### W-10 — Catálogo de auditoría

- **Decisión**: ampliar `AuditoriaCatalogo.TIPOS` con **`waitlist.creado`**, **`waitlist.ofrecido`**, **`waitlist.adjudicado`**, **`waitlist.expirado`**, **`turno.inasistencia`** y **`metrica.publicada`**. Entidades: `lista_espera`, `turno` y `metrica`. Actores: `ActorSimulado.system()` (modo `SYSTEM`) para adjudicación/expiración/cálculo y `ActorSimulado.recepcionista()` (modo `SIMULADO`) para alta y registro de inasistencia. Metadata acotada a identificadores, `periodo`, `motivo` y `correlationId`; quedan prohibidas las claves de `AuditoriaMetadataValidator`.Esta acta cumple la Decisión 1 de C-05 (proponer eventos por escrito); cada escenario lleva su test.
- **Justificación**: trazabilidad de adjudicación y de las cifras publicadas, sin filtrar datos personales ni presentar la métrica como evidencia de cumplimiento.

### W-11 — Governance y checkpoints

- **Decisión**: mantener **MEDIO** según `CHANGES.md`, con dos checkpoints: (1) esquema V023 + waitlist + adjudicación atómica; (2) depósito/conciliación + métricas + auditoría. No se eleva a CRITICO porque C-19 **no implementa cobro nuevo** (reutiliza C-15), no crea secretos y no modifica `auth`/facturación. La aprobación de este acta cubre el inicio del propose; el apply respeta los checkpoints.
- **Justificación**: el riesgo de dinero ya fue aprobado en C-15; el alcance de C-19 es pipeline y medición,coherente con el nivel MEDIO del roadmap.

### W-12 — Superficie, wiring y configuración

- **Decisión**: paquete nuevo `com.turnosodontologia.waitlist`; controlador `@Profile({"dev","test"})` con rutas indicativas `/lista-espera` y `/metricas` (sin prefijo `/publico/v1`, sin token de C-06 y sin RBAC nuevo). Registrar el paquete en `TestJpaConfig` igual que `depositpayments`/`invoicing` (AR-13). Nuevas propiedades **`waitlist.oferta.minutos`** y **`waitlist.metricas.periodo`** con defaults seguros en `application-test.yml` y `@ConfigurationProperties`; sin secretos reales, sin dependencias nuevas. El rol `dirección` sigue diferido (C-03 D7). La ausencia de `@SpringBootApplication` en `src/main` sigue siendo limitación registrada, no bloqueante. **Sin UI** (`Decision: no (no UI)`).
- **Justificación**: reproduce el patrón de superficies ya verificado y garantiza que GitHub Actions no rompa por placeholders sin valor.

### W-13 — Límite respecto de C-09

- **Decisión**: **no reabrir C-09**. Se mantiene su D4 (rechazo `409` con hasta 3 alternativas y «sin waitlist» cuando el hueco se toma entre consulta y confirmación). C-19 solo opera como motor explícito sobre huecos **ya liberados** por cancelación, sin modificar endpoints, scopes ni token público de C-09. El cableado automático queda registrado en W-4.
- **Justificación**: preserva el contrato público archivado y evita deltas `MODIFIED` en specs de C-09.

## Pendientes explícitos (no resueltos aquí)

- Monto del depósito de waitlist y plazo de expiración productivo: `(TBD) -> forma de obtención` (W-5, W-6; PO Parte 3 + piloto).
- Metas numéricas de ocupación y ausentismo: `(TBD) -> forma de obtención` (W-9; baseline del piloto).
- Cableado automático cancelación → motor de relleno (W-4) y notificación WhatsApp real (W-3, C-10).
- Caja, asiento, arqueo, liquidación y facturación del depósito (W-8, MP-3, AR-3).
- Protección RBAC de la agenda y adaptador `AuthenticatedActor` → `CurrentActorProvider` (deuda de C-05).
- Aplicación ejecutable en `src/main` y gestor de secretos (C-17/C-18).

## Regla de aplicación

Estas decisiones habilitan únicamente el `/opsx:propose` de C-19 con la migración **`V023__waitlist_and_metrics.sql`**. No autorizan escribir en `src/`, crear la migración durante el propose, commits ni push. Antes del apply se debe verificar que proposal, design, tasks y deltas reflejen W-1..W-13, los `(TBD) -> forma de obtención`, la exclusión de C-09/C-14/C-15/C-16 y los dos checkpoints de W-11.
