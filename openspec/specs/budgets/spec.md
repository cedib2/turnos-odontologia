# budgets Specification

## Purpose

Define el comportamiento observable de la emisión y gestión básica de presupuestos odontológicos con ítems a precio snapshot, total en ARS, estados acotados y un contrato estable que los changes de cobro y facturación puedan consumir sin acoplarse a la agenda.

## Requirements

### Requirement: Superficie de presupuestos aislada
El sistema SHALL exponer la gestión de presupuestos bajo `/presupuestos` solo en perfiles `dev`/`test`, sin exigir credenciales de staff, RBAC ni cabeceras de identidad. La superficie MUST NOT modificar `/agenda`, `/turnos`, `/bloqueos`, `/sobreturnos`, los catálogos C-07 ni la cadena `/auth/**`.

#### Scenario: Acceso sin credenciales de staff
- **GIVEN** el perfil `dev` o `test` y ninguna credencial de staff
- **WHEN** se invoca una operación de `/presupuestos`
- **THEN** la operación se resuelve sin login, RBAC ni cabeceras de identidad

#### Scenario: Superficie existente intacta
- **GIVEN** los controladores existentes de agenda, turnos, bloqueos, sobreturnos y catálogos y la cadena `/auth/**`
- **WHEN** se inspecciona el cambio
- **THEN** permanecen sin modificar, conservando sus perfiles, guardias y semántica actual

### Requirement: Presupuesto con ítems a precio snapshot
El sistema SHALL persistir un presupuesto con paciente, ítems, moneda fija `ARS`, total, estado y borrado lógico. Cada ítem SHALL registrar `tratamiento_id`, `cantidad`, `descripción` y un `precio` snapshot informado por recepción; MUST NOT leer ni escribir un precio en el catálogo de C-07 ni crear seeds.

#### Scenario: Ítem con precio snapshot
- **GIVEN** un tratamiento existente del catálogo C-07 y un precio acordado informado por recepción
- **WHEN** se crea el presupuesto con ese tratamiento, cantidad y descripción
- **THEN** el ítem conserva el precio informado como dato del presupuesto

#### Scenario: Catálogo sin precio
- **GIVEN** la tabla `tratamientos` y su entidad de dominio
- **WHEN** se inspecciona el cambio
- **THEN** no tienen columna `precio`, no se generan seeds y la lectura de duración permanece intacta

### Requirement: Cálculo determinista del total
El sistema SHALL calcular `total = ROUND(SUM(cantidad * precio), 2)` con `HALF_UP`, aplicado al final, usando importes `NUMERIC(12,2)`. La `cantidad` MUST ser entero positivo y el `precio` MUST ser mayor o igual a cero. El total SHALL incluir impuestos y MUST NOT aplicar descuentos, recargos ni redondeo por ítem.

#### Scenario: Total con varios ítems
- **GIVEN** dos ítems con cantidades y precios válidos
- **WHEN** se emite el presupuesto
- **THEN** el total es la suma redondeada a dos decimales de `cantidad * precio` de cada ítem

#### Scenario: Redondeo HALF_UP
- **GIVEN** una suma cuyo tercer decimal es cinco
- **WHEN** se calcula el total
- **THEN** el resultado se redondea hacia arriba al segundo decimal

#### Scenario: Dato de ítem inválido
- **GIVEN** un ítem con cantidad cero, negativa o precio negativo
- **WHEN** se intenta emitir o modificar el presupuesto
- **THEN** la operación se rechaza sin persistir cambios ni emitir evento

### Requirement: Estados y transiciones del presupuesto
El sistema SHALL usar `estado` como cadena con valores `emitido` y `anulado`. La creación SHALL persistir `emitido`; la modificación SHALL permitirse solo mientras el presupuesto esté `emitido`; la anulación SHALL fijar `estado=anulado` y `activo=false` mediante borrado lógico. El sistema MUST NOT crear enum SQL, estado `borrador` ni borrado físico.

#### Scenario: Creación emitida
- **GIVEN** un presupuesto válido con paciente y al menos un ítem
- **WHEN** se crea
- **THEN** se persiste con `estado=emitido`, `activo=true` y `moneda=ARS`

#### Scenario: Modificación permitida
- **GIVEN** un presupuesto `emitido`
- **WHEN** se modifican sus ítems o descripción
- **THEN** se recalcula el total y el presupuesto continúa `emitido`

#### Scenario: Modificación de un presupuesto anulado
- **GIVEN** un presupuesto `anulado`
- **WHEN** se intenta modificarlo
- **THEN** la operación se rechaza sin cambios ni evento

#### Scenario: Anulación
- **GIVEN** un presupuesto `emitido`
- **WHEN** se anula
- **THEN** pasa a `estado=anulado` y `activo=false`, conservando sus ítems y total

### Requirement: Asociación opcional al turno
El sistema SHALL aceptar un `turno_id` nullable con FK a `turnos`, permitir varios presupuestos por turno y usar esa asociación solo como referencia y como metadata opcional de auditoría. El presupuesto MUST NOT modificar el turno, su estado, su ocupación ni su ciclo de vida público de C-09.

#### Scenario: Presupuesto vinculado
- **GIVEN** un turno existente
- **WHEN** se emite un presupuesto con ese `turno_id`
- **THEN** queda vinculado por FK sin alterar el turno

#### Scenario: Presupuesto sin turno
- **GIVEN** un presupuesto emitido desde recepción sin turno
- **WHEN** se consulta
- **THEN** su `turno_id` es nulo y la operación es válida

#### Scenario: Turno cancelado o reprogramado
- **GIVEN** un presupuesto vinculado a un turno que luego se cancela o reprograma
- **WHEN** se consulta el presupuesto
- **THEN** conserva su estado, total y asociación sin efecto sobre la agenda

### Requirement: Contrato estable para cobro y facturación
El sistema SHALL exponer como contrato estable `presupuesto.id`, `total`, `moneda`, `estado`, `turno_id` opcional y sus ítems, de modo que C-15 pueda referenciar `presupuesto_id` de forma opcional y C-16 consuma el mismo presupuesto. C-14 MUST NOT almacenar datos de comprobante, impuestos, cobro, conciliación ni caja.

#### Scenario: Respuesta con contrato completo
- **GIVEN** un presupuesto emitido con y sin turno
- **WHEN** se consulta por id y se lista
- **THEN** ambas respuestas incluyen id, total, moneda, estado, turno_id opcional e ítems

#### Scenario: Sin datos de cobro ni factura
- **GIVEN** un presupuesto emitido
- **WHEN** se inspeccionan sus campos y eventos
- **THEN** no hay estado de pago, external_reference, comprobante, impuesto ni asiento de caja

### Requirement: Exclusiones explícitas del alcance
El sistema MUST NOT exponer lectura de presupuestos para pacientes, generación de PDF, asiento de caja, arqueo, cobro real, integración con Mercado Pago, reembolso, waitlist ni notificaciones. Esas capacidades SHALL quedar para C-15, C-16, C-19, C-10 y un change visual posterior según corresponda.

#### Scenario: Sin lectura pública ni impresión
- **GIVEN** un paciente con token público vigente de C-06/C-09
- **WHEN** intenta consultar un presupuesto o solicitar su PDF
- **THEN** no existe superficie que lo permita y no se crea scope nuevo

#### Scenario: Sin movimientos de dinero
- **GIVEN** la emisión, modificación o anulación de un presupuesto
- **WHEN** se inspeccionan tablas y eventos
- **THEN** no se crea asiento de caja, seña, pago ni evento de cobro o reembolso
