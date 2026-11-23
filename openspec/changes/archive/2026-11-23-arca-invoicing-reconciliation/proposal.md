# Proposal

## Why

C-15 cerró el ciclo de la seña con Mercado Pago, pero el consultorio aún no puede emitir el comprobante ARCA por el dinero efectivamente cobrado ni dejar evidencia de que importe, seña y comprobante concuerdan. C-01 A1 resolvió IN-01/DD-09 a favor de **ARCA completo en MVP** y el PO aprobó el 2026-11-23 las decisiones `AR-1`..`AR-14` de `docs/decisiones/C-16-decisiones.md`: este change cierra la cadena presupuesto → seña → factura. El apply sigue sujeto a la gobernanza **CRITICO**: requiere aprobación humana explícita adicional antes de escribir código.

## What Changes

- Migración **`V022__arca_invoicing.sql`** de la Parte 3: tabla nueva `comprobantes` con `senia_id` único, `presupuesto_id`/`turno_id` opcionales sin FK física, `tipo`, `estado`, `moneda=ARS`, `monto NUMERIC(12,2)`, `concepto`, `cae`, `fecha_emision`, `intentos`, `ultimo_error` saneado y `conciliado`. No se alteran V001–V021, `presupuestos` ni `senias`; no hay tablas de renglones, impuestos, caja, asiento ni arqueo. El número histórico `011` del scope queda supersedido por la tabla de reservas.
- Emisión de un comprobante **por seña aprobada** y por el importe cobrado (`senia.monto`): tipo `factura`, letra `C` configurable, receptor consumidor final e IVA no discriminado, coherente con C-14 D5 y la enmienda C-01 A5 (sin desglose de impuestos ni costos de API).
- Integración REST detrás de un puerto, con `RestTemplate` ya presente y **sin SDK ni dependencias nuevas**. Adaptador NO-OP/simulado en `dev`/`test` con CAE determinista de prueba; el adaptador real no se invoca en tests. Variables nuevas `ARCA_BASE_URL` y `ARCA_ACCESS_TOKEN` solo como nombres en `.env.example`, con defaults seguros y no productivos en el perfil `test` para no romper GitHub Actions.
- Estados `pendiente`/`emitido`/`rechazado`/`anulado` y tipos `factura`/`nota_credito` con transiciones acotadas. Idempotencia reutilizando `public_idempotencia` (V004) con operación `factura.emitir` y clave derivada de la seña, registrada en la misma transacción; sin outbox, async ni `REQUIRES_NEW`. Reintento manual explícito desde `rechazado`, con `intentos` y `ultimo_error` sin secretos.
- Conciliación verificable: `conciliado=true` solo con CAE, importe igual al de la seña y seña `aprobado` **y** `conciliado` según C-15; en caso contrario queda `false` y no se emite evento de conciliación. No se crean turnos, presupuestos ni asientos.
- Nota de crédito **manual de recepción** sobre factura emitida, válida solo si la seña está `reembolsado` y por el total. C-15 no se reabre: el disparo automático reembolso → nota de crédito queda como pendiente explícito.
- Cinco tipos de auditoría aprobados por acta PO (`factura.emitida`, `factura.rechazada`, `factura.conciliada`, `factura.anulada`, `nota_credito.emitida`), con metadata sanitizada: sin CAE, DNI, dirección, credenciales ni secretos.
- Superficie `POST /comprobantes`, `POST /comprobantes/{id}/reintentar`, `POST /comprobantes/{id}/nota-credito` y `GET /comprobantes/{id}`, solo en perfiles `dev`/`test`, sin credenciales de staff ni RBAC nuevo; sin tocar `/auth/**`.
- Verificación con Testcontainers/PostgreSQL: `Migration022Test` con V001–V021 intactas, idempotencia, conciliación, reintento, nota de crédito, auditoría y secretos fuera de logs/metadata; línea base de 287 tests verdes y `openspec validate arca-invoicing-reconciliation --strict`.

## Capabilities

### New Capabilities

- `arca-invoicing`: ciclo de vida observable del comprobante — emisión sobre seña aprobada, estados y transiciones, idempotencia, reintentos, nota de crédito y superficie aislada de prueba.
- `arca-reconciliation`: criterio y evidencia de la conciliación comprobante–seña–presupuesto, sin contabilidad de caja ni modificación del change archivado de pagos.

### Modified Capabilities

- `audit-event-emission`: incorpora los cinco eventos de facturación y su metadata sanitizada.
- `audit-append-only-log`: amplía el catálogo cerrado con los cinco tipos aprobados, conservando una fila por operación confirmada.

## Impact

- **Código**: paquete nuevo `com.turnosodontologia.invoicing` sobre la arquitectura DDD de C-02; reutilización de `public_idempotencia` (V004), `AuditoriaCatalogo`, `AuditoriaMetadataValidator`, `AuditableEntity`, `Senia` (V021), `Presupuesto` (V020) y `RestTemplate`. Sin cambios en `src/` durante el propose.
- **API**: cuatro rutas `/comprobantes` bajo `@Profile({"dev","test"})`; sin endpoints de caja, asiento, auditoría ni consulta paciente; sin permisos RBAC nuevos.
- **Esquema**: solo V022. V020 y V021 quedan intactas; la numeración `011` histórica no se usa.
- **Configuración**: nombres nuevos `ARCA_BASE_URL` y `ARCA_ACCESS_TOKEN` en `.env.example` y parámetros no secretos de letra/punto de venta; defaults de prueba en el perfil `test`, sin valores reales ni gestor de secretos.
- **Dependencias**: C-01, C-15 y sus predecesoras completadas. No modifica C-14, C-15, C-09 ni la cadena `/auth/**`.
- **Gobernanza**: CRITICO — el propose es análisis aprobado; el apply requiere confirmación humana explícita posterior.

## Out of scope

Caja, asiento, arqueo y liquidación contable; impresión/PDF y lectura por paciente; adaptador `AuthenticatedActor` → `CurrentActorProvider` y RBAC de agenda; `@SpringBootApplication` y gestor de secretos (C-17/C-18); owner legal, dossier y métricas (C-18/C-22); facturación del saldo del presupuesto, disparo automático de nota de crédito, reconsulta a ARCA y notificaciones reales.
