# deposit-payments Specification

## Purpose

Define el contrato observable de la seña cobrada con Mercado Pago: solicitud idempotente, recepción autenticada del webhook, estados del pago, conciliación con el turno o presupuesto y reembolso total, sin inventar contabilidad de caja.

## Requirements

### Requirement: Solicitud de seña con referencia idempotente

El sistema SHALL crear una seña con `monto` decimal en `ARS`, proveedor `mercadopago`, `estado=pendiente`, `external_reference` derivada de una clave única generada por el sistema y asociación opcional a `turno_id` y `presupuesto_id`. La misma clave SHALL viajar como clave de idempotencia de la solicitud; un reenvío MUST NOT crear una segunda seña. La solicitud SHALL estar disponible solo en entornos de desarrollo y prueba, sin credenciales de staff ni permisos nuevos.

#### Scenario: Solicitud con presupuesto y turno

- **GIVEN** un presupuesto vigente en `ARS`, su turno asociado y un monto de seña válido
- **WHEN** se solicita la seña con una clave única
- **THEN** se crea una sola seña `pendiente` con `proveedor=mercadopago`, `moneda=ARS`, ambas referencias y una `external_reference` derivada de la clave

#### Scenario: Solicitud sin presupuesto

- **GIVEN** un turno existente sin presupuesto asociado y un monto válido
- **WHEN** se solicita la seña
- **THEN** la seña se crea con `presupuesto_id` nulo y conserva el `turno_id`

#### Scenario: Reenvío de la solicitud

- **GIVEN** una seña ya creada con una clave de idempotencia
- **WHEN** se repite la solicitud con la misma clave
- **THEN** no se crea otra seña y la respuesta corresponde a la solicitud original

### Requirement: Verificación de firma del webhook

El sistema MUST verificar localmente la firma HMAC-SHA256 del webhook usando el componente `v1` de `x-signature`, el timestamp `ts` y `x-request-id` como mensaje, con el secreto configurado para Mercado Pago. Una firma ausente, malformada o inválida SHALL responderse con `400`, sin cambios de estado, sin persistencia, sin auditoría y sin revelar el secreto ni el motivo exacto del fallo. El webhook SHALL autenticarse por esta firma, sin token público ni credenciales de staff.

#### Scenario: Webhook con firma válida

- **GIVEN** un webhook con `x-signature`, `x-request-id` y una firma calculada con el secreto configurado
- **WHEN** se recibe en el endpoint del proveedor
- **THEN** se acepta la petición y se procesa el evento de pago

#### Scenario: Firma ausente o inválida

- **GIVEN** un webhook sin firma o con un valor de firma manipulado
- **WHEN** se recibe en el endpoint del proveedor
- **THEN** se responde `400`, la seña permanece `pendiente` y no se inserta ninguna fila de auditoría

#### Scenario: Secreto nunca expuesto

- **GIVEN** peticiones con firma válida e inválida
- **WHEN** se inspeccionan respuestas, logs y metadata de auditoría
- **THEN** el secreto configurado no aparece en ninguno de ellos

### Requirement: Idempotencia ante reenvío del webhook

El sistema SHALL registrar la clave de deduplicación del webhook en `public_idempotencia`, operación `pago.webhook`, en la misma transacción que el cambio de estado. La clave SHALL derivarse de `x-request-id` o del par `external_reference`/`ts`. Un reenvío con la misma clave SHALL responder `200` sin segundo efecto; una reversión MUST NOT dejar la clave registrada.

#### Scenario: Webhook duplicado

- **GIVEN** un webhook aprobado ya procesado para una seña
- **WHEN** el proveedor reenvía el mismo `x-request-id`
- **THEN** se responde `200`, la seña sigue `aprobado` y no se emite un segundo evento de pago

#### Scenario: Eventos distintos

- **GIVEN** dos webhooks con claves de deduplicación distintas para la misma seña
- **WHEN** se procesan en orden
- **THEN** ambos son aceptados y solo el primero produce el cambio de estado permitido

#### Scenario: Reversión atómica

- **GIVEN** un webhook cuya escritura de auditoría falla
- **WHEN** se procesa la petición
- **THEN** no se registra la clave de idempotencia, no cambia el estado de la seña y no queda fila de auditoría

### Requirement: Transiciones de estado del pago

El sistema SHALL registrar el estado como `pendiente`, `aprobado`, `rechazado`, `cancelado` o `reembolsado`, según `pending`, `approved`, `rejected`, `cancelled` y `refunded`/`charged_back`. Solo SHALL permitirse transiciones desde `pendiente`; los demás estados son terminales y un evento tardío SHALL descartarse sin efectos. `fecha_aprobacion` SHALL registrar el instante de observación del evento aprobado.

#### Scenario: Pago aprobado

- **GIVEN** una seña `pendiente`
- **WHEN** llega un webhook con estado `approved`
- **THEN** la seña pasa a `aprobado` y `fecha_aprobacion` contiene el instante de observación

#### Scenario: Pago rechazado sin pérdida del turno

- **GIVEN** una seña `pendiente` ligada a un turno confirmado
- **WHEN** llega un webhook con estado `rejected`
- **THEN** la seña pasa a `rechazado`, el turno permanece confirmado y se solicita aviso a recepción

#### Scenario: Contracargo y reenvío tardío

- **GIVEN** una seña `aprobado`
- **WHEN** llega un `charged_back` y después un `approved` reenviado
- **THEN** la seña queda `reembolsado` y el evento `approved` tardío se descarta sin efectos

### Requirement: Conciliación turno–pago

El sistema SHALL marcar `conciliado` una seña `aprobado` únicamente cuando su `turno_id` o `presupuesto_id` resuelve a una entidad existente, y SHALL emitir el evento de conciliación correspondiente. Una referencia inexistente SHALL dejar la seña sin conciliar y NO SHALL crear turnos, presupuestos ni asientos. El sistema MUST NOT crear tablas ni registros de caja, asiento ni arqueo en este change.

#### Scenario: Conciliación con turno existente

- **GIVEN** una seña `aprobado` con `turno_id` existente
- **WHEN** se ejecuta la conciliación
- **THEN** `conciliado` queda activo y se registra un evento `pago.conciliado`

#### Scenario: Referencia inexistente

- **GIVEN** una seña `aprobado` cuyo `turno_id` no existe
- **WHEN** se ejecuta la conciliación
- **THEN** la seña no queda conciliada, no se crea un turno y se informa la inconsistencia

#### Scenario: Sin caja ni arqueo

- **GIVEN** el esquema resultante de la migración de este change
- **WHEN** se inspeccionan sus tablas
- **THEN** no existen tablas de caja, asiento ni arqueo

### Requirement: Reembolso total y liberación del hueco

El sistema SHALL permitir a recepción reembolsar una seña `aprobado` por su importe total contra el proveedor, registrando `reembolsado`. La política es reembolso total y liberación inmediata del hueco, sin retención; la liberación la realiza el flujo de cancelación existente. Una seña `rechazado` MUST NOT reembolsarse y SHALL conservar el turno. Igual política SHALL aplicarse dentro y fuera de la ventana.

#### Scenario: Reembolso de pago aprobado

- **GIVEN** una seña `aprobado` y un turno confirmado
- **WHEN** recepción solicita el reembolso
- **THEN** se revierte el total, la seña queda `reembolsado`, se emite `pago.reembolsado` y el hueco queda disponible sin retención

#### Scenario: Reembolso de pago rechazado

- **GIVEN** una seña `rechazado` con turno confirmado
- **WHEN** recepción intenta reembolsar
- **THEN** la operación se rechaza, no se llama al proveedor y el turno sigue confirmado

#### Scenario: Cancelación fuera de ventana

- **GIVEN** una seña `aprobado` cancelada fuera de la ventana de 48 hs hábiles
- **WHEN** recepción solicita el reembolso
- **THEN** se aplica la misma política de reembolso total y liberación inmediata

### Requirement: Aviso simulado al paciente

El sistema SHALL solicitar el aviso al paciente a través de un puerto de notificación con implementación simulada en desarrollo y prueba, sin envío real y sin afirmar entrega. NO SHALL crear tipos de auditoría de notificación ni modificar el change de WhatsApp.

#### Scenario: Aviso tras rechazo

- **GIVEN** una seña rechazada con turno confirmado
- **WHEN** se procesa el evento de rechazo
- **THEN** se registra la solicitud de aviso en el puerto simulado y no se envía ningún mensaje real

#### Scenario: Sin evento de entrega

- **GIVEN** una solicitud de aviso simulada
- **WHEN** se consultan los tipos de auditoría disponibles
- **THEN** no existe un tipo de notificación o entrega asociado

### Requirement: Depósito de waitlist sobre el ciclo de seña existente

El sistema SHALL soportar un depósito opcional por entrada de waitlist reutilizando el ciclo de seña de Mercado Pago ya definido: la entrada conserva `deposito_requerido` y `senia_id` opcional sin FK física, y el cobro SHALL crear una sola seña por turno adjudicado. El sistema MUST NOT crear tablas de pago, proveedor nuevo, caja, asiento, arqueo ni liquidación contable. El monto productivo permanece `(TBD) -> forma de obtención` (PO Parte 3 y medición del piloto).

#### Scenario: Depósito requerido con seña única

- **GIVEN** una entrada de waitlist con depósito requerido y un turno adjudicado
- **WHEN** se solicita el depósito dos veces con la misma clave
- **THEN** se crea una sola seña `pendiente` en `ARS` y el segundo intento no genera otra

#### Scenario: Entrada sin depósito

- **GIVEN** una entrada de waitlist sin depósito requerido
- **WHEN** se adjudica el hueco
- **THEN** la entrada conserva `senia_id` vacío y no se solicita ninguna seña

#### Scenario: Conciliación por flag y reembolso total

- **GIVEN** una seña de waitlist aprobada y conciliada que luego se cancela
- **WHEN** se completa la conciliación y se procesa el reembolso
- **THEN** se actualiza el flag `conciliado`, se reembolsa el total y se libera el hueco de inmediato, sin crear asiento ni caja
