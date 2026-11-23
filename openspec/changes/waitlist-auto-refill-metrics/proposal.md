# Proposal

## Why

C-01 «Gap C-19» había diferido la waitlist con relleno automático y depósito a v0.2 y cerrado C-19 sin código. El PO de la Parte 3 supersedió esa decisión el 2026-11-23 (`docs/decisiones/C-19-decisiones.md`, W-1) y habilitó el change en v0.1 con el alcance mínimo de W-1..W-13: relleno explícito sobre huecos ya liberados, depósito reutilizando el ciclo de seña de C-15 y métricas verificables de ocupación y ausentismo, sin reabrir C-09, C-14, C-15 ni C-16.

## What Changes

- Migración **`V023__waitlist_and_metrics.sql`** de la Parte 3: tablas `lista_espera` y `metricas_ocupacion` con índices y checks. `V001`–`V022` quedan intactas; no se alteran `senias`, `turnos`, `ocupaciones` ni `public_idempotencia`. El número histórico `013` del scope queda supersedido por la tabla de reservas.
- Motor de relleno como **caso de uso explícito** `RellenoWaitlistService` sobre turnos ya cancelados con ocupación desactivada; no se agrega `ApplicationEventListener`, no se usa el log de auditoría como bus y no se modifica C-09. El cableado automático cancelación → relleno queda como pendiente explícito (W-4).
- Adjudicación **atómica** de un solo candidato por hueco: validación con `DisponibilidadService` y las exclusion constraints de V006, dedupe en `public_idempotencia` (V004) con operación `waitlist.adjudicar`, creación de la ocupación por el camino de servicio existente y evento de auditoría en la misma transacción. Reenvío o doble candidato se rechaza sin segundo efecto (W-7).
- **Depósito reutilizando C-15**: la entrada conserva `deposito_requerido` y `senia_id` opcional sin FK física; el cobro invoca `SeniaService.solicitar` tal cual, sin tabla de pagos ni proveedor nuevo. Conciliación por flag `conciliado` y reembolso total con liberación inmediata (C-01 A4). Sin caja, asiento, arqueo ni liquidación contable (MP-3, AR-3). Monto productivo `(TBD) -> forma de obtención` (W-5).
- **Oferta simulada**: puerto `NotificacionWaitlistPort` con adaptador NO-OP/simulado en `dev`/`test`, sin WhatsApp real, outbox ni plantillas (C-10 sigue pendiente). Plazo de expiración configurable con `Clock` inyectado; valor productivo `(TBD) -> forma de obtención` (W-3, W-6).
- **Métricas verificables**: ocupación = `100 × minutos ocupados / minutos disponibles` por sillón, con bloqueos contando como agenda ocupada; ausentismo = `100 × turnos con inasistencia / turnos confirmados`, sin cancelados en el numerador. Período mensual `YYYY-MM` en `America/Argentina/Buenos_Aires` y `metodologia` obligatoria con fórmula y fuente. Metas iniciales `(TBD) -> forma de obtención` (W-9).
- **Seis eventos de auditoría** aprobados por acta PO: `waitlist.creado`, `waitlist.ofrecido`, `waitlist.adjudicado`, `waitlist.expirado`, `turno.inasistencia` y `metrica.publicada`, con metadata acotada y un test por escenario (W-10, Decisión 1 de C-05).
- **Superficie HTTP indicativa** `/lista-espera` y `/metricas` bajo `@Profile({"dev","test"})`, sin prefijo `/publico/v1`, sin token de C-06 y sin RBAC nuevo. Paquete `com.turnosodontologia.waitlist` registrado en `TestJpaConfig` (W-12). Sin UI.
- **Configuración segura**: propiedades `waitlist.oferta.minutos` y `waitlist.metricas.periodo` con defaults no productivos en el perfil `test`, reloj inyectado y tests deterministas sin zona horaria local ni secretos, desde la línea base de 320 tests verdes (W-6, W-12).
- Gobernanza **MEDIO** con dos checkpoints: (1) esquema V023 + waitlist + adjudicación atómica; (2) depósito/conciliación + métricas + auditoría (W-11).

## Capabilities

### New Capabilities

- `waitlist-auto-refill`: ciclo observable de la lista de espera — alta de candidatos, ofrecimiento simulado, expiración de la oferta, adjudicación atómica del hueco liberado y superficie indicativa de prueba.
- `occupancy-metrics`: definición y cálculo auditable de la ocupación y el ausentismo por sillón y período, con metodología explícita y publicación condicionada a metodología y período.

### Modified Capabilities

- `agenda`: incorpora la reocupación de un hueco liberado mediante el motor de waitlist, con dedupe y sin inserciones directas.
- `deposit-payments`: incorpora el depósito de waitlist sobre el ciclo de seña existente, sin tabla de pago ni contabilidad nueva.
- `audit-event-emission`: incorpora los seis eventos de waitlist, inasistencia y métrica y su metadata sanitizada.
- `audit-append-only-log`: amplía el catálogo cerrado con los seis tipos aprobados para W-10, conservando una fila por operación confirmada.

## Impact

- **Código**: paquete nuevo `com.turnosodontologia.waitlist`; reutilización de `Ocupacion`/`Turno`, `DisponibilidadService`, `SeniaService`, `public_idempotencia` (V004), `AuditoriaCatalogo`, `AuditoriaMetadataValidator` y los puertos simulados de notificación. Sin cambios en `src/` durante el propose.
- **API**: dos rutas `/lista-espera` y `/metricas` bajo `@Profile({"dev","test"})`; sin endpoints de caja, asiento, auditoría ni consulta paciente; sin permisos RBAC nuevos.
- **Esquema**: solo V023. V001–V022 quedan intactas; la numeración `013` histórica no se usa.
- **Configuración**: dos propiedades no secretas con defaults de prueba; sin secretos reales, dependencias nuevas ni gestor de secretos.
- **Dependencias**: C-01, C-09 y C-15 completadas. C-11 (WhatsApp) sigue pendiente y **no** se reabre. No modifica C-09, C-14, C-15 ni C-16.
- **Gobernanza**: MEDIO con checkpoints en W-11; el acta PO ya habilitó el propose, el apply respeta la verificación por checkpoint.

## Out of scope

Reabrir C-09/C-14/C-15/C-16; cableado automático cancelación → relleno; WhatsApp real, outbox y plantillas; caja, asiento, arqueo, liquidación y facturación del depósito; números productivos de depósito, plazo y metas; rol `dirección`; UIC/UI; adaptador `AuthenticatedActor` → `CurrentActorProvider` y RBAC de agenda; `@SpringBootApplication` y gestor de secretos (C-17/C-18); owner legal y dossier (C-22).
