# C-15 — Decisiones del PO para `mercadopago-deposits-reconciliation` (Parte 3)

- **Change**: `C-15 mercadopago-deposits-reconciliation`
- **Governance local**: **CRITICO** — cobros y conciliación; solo análisis y aprobación humana explícita antes de codear
- **Decisor**: Lautaro Ellerbach, Product Owner del equipo de Parte 3
- **Fecha**: 2026-11-23
- **Estado**: aprobado — opciones recomendadas más simples, seguras y alineadas con C-01..C-14
- **Alcance de este documento**: cerrar los 12 puntos abiertos del explore (`MP-1`..`MP-12`) para habilitar `/opsx:propose`. No implementa código, no crea migraciones, no modifica `src/` y no autoriza commits.
- **Fuentes**: `CHANGES.md` §C-15 y §«Reservas de numeración de migraciones Flyway», `docs/decisiones/C-01-decisiones-habilitantes.md` (A1, A4, A5), `C-02-resoluciones-stack.md`, `C-05-ajustes-pre-apply-PO.md`, `C-06-decisiones-public-access-tokens.md`, `C-09-decisiones.md`, `C-14-decisiones.md`, `knowledge-base/04,05,06,07,08,09,10`, `openspec/specs/budgets/spec.md` y relevamiento de `src/`.

> Estas decisiones son la respuesta exigida por RN-GL-03: ningún `(TBD)` se resuelve por convención del agente. Lo que aquí no se decide sigue `(TBD)` y se registra como pendiente explícito, no como valor inventado. Las decisiones ya tomadas en C-01 (A4, A5), C-09 y C-14 no se reabren.

## Decisiones aprobadas

### MP-1 — Verificación de firma del webhook

- **Decisión**: verificación **local HMAC-SHA256** del header `x-signature` (componentes `ts` y `v1`, con `x-request-id` como parte del mensaje) usando `MERCADOPAGO_WEBHOOK_SECRET`. Firma ausente, malformada o inválida → `400` sin efectos, sin persistencia y sin evento de auditoría; el secreto nunca se registra ni se loguea. **No** se reconsulta el pago a la API de Mercado Pago como segunda validación.
- **Alternativa descartada**: reconsultar `GET /v1/payments/{id}` por cada webhook — agrega una dependencia de red al camino crítico y una superficie de reintentos que hoy no está decidida; queda como mejora futura.
- **Justificación**: replica el patrón HMAC ya aprobado y probado en C-06, usa `javax.crypto` (sin dependencias nuevas) y satisface la exigencia de `knowledge-base/08_arquitectura_propuesta.md` §Webhooks.

### MP-2 — Superficie e integración REST

- **Decisión**: integración por **REST con `RestTemplate`** de `spring-boot-starter-web` (ya presente en `pom.xml`) detrás de un puerto de aplicación, **sin SDK ni dependencias nuevas**. Creación de la solicitud de seña mediante `POST /v1/preferences` con `external_reference = "senia-" + idempotencyKey`, donde `idempotencyKey` es un UUID generado por el sistema y enviado también como `X-Idempotency-Key`. La URL base se toma de `APP_BASE_URL`. Webhook en `POST /publico/v1/webhooks/mercadopago`, **sin token público de C-06**: se autentica solo por la firma de MP-1. Controlador `@Profile({"dev","test"})`, igual que C-14, sin RBAC nuevo.
- **Alternativa descartada**: instalar el SDK de Mercado Pago o crear un scope financiero nuevo en C-06 — requiere aprobación e instalación explícitas y ampliaría la superficie pública sin necesidad del flujo.
- **Justificación**: reutiliza las dependencias y el patrón de perfiles ya vigentes; `external_reference` derivado de un UUID interno permite resolver la seña sin analizar compuestos ni depender de secuencias de base de datos, y mantiene la idempotencia de RN-GL-02 desde el primer momento.

### MP-3 — Conciliación, caja y arqueo

- **Decisión**: la **conciliación se reduce a un estado verificable**: columna `conciliado` en la seña, establecida cuando un pago `aprobado` se resuelve contra un `turno_id` o `presupuesto_id` existente, más su evento de auditoría. **No se crea entidad `Caja`, `Asiento` ni tabla de arqueo** en V021. El asiento de caja y el arqueo quedan como **pendiente explícito** para un change posterior con decisión del PO.
- **Alternativa descartada**: inventar semántica contable en este change — la KB no define `Caja`/`Asiento`, y C-14 D10/D15 prohíbe crearla sin base aprobada.
- **Justificación**: es la opción mínima verificable y auditable; satisface RN-FI-03 sin fijar reglas contables no acordadas. Se registra que C-14 D10 esperaba caja/arqueo en C-15 y que esta decisión PO los difiere expresamente por ausencia de definición en la KB.

### MP-4 — Estados del pago y mapping

- **Decisión**: `estado` como `String` (patrón C-08/C-09/C-14, sin enum SQL) con valores `pendiente`, `aprobado`, `rechazado`, `cancelado`, `reembolsado`. Mapping: `pending`→`pendiente`, `approved`→`aprobado`, `rejected`→`rechazado`, `cancelled`→`cancelado`, `refunded`/`charged_back`→`reembolsado`. Terminales: `aprobado`, `rechazado`, `cancelado`, `reembolsado`; solo se permiten transiciones desde `pendiente`, y un evento tardío sobre un estado terminal se descarta sin efectos. `fecha_aprobacion` = instante en que se observó el webhook aprobado, no un valor parseado del payload de MP. `moneda` fija `ARS` y `monto NUMERIC(12,2)` con `HALF_UP`, coherente con C-14 D5.
- **Alternativa descartada**: enum SQL o refrescar el estado desde la API de MP en cada consulta — el primero rompe el patrón de esquema vigente y el segundo contradice MP-1.
- **Justificación**: cubre el ciclo del Flujo 5 con valores trazables, evita regresiones de estado y conserva el formato monetario ya aprobado.

### MP-5 — Operativa del reembolso

- **Decisión**: política **A4 sin cambios**: reembolso **total** y liberación **inmediata** del hueco, sin retención. C-15 expone `POST /senias/{id}/reembolsar` para recepción; el importe es el total de la seña y la operación se ejecuta contra la API de MP solo si el pago está `aprobado`, registrando el resultado y su auditoría. La liberación del hueco la sigue haciendo C-09; **no se modifica C-09**. Ante un pago `rechazado`, la seña queda `rechazado`, el turno permanece `confirmado` y se avisa a recepción por el puerto de MP-9. El disparo automático "cancelación pública → reembolso" sin intervención de recepción queda como pendiente explícito.
- **Alternativa descartada**: retención parcial u ocultamiento del hueco — descartada por C-01 A4 y por romper el *write-back* real de DD-03.
- **Justificación**: cumple la política aprobada sin reabrir el flujo público de C-09 ni inventar un bus de eventos a partir del log de auditoría.

### MP-6 — Catálogo de eventos de auditoría

- **Decisión**: ampliar el catálogo con **`pago.solicitado`**, **`pago.aprobado`**, **`pago.rechazado`**, **`pago.reembolsado`** y **`pago.conciliado`**. Entidad `senia`, `entidadId = seña.id`. Actores: `ActorSimulado.system()` (modo `SYSTEM`) para eventos originados en el webhook y `ActorSimulado.recepcionista()` (modo `SIMULADO`) para la solicitud y el reembolso. Metadata acotada a `seniaId`, `turnoId` y `presupuestoId` cuando existan, `monto` como texto, `moneda`, `estado`, `externalReference`, `motivo` y `correlationId` nuevo por operación. Quedan prohibidas las claves de `AuditoriaMetadataValidator` (`token`, `email`, `dni`, `telefono`, etc.).
- **Alternativa descartada**: agregar eventos durante el apply — está prohibido por la Decisión 1 de C-05; esta acta es la decisión posterior del PO que habilita la ampliación.
- **Justificación**: cubre cada movimiento de dinero y la conciliación con trazabilidad, sin filtrar datos personales ni secretos, y respeta la regla de proponer los eventos por escrito con un test por escenario.

### MP-7 — Idempotencia del webhook

- **Decisión**: reutilizar **`public_idempotencia` (V004)** con operación `pago.webhook` y clave = hash SHA-256 de `x-request-id` (o del par `external_reference` + `ts` si aquel falta). Un reenvío con la misma clave responde **`200` sin segundo efecto**, nunca `409`, porque MP reintenta hasta recibir 2xx. La clave se registra **dentro de la misma transacción** que el cambio de estado de la seña. `idempotency_key` de la entidad se usa para la creación de la preferencia (MP-2) y es independiente de la clave del webhook.
- **Alternativa descartada**: `409` ante duplicado — provocaría reintentos indefinidos del proveedor; y una tabla nueva de idempotencia — duplicaría V004 sin beneficio.
- **Justificación**: mantiene RN-GL-02 y el patrón de C-09 D7, con atomicidad entre dedupe y cambio de estado.

### MP-8 — Costos de API (enmienda de C-01 A5)

- **Decisión**: se aplica **C-01 A5**: precio plano todo incluido, **sin desglose de costos de API** para el consultorio; RN-SE-06 quedó enmendada en esa fecha. Los artefactos del change **no reproducen** la redacción antigua de `CHANGES.md` C-15/C-21 sobre desglose de costos y declaran expresamente la supersesión.
- **Alternativa descartada**: mantener ambas redacciones — generaría un requisito contradictorio con RN-SE-06 y con el ajuste ya aprobado de C-21.
- **Justificación**: respeta la decisión humana del PO y evita presentar como pendiente lo que ya fue resuelto en la fase habilitante.

### MP-9 — Notificación al paciente

- **Decisión**: C-15 emite una solicitud de notificación a través de un **puerto con implementación NO-OP/simulada en `dev`/`test`**, igual que `NotificacionPublicaPort` en C-09 D8. No se envía WhatsApp real, no se modifica C-10 y no se afirma entrega.
- **Alternativa descartada**: integrar WhatsApp en C-15 — C-10 es un change separado y no es dependencia de este.
- **Justificación**: deja el acoplamiento detrás de un puerto, sin adelantar el outbox de C-10 ni inventar eventos de notificación nuevos.

### MP-10 — Secretos y dependencias

- **Decisión**: se usan **solo los nombres ya presentes en `.env.example`**: `MERCADOPAGO_ACCESS_TOKEN`, `MERCADOPAGO_WEBHOOK_SECRET` y `APP_BASE_URL`. Sin valores reales, sin gestor de secretos nuevo (sigue `(TBD)` desde C-02) y sin dependencias ni servicios externos nuevos.
- **Alternativa descartada**: agregar variables o instalar el SDK — excede la aprobación del PO y la restricción presupuestaria.
- **Justificación**: preserva la regla de "sin secretos en repositorio" y la restricción de esta fundación.

### MP-11 — Verificación

- **Decisión**: patrón de tests con **Testcontainers/PostgreSQL** (`PostgresFlywayTest`) cubriendo: webhook duplicado, pago rechazado, conciliación, reembolso y auditoría; más tests unitarios de firma HMAC, mapping de estados y transiciones inválidas. Se parte de la línea base de 241 tests verdes de C-14 y se cierra con `mvn test` y `openspec validate mercadopago-deposits-reconciliation --strict`. La ausencia de `@SpringBootApplication` en `src/main` sigue siendo limitación registrada en C-09 D13, no bloqueante.
- **Alternativa descartada**: verificar solo con H2 — no cubre la atomicidad de `public_idempotencia` ni la unicidad concurrente exigida por RN-GL-02.
- **Justificación**: da evidencia verificable para cada escenario de error del Flujo 5 y para la regla de C-05 de un test por escenario de auditoría.

### MP-12 — Deuda de `AuthenticatedActor`

- **Decisión**: la deuda del adaptador `AuthenticatedActor` → `CurrentActorProvider` y la protección RBAC de la agenda se mantienen **fuera de alcance**, igual que en C-14 D1. C-15 no modifica C-04, C-08 ni la cadena `/auth/**`.
- **Alternativa descartada**: resolverla aquí — es ortogonal al dominio financiero y elevaría un change CRITICO a cambio de seguridad sin decisión específica.
- **Justificación**: evita reabrir changes archivados y preserva las guardias vigentes.

## Pendientes explícitos (no resueltos aquí)

- Asiento de caja y arqueo (MP-3): requiere definición de entidad y semántica contable en un change posterior con decisión del PO.
- Disparo automático cancelación → reembolso sin intervención de recepción (MP-5).
- Reconsulta del pago a la API de MP como segunda validación de firma (MP-1).
- Gestor de secretos (MP-10) y aplicación ejecutable en `src/main` (MP-11): competen a C-17/C-18.
- Protección RBAC de la agenda y adaptador de actor real (MP-12).
- Facturación ARCA y comprobantes: C-16, según C-01 A1.

## Regla de aplicación

Estas decisiones habilitan únicamente el `/opsx:propose` de C-15 con la migración **`V021__mercadopago_deposits.sql`**. No autorizan escribir en `src/`, crear migraciones durante el propose, commits ni push. Antes del apply se debe verificar que proposal, design, tasks y deltas reflejen MP-1..MP-12, la enmienda de MP-8 y la exclusión de caja/arqueo de MP-3.
