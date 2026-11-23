# C-14 — Decisiones del PO para `budgets-and-cash` (Parte 3)

- **Change**: `C-14 budgets-and-cash`
- **Governance local**: BAJO (CRUD de documento/catálogo; sin cobro ni facturación)
- **Decisor**: Product Owner del equipo de Parte 3
- **Fecha**: 2026-11-23
- **Estado**: aprobado — opciones recomendadas más simples, seguras y alineadas con C-01..C-09
- **Alcance de este documento**: cerrar los 15 puntos abiertos del explore para habilitar `/opsx:propose`. No implementa código, no crea migraciones, no modifica `src/` y no autoriza commits.
- **Fuentes**: `CHANGES.md` §C-14 y §«Reservas de numeración de migraciones Flyway», `docs/decisiones/C-01-decisiones-habilitantes.md`, `C-02-resoluciones-stack.md`, `C-03-decisiones-core-models.md`, `C-04-decisiones-auth-rbac.md`, `C-05-ajustes-pre-apply-PO.md`, `C-05-deuda-adaptador-actor.md`, `C-06-decisiones-public-access-tokens.md`, `C-07-scheduling-catalogs-checkpoints.md`, `C-08-agenda-core-overlap-checkpoints.md`, `C-09-decisiones.md`, `knowledge-base/02,03,04,05,06,07,09,10` y relevamiento de `src/`.

> Estas decisiones son la respuesta exigida por RN-GL-03: ningún `(TBD)` se resuelve por convención del agente. Lo que no se decide aquí sigue `(TBD)` y se registra como pendiente explícito, no como valor inventado.

## Decisiones aprobadas

### D1 — Dependencia formal de C-08

- **Decisión**: tratar C-08 como habilitador técnico cumplido para C-14. C-05, C-07 y C-08 están implementados y archivados; la deuda pendiente de C-08 (adaptador `AuthenticatedActor` → `CurrentActorProvider` y protección RBAC de la agenda) es orthogonal al dominio financiero y queda para el change posterior ya registrado en `docs/decisiones/C-05-deuda-adaptador-actor.md`.
- **Justificación**: evita bloquear un CRUD de presupuestos por una deuda de identidad que no afecta su modelo ni su persistencia, sin reabrir C-08 ni debilitar sus guardias.

### D2 — Governance

- **Decisión**: mantener **BAJO** según `CHANGES.md:460`, con verificación técnica completa antes de marcar el change. No se agregan checkpoints MEDIO adicionales: las decisiones sensibles (dinero, eventos y permisos) quedan cerradas en esta acta.
- **Justificación**: C-14 no cobra, no concilia y no factura; es un CRUD de documento con cálculo de total y eventos. La autonomía de BAJO se conserva dentro del alcance recortado por D10, D13, D14 y D15.

### D3 — Numeración de Flyway

- **Decisión**: C-14 usa la reserva de Parte 3 y crea, si hace falta, **`V020__budgets_and_cash.sql`**. Los números `V021+` quedan disponibles para C-15 y los demás changes de la Parte 3. No se edita ninguna migración aplicada (V001–V006).
- **Justificación**: `CHANGES.md:35-46` prevalece sobre el “009” histórico del scope; V020 es el primer número libre de la Parte 3 y evita colisiones con los compañeros.

### D4 — Catálogo de eventos de auditoría

- **Decisión**: ampliar el catálogo con **`presupuesto.creado`** y **`presupuesto.modificado`** únicamente. Entidad `presupuesto`, `entidadId = presupuesto.id`, actor `ActorSimulado.recepcionista()` (modo `SIMULADO`), `correlationId` nuevo por operación y metadata limitada a `presupuestoId`, `turnoId` cuando exista, `total`, `moneda`, `cantidadItems` y `motivo` cuando corresponda. Sin evento de entrega, cobro ni caja en C-14.
- **Justificación**: cumple la regla de `docs/decisiones/C-05-ajustes-pre-apply-PO.md` de proponer eventos nuevos por escrito antes de implementarlos, reutiliza el patrón de C-09 y evita filtrar datos personales o secretos en `metadata`.

### D5 — Moneda y tipo monetario

- **Decisión**: moneda fija **`ARS`**, importes `NUMERIC(12,2)` y redondeo `HALF_UP` a 2 decimales. Impuestos incluidos en el precio, coherente con el precio plano de C-01 A5. No se crea tabla de monedas ni configuración de divisas en C-14.
- **Justificación**: es la representación mínima suficiente para un consultorio único en Argentina (C-01 A2/B3) y evita un catálogo de monedas especulativo.

### D6 — Origen del precio del Ítem

- **Decisión**: `Ítem` guarda un **precio snapshot** informado por recepción, con `tratamiento_id` como FK al catálogo de C-07. No se agrega `precio` a `tratamientos`, no se generan seeds y no se modifica `ConfiguracionSchedulingService` ni su semántica de lectura.
- **Justificación**: el catálogo de C-07 solo define `codigo`, `nombre` y `duracionMinutos` y sus decisiones prohíben cambiar esa lectura; el precio acordado con el paciente es un dato del presupuesto, no una propiedad inmutable del tratamiento.

### D7 — Cálculo del total

- **Decisión**: `total = ROUND(SUM(cantidad * precio), 2)` con `HALF_UP`, aplicado al final. `cantidad` es entero > 0 y `precio >= 0`. No hay descuentos, recargos ni redondeo por ítem en C-14.
- **Justificación**: es la fórmula mínima verificable por tests y evita reglas comerciales no acordadas (RN-GL-03).

### D8 — Estados del presupuesto

- **Decisión**: `estado` es `String` con valores **`emitido`** y **`anulado`**. La creación persiste `emitido`; la modificación se permite mientras esté `emitido`; anular fija `estado=anulado` y `activo=false` usando `AuditableEntity.desactivar`. No se crea enum SQL ni estado `borrador`.
- **Justificación**: cubre el ciclo que necesita el flujo de presupuesto sin inventar un workflow de aprobación; conserva el patrón de `estado`/`origen` como cadenas de C-08/C-09.

### D9 — Asociación al turno

- **Decisión**: `turno_id` es **opcional y nullable**, con FK a `turnos`. Se permite más de un presupuesto por turno y no se modifica el turno por acción del presupuesto. La asociación solo se usa como referencia y como `turnoId` opcional en metadata de auditoría.
- **Justificación**: concreta RN-FI-01 y el `(TBD)` de `CHANGES.md:455` sin acoplar la emisión del presupuesto al ciclo de vida público de C-09 ni crear reglas de cancelling no decididas.

### D10 — Asiento de caja

- **Decisión**: **diferir el asiento de caja y el arqueo a C-15**. C-14 no crea tabla, entidad ni evento de caja; su única salida financiera es el presupuesto con total, moneda, estado y ítems.
- **Justificación**: un presupuesto no es un movimiento de dinero. La KB no define la entidad `Caja`/`Asiento` y C-15 ya posee conciliación, caja y arqueo; crear un asiento aquí obligaría a inventar semántica contable sin base aprobada.

### D11 — Contrato para C-15 y C-16

- **Decisión**: C-14 expone como contrato estable `presupuesto.id`, `total`, `moneda`, `estado`, `turno_id` opcional y sus ítems. C-15 podrá referenciar `presupuesto_id` de forma opcional desde la seña. C-16 consumirá ese mismo presupuesto para ARCA y agregará allí los campos de comprobante/impuestos; C-14 no almacena datos de facturación.
- **Justificación**: mantiene la cadena `C-14 → C-15 → C-16` del roadmap sin duplicar campos ni adelantar decisiones de integración CRITICO de C-15/C-16.

### D12 — Permisos y actor

- **Decisión**: `/presupuestos` sigue el patrón indicativo de C-07/C-08: controlador `@Profile({"dev","test"})`, sin autenticación, sin RBAC, sin cabeceras de identidad y sin rutas `/auth/*`. El actor de auditoría es `ActorSimulado.recepcionista()` (modo `SIMULADO`); no se agrega factory `administrador` ni permisos financieros nuevos en V001.
- **Justificación**: preserva las guardias de C-04 y la deuda de C-05; la distinción recepción/administración de la matriz RBAC queda para el change que proteja la agenda. Evita convertir un CRUD BAJO en cambio de seguridad.

### D13 — Lectura por paciente

- **Decisión**: **diferida explícitamente**. C-14 no expone lectura de presupuestos para pacientes, no crea scope nuevo y no modifica C-06/C-09.
- **Justificación**: el token público actual cubre reserva e historia, no acceso financiero; agregarlo excedería el alcance acotado y elevaría el riesgo de exposición de datos.

### D14 — Impresión/PDF

- **Decisión**: **diferida explícitamente**. C-14 no genera PDF, no agrega dependencia de renderizado ni endpoint de impresión.
- **Justificación**: no hay formato, plantilla ni librería aprobada; el presupuesto queda consultable como dato y el formato de entrega se decide en un change posterior.

### D15 — Exclusiones de alcance

- **Decisión**: C-14 **no** procesa cobros reales, **no** integra Mercado Pago, **no** emite factura ARCA, **no** implementa waitlist ni reembolso, **no** envía notificaciones y **no** crea UI. Esas capacidades corresponden a C-15 (cobro/webhook/conciliación/reembolso), C-16 (ARCA), C-19 (waitlist), C-10 (notificaciones) y un change visual posterior. Tampoco se crean secretos ni se hacen commits.
- **Justificación**: mantiene el MVP sequenceado del roadmap y evita presentar como implementado lo que hoy solo es paridad competitiva o cumplimiento pendiente (DD-05, DD-06, C-01 A1/A4/A5).

## Pendientes explícitos (no resueltos aquí)

- Diseño del formato de impresión/PDF y su plantilla (D14).
- Lectura por paciente y su mecanismo de autorización (D13).
- Esquema y semántica del asiento de caja, arqueo y conciliación (D10, C-15).
- Verificación de firma, idempotencia de webhook y política de reembolso de Mercado Pago (C-15, IN-02).
- Comprobantes, liquidación y campos impositivos de ARCA (C-16, C-01 A1).
- Protección RBAC de la agenda y adaptador de actor real (deuda de C-05, change posterior).

## Regla de aplicación

Las decisiones habilitan únicamente el `/opsx:propose` de C-14. No autorizan escribir en `src/`, crear migraciones durante el propose, commits ni push. Antes del apply se debe verificar que los deltas, diseño y tareas del change reflejen estas quince decisiones y la exclusión de D10, D13, D14 y D15.
