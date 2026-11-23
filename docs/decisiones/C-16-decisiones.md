# C-16 — Decisiones del PO para `arca-invoicing-reconciliation` (Parte 3)

- **Change**: `C-16 arca-invoicing-reconciliation`
- **Governance local**: **CRITICO** — facturación; análisis y aprobación humana explícita antes de codear
- **Decisor**: Lautaro Ellerbach, Product Owner del equipo de Parte 3
- **Fecha**: 2026-11-23
- **Estado**: aprobado — opciones recomendadas más simples, seguras y alineadas con C-01..C-15 para AR-1..AR-14
- **Alcance de este documento**: cerrar los catorce puntos abiertos del explore para habilitar `/opsx:propose`. No implementa código, no crea migraciones, no modifica `src/` y no autoriza commits.
- **Fuentes**: `CHANGES.md` §C-16 y §«Reservas de numeración de migraciones Flyway», `docs/decisiones/C-01-decisiones-habilitantes.md` (A1, A4, A5), `C-02-resoluciones-stack.md`, `C-03-decisiones-core-models.md`, `C-05-ajustes-pre-apply-PO.md`, `C-09-decisiones.md`, `C-14-decisiones.md` (D5, D11, D13, D14), `C-15-decisiones.md` (MP-1..MP-12), `knowledge-base/01,02,05,09,10` y relevamiento de `src/`.

> Estas decisiones son la respuesta exigida por RN-GL-03: ningún `(TBD)` se resuelve por convención del agente. Las decisiones ya tomadas en C-01, C-14 y C-15 no se reabren. El texto residual de `CHANGES.md` C-16 (`(TBD, IN-01/DD-09)`, «Migración 011») queda supersedido por C-01 A1 y por la tabla de reservas: este change usa **V022**.

## Decisiones aprobadas

### AR-1 — Disparador y alcance de la emisión

- **Decisión**: emitir un comprobante **por cada seña aprobada**, por el importe efectivamente cobrado (`senia.monto`), con referencia obligatoria a `senia_id` y opcional a `presupuesto_id`/`turno_id` (tomados de la seña). Si la seña tiene presupuesto, el `concepto` del comprobante se describe desde ese presupuesto; si no lo tiene, se usa un concepto genérico de seña odontológica.
- **Justificación**: no se factura dinero no cobrado; es el cierre natural de la cadena C-14 → C-15 → C-16 y no obliga a reabrir C-14 ni C-15. La facturación del saldo del presupuesto queda como pendiente explícito porque el MVP solo cobra la seña.

### AR-2 — Modelo de datos de V022

- **Decisión**: crear una tabla nueva **`comprobantes`** con `senia_id` (FK a `senias`, único), `presupuesto_id` y `turno_id` opcionales **sin FK física** (mismo patrón que V021), `tipo`, `estado`, `moneda` fija `ARS`, `monto NUMERIC(12,2)`, `concepto`, `cae`, `fecha_emision`, `intentos`, `ultimo_error` (texto saneado, sin secretos) y `conciliado`, más `created_at/created_by/updated_at/updated_by/activo` y checks/índices. **No** se altera `presupuestos` ni `senias`; no se crea tabla de renglones ni de impuestos.
- **Justificación**: conserva el contrato estable de C-14 D11 sin ensuciarlo con datos fiscales, mantiene una fila por comprobante y evita un modelo contable que la KB no define. FKs opcionales a turno/presupuesto se omiten igual que en C-15 para no acoplar changes archivados.

### AR-3 — «Liquidación»

- **Decisión**: el alcance de C-16 es **emisión, reintentos, conciliación y nota de crédito**. La «liquidación» contable (cierre, asiento, arqueo, libro IVA) queda **diferida** como en C-15 MP-3.
- **Justificación**: no existe definición de `Caja`/`Asiento` en la KB y C-15 ya difirió esa semántica; inventarla elevaría el riesgo de un change CRITICO sin base aprobada.

### AR-4 — Régimen y tipo de comprobante

- **Decisión**: comprobante de receptor **consumidor final**, tipo `factura`, letra **`C`** (sin discriminación de IVA). Letra y punto de venta se leen de configuración, sin valores reales en el repositorio.
- **Justificación**: es la opción mínima para un consultorio único con precio todo incluido (C-01 A5, C-14 D5); evita calcular una alícuota no acordada y reduce los datos fiscales exigidos al paciente. Si el asesoramiento contable exigiera otra letra en el apply, se cambia solo el parámetro, sin tocar esquema ni specs.

### AR-5 — Tratamiento del IVA

- **Decisión**: **IVA no discriminado**. `monto` del comprobante = `senia.monto` en `ARS`; no se crean campos de impuesto, subtotal ni alícuota, y no se desglosan costos de API ni impuestos en la superficie del consultorio.
- **Justificación**: coherente con C-14 D5 («impuestos incluidos en el precio») y con la enmienda C-01 A5 confirmada en MP-8.

### AR-6 — Datos fiscales del paciente

- **Decisión**: **no crear tabla ni campos fiscales nuevos** ni recolectar datos en superficie pública. Se usan los ya existentes en `pacientes` (`nombre`, `apellido`, `documento_tipo`, `documento_numero`, `direccion` opcional). Si ARCA exige un dato no disponible, el comprobante queda `pendiente` y se solicita aviso a recepción por un puerto NO-OP/simulado. La auditoría no incluye DNI, documento, dirección ni datos de receptor.
- **Justificación**: minimiza el tratamiento de datos personales (Ley 25.326) y respeta `AuditoriaMetadataValidator`, que prohíbe `dni`, `documento_numero` y `direccion` en metadata.

### AR-7 — Modalidad de integración y configuración

- **Decisión**: integración **REST con `RestTemplate`** detrás de un puerto de aplicación, **sin SDK ni dependencias nuevas**. Variables nuevas solo como nombres: `ARCA_BASE_URL`, `ARCA_ACCESS_TOKEN` (y parámetros no secretos `arca.comprobante.letra` / `arca.comprobante.punto-venta`). En el perfil `test` se definen **valores por defecto seguros y no productivos** (`http://localhost` y un token marcado claramente como de prueba) para no romper GitHub Actions; en `.env.example` solo los nombres, nunca valores reales.
- **Justificación**: replica MP-2/MP-10; un SDK o un proveedor externo de facturación sería un servicio nuevo que requiere aprobación explícita y presupuesto adicional.

### AR-8 — Simulación en dev/test

- **Decisión**: puerto `FacturacionPort` con adaptador **NO-OP/simulado** en `dev`/`test` que devuelve un CAE determinista de prueba; el adaptador REST real queda detrás del mismo puerto y no se invoca en tests.
- **Justificación**: igual patrón que `MercadoPagoPort`/`NotificacionPublicaPort`; permite verificar el flujo sin red, sin secretos y sin afirmar emisión real.

### AR-9 — Estados, idempotencia y reintentos

- **Decisión**: `estado` como `String` con `pendiente`, `emitido`, `rechazado`, `anulado`; `tipo` con `factura` y `nota_credito`. Transiciones: `pendiente → emitido | rechazado`; `rechazado → pendiente` solo por reintento manual; `emitido → anulado` solo por nota de crédito. Idempotencia reutilizando `public_idempotencia` (V004) con operación **`factura.emitir`** y clave SHA-256 de `senia.id`, registrada en la misma transacción que el cambio de estado. No hay outbox ni `REQUIRES_NEW`: el reintento es una operación explícita de recepción con la misma clave; `intentos` y `ultimo_error` quedan registrados sin secretos.
- **Justificación**: cumple RN-GL-02 y la Decisión 3 de C-05 (misma transacción), y satisface el requisito de «reintentos» del scope sin inventar un scheduler ni un bus de eventos.

### AR-10 — Conciliación

- **Decisión**: `conciliado` se fija en `true` solo cuando el comprobante está `emitido` con CAE, su `monto` coincide con el de la seña y la seña está `aprobado` **y** `conciliado` según C-15. Un comprobante emitido sobre una seña aún no conciliada queda `conciliado=false` y no emite `factura.conciliada`. No se crean turnos, presupuestos ni asientos.
- **Justificación**: encadena el flag verificable de C-15 con el comprobante ARCA sin duplicar reglas de referencia ni reabrir la conciliación de MP.

### AR-11 — Reembolso y nota de crédito

- **Decisión**: la nota de crédito es una operación **manual de recepción** (`factura` emitida → `nota_credito` anulada) que valida `senia.estado = reembolsado` y reverta el total. C-16 **no modifica C-15** ni dispara automáticamente la nota desde el reembolso; ese disparo queda como pendiente explícito.
- **Justificación**: C-01 A4/MP-5 fijan el reembolso total, pero C-15 ya está archivado: reabrirlo para un bus de eventos elevaría el riesgo sin una decisión específica.

### AR-12 — Catálogo de auditoría

- **Decisión**: ampliar el catálogo con **`factura.emitida`**, **`factura.rechazada`**, **`factura.conciliada`**, **`factura.anulada`** y **`nota_credito.emitida`**. Entidad `comprobante`, `entidadId = comprobante.id`; actores `ActorSimulado.recepcionista()` para emisión, reintento, anulación y nota de crédito, y `ActorSimulado.system()` para `factura.conciliada`. Metadata acotada a `comprobanteId`, `seniaId`, `presupuestoId`/`turnoId` cuando existan, `monto` como texto, `moneda`, `tipo`, `estado` y `motivo`, con `correlationId` nuevo por operación. Sin CAE, DNI, dirección, credenciales ni secretos.
- **Justificación**: cumple la Decisión 1 de C-05 (acta escrita antes de implementar) y un test por escenario; evita filtrar datos personales y mantiene la granularidad de una fila por operación confirmada.

### AR-13 — Superficie y wiring

- **Decisión**: paquete nuevo `com.turnosodontologia.invoicing`; controlador `@Profile({"dev","test"})` con `POST /comprobantes`, `POST /comprobantes/{id}/reintentar`, `POST /comprobantes/{id}/nota-credito` y `GET /comprobantes/{id}`, sin credenciales de staff ni RBAC nuevo. El apply debe registrar el paquete en `TestJpaConfig` igual que `depositpayments`. No se toca la cadena `/auth/**`.
- **Justificación**: reproduce el patrón de superficies financieras de C-14/C-15 y mantiene las guardias de C-04; es la condición técnica para ejecutar los tests de integración.

### AR-14 — Definición de done

- **Decisión**: se confirma la definición de done del explore: `Migration022Test` sobre `PostgresFlywayTest` dejando V001–V021 intactas; tests de idempotencia, conciliación, reembolso/nota de crédito y auditoría; secretos y credenciales fuera de logs y metadata; `openspec validate arca-invoicing-reconciliation --strict` sin errores; todo desde la línea base de **287 tests verdes** de C-15. Además, todo placeholder o variable nueva debe tener valor por defecto seguro en el perfil `test` para no romper GitHub Actions.
- **Justificación**: hace verificable cada escenario del Flujo 5 y la continuidad de la línea base, sin depender de servicios externos en CI.

## Fuera de alcance / diferidos explícitamente

- Caja, asiento y arqueo; liquidación contable/libro IVA (AR-3, MP-3).
- Impresión/PDF de comprobantes y lectura por paciente (C-14 D13/D14).
- Adaptador `AuthenticatedActor` → `CurrentActorProvider` y protección RBAC de la agenda (deuda de C-05; C-14 D1, MP-12).
- `@SpringBootApplication` en `src/main` y gestor de secretos: competen a C-17/C-18 (C-09 D13, MP-10/MP-11).
- Owner legal, dossier de cumplimiento y métricas: C-18/C-22.
- Facturación del saldo del presupuesto, disparo automático reembolso → nota de crédito, reconsulta a la API de ARCA y notificaciones reales: pendientes explícitos de este change.

## Regla de aplicación

Estas decisiones habilitan únicamente el `/opsx:propose` de C-16 con la migración **`V022__arca_invoicing.sql`**. No autorizan escribir en `src/`, crear la migración durante el propose, commits ni push. Antes del apply se debe verificar que proposal, specs, design y tasks reflejen AR-1..AR-14, la DoD de AR-14 y las exclusiones declaradas; el apply requiere una nueva aprobación humana explícita por la governance CRITICO.
