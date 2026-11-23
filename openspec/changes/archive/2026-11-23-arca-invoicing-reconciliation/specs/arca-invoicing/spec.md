# Spec Delta

## Purpose

Define el ciclo de vida observable del comprobante ARCA emitido a partir de una seña aprobada: estados, idempotencia, reintentos, nota de crédito y una superficie de prueba sin red ni secretos reales.

## ADDED Requirements

### Requirement: Comprobante por seña aprobada

El sistema SHALL crear como máximo un comprobante `factura` por seña `aprobado`, por `senia.monto` en `ARS`, con `senia_id` obligatorio y `presupuesto_id`/`turno_id` heredados de la seña. La letra SHALL configurarse, con valor de prueba `C`, y el IVA SHALL considerarse no discriminado. El sistema MUST NOT crear campos de impuestos, renglones ni desglose de costos de API.

#### Scenario: Seña con presupuesto

- **GIVEN** una seña `aprobado` con `presupuesto_id` y `turno_id`
- **WHEN** se emite el comprobante
- **THEN** se crea una sola `factura` por el monto de la seña, en `ARS`, con las tres referencias y un concepto derivado del presupuesto

#### Scenario: Seña sin presupuesto

- **GIVEN** una seña `aprobado` sin `presupuesto_id`
- **WHEN** se emite el comprobante
- **THEN** se crea una `factura` con `presupuesto_id` nulo, `turno_id` heredado si existe y concepto genérico de seña odontológica

#### Scenario: Seña no aprobada

- **GIVEN** una seña `pendiente`, `rechazado` o `reembolsado`
- **WHEN** se solicita la emisión
- **THEN** no se crea comprobante y la operación se rechaza sin efectos

#### Scenario: Saldo del presupuesto no facturado

- **GIVEN** un presupuesto con total superior a la seña cobrada
- **WHEN** se emite el comprobante de la seña
- **THEN** el monto del comprobante es el de la seña y no se crea comprobante por el saldo

### Requirement: Estados y transiciones del comprobante

El sistema SHALL registrar `tipo` en `factura`/`nota_credito` y `estado` en `pendiente`/`emitido`/`rechazado`/`anulado`. Solo SHALL permitirse `pendiente → emitido|rechazado`, `rechazado → pendiente` por reintento manual y `emitido → anulado` por nota de crédito. Toda transición no listada SHALL rechazarse sin cambios de estado ni auditoría.

#### Scenario: Emisión confirmada

- **GIVEN** un comprobante `pendiente`
- **WHEN** el adaptador devuelve un CAE válido
- **THEN** el comprobante pasa a `emitido`, conserva el CAE y registra `fecha_emision`

#### Scenario: Emisión rechazada

- **GIVEN** un comprobante `pendiente`
- **WHEN** el adaptador informa un error de emisión
- **THEN** el comprobante pasa a `rechazado` sin CAE y queda habilitado para reintento manual

#### Scenario: Reintento desde rechazado

- **GIVEN** un comprobante `rechazado`
- **WHEN** recepción solicita un reintento
- **THEN** el comprobante vuelve a `pendiente` y conserva su identificador

#### Scenario: Anulación por nota de crédito

- **GIVEN** una factura `emitida` con nota de crédito conferida
- **WHEN** se registra la nota de crédito
- **THEN** la factura pasa a `anulado` y la nota queda `emitido`

#### Scenario: Transición no permitida

- **GIVEN** un comprobante `anulado`
- **WHEN** se solicita reemitirlo
- **THEN** la operación se rechaza, el estado permanece `anulado` y no se emite auditoría

### Requirement: Idempotencia de emisión en la misma transacción

El sistema SHALL registrar la operación `factura.emitir` en `public_idempotencia` (V004) con clave derivada de `senia.id`, en la misma transacción que el cambio de estado y su evento de auditoría. Un reenvío SHALL responder sin crear un segundo comprobante; una reversión MUST NOT dejar la clave registrada. Los reintentos SHALL reutilizar la misma clave.

#### Scenario: Reenvío de emisión

- **GIVEN** un comprobante ya creado para una seña
- **WHEN** se repite la solicitud con la misma seña
- **THEN** no se crea otro comprobante, no se registra otra clave y la respuesta corresponde al original

#### Scenario: Reversión atómica

- **GIVEN** una emisión cuya escritura de auditoría falla
- **WHEN** se procesa la solicitud
- **THEN** no se registra la clave de idempotencia, no queda comprobante ni evento y el estado de la seña no cambia

#### Scenario: Reintento con la misma clave

- **GIVEN** un comprobante `rechazado` con su clave de idempotencia registrada
- **WHEN** se reintenta la emisión
- **THEN** se reutiliza la clave, no se crea un segundo comprobante y el intento queda registrado

### Requirement: Reintento manual y errores saneados

El sistema SHALL permitir a recepción reintentar un comprobante `rechazado`, incrementando `intentos` y registrando en `ultimo_error` solo texto saneado. Ese texto MUST NOT contener tokens, credenciales, secretos, CAE ni datos personales. El sistema MUST NOT instrumentar scheduler, outbox ni reintento automático.

#### Scenario: Reintento exitoso

- **GIVEN** un comprobante `rechazado` con un intento previo
- **WHEN** recepción reintenta y el adaptador devuelve un CAE
- **THEN** el comprobante queda `emitido`, `intentos` se incrementa y `ultimo_error` queda vacío

#### Scenario: Error saneado

- **GIVEN** un adaptador que responde con un mensaje que incluye un token de prueba
- **WHEN** se registra el error del intento
- **THEN** `ultimo_error` no contiene el token ni credenciales y conserva solo una descripción genérica del fallo

#### Scenario: Sin reintento automático

- **GIVEN** un comprobante `rechazado`
- **WHEN** transcurre el tiempo sin intervención de recepción
- **THEN** el estado no cambia y no se registra ningún intento adicional

### Requirement: Nota de crédito manual tras reembolso

El sistema SHALL permitir a recepción emitir una `nota_credito` por el total de una factura `emitida` solo cuando la seña esté `reembolsado`; la factura pasa a `anulado`. Una seña no reembolsada SHALL rechazar la operación. El sistema MUST NOT modificar el change archivado de pagos ni disparar la nota desde el reembolso.

#### Scenario: Nota de crédito válida

- **GIVEN** una factura `emitida` cuya seña está `reembolsado`
- **WHEN** recepción solicita la nota de crédito
- **THEN** se crea la `nota_credito` por el total, la factura pasa a `anulado` y se registra `nota_credito.emitida`

#### Scenario: Seña no reembolsada

- **GIVEN** una factura `emitida` cuya seña está `aprobado`
- **WHEN** se solicita la nota de crédito
- **THEN** la operación se rechaza, no se crea nota y la factura sigue `emitido`

#### Scenario: Sin disparo automático

- **GIVEN** una seña reembolsada con factura emitida
- **WHEN** finaliza el reembolso en el flujo de pagos
- **THEN** no se emite nota de crédito automáticamente; solo queda disponible la operación manual de recepción

### Requirement: Superficie aislada y adaptador simulado

El sistema SHALL exponer las rutas de comprobantes solo en perfiles `dev`/`test`, sin credenciales de staff, RBAC nuevo ni cambios en la cadena `/auth/**`. En esos perfiles SHALL utilizar un adaptador de facturación simulado que devuelve un CAE determinista de prueba y MUST NOT realizar llamadas de red al proveedor real.

#### Scenario: Rutas limitadas a prueba

- **GIVEN** los perfiles `dev` y `test`
- **WHEN** se consultan las rutas de comprobantes
- **THEN** están disponibles emisión, reintento, nota de crédito y consulta, sin exigir credenciales ni permisos nuevos

#### Scenario: CAE simulado

- **GIVEN** una emisión en perfil `test`
- **WHEN** se consulta el comprobante resultante
- **THEN** contiene un CAE determinista marcado como de prueba y no como evidencia de facturación real

#### Scenario: Sin llamada de red

- **GIVEN** el adaptador simulado activo
- **WHEN** se emite, reintenta o anula un comprobante
- **THEN** no se realiza ninguna petición HTTP al proveedor real

### Requirement: Configuración sin secretos en el repositorio

El sistema SHALL declarar `ARCA_BASE_URL` y `ARCA_ACCESS_TOKEN` solo como nombres en `.env.example`, con valores por defecto seguros y no productivos en el perfil `test` que permitan ejecutar GitHub Actions sin secretos. Los valores reales MUST NOT escribirse en el repositorio, logs ni metadata de auditoría.

#### Scenario: Perfil test sin secretos

- **GIVEN** el perfil `test` sin variables de entorno ARCA definidas
- **WHEN** arranca el contexto de pruebas
- **THEN** se usan valores por defecto no productivos y el contexto inicia correctamente

#### Scenario: Nombres sin valores

- **GIVEN** `.env.example`
- **WHEN** se inspeccionan las entradas ARCA
- **THEN** aparecen solo los nombres de las variables, sin tokens ni URLs productivas

#### Scenario: Secretos fuera de logs y metadata

- **GIVEN** una emisión que transporta credenciales del proveedor
- **WHEN** se inspeccionan logs, respuesta y metadata de auditoría
- **THEN** la credencial no aparece en ninguno de ellos
