# whatsapp-notifications Specification

## Purpose
Confirmaciones y recordatorios de turno por WhatsApp Business API (obligatoria en el MVP) sobre un outbox transaccional, con registro de estado e intentos y webhooks firmados e idempotentes (C-10; RN-NT-01, RN-NT-03, RN-GL-02, DD-02). Solo plantillas aprobadas; sin asistente conversacional (C-01 A3).

## Requirements

### Requirement: Solo plantillas aprobadas
El sistema SHALL encolar un mensaje únicamente si existe una plantilla `APROBADA` y activa para su propósito (`CONFIRMACION` o `RECORDATORIO`). Si la plantilla falta o no está aprobada, la notificación MUST quedar `BLOQUEADA`, no enviarse y emitir `notificacion.bloqueada` como aviso a administración. La aprobación se vuelve a comprobar al despachar. Los códigos de plantilla son `(TBD)` y no se siembran.

#### Scenario: Envío con plantilla aprobada
- **GIVEN** una plantilla aprobada de confirmación
- **WHEN** se programa y se despacha la notificación
- **THEN** queda `ENVIADA` con `proveedor_message_id`, un intento `EXITO` registrado y eventos `notificacion.programada` y `notificacion.enviada`

#### Scenario: Plantilla rechazada
- **GIVEN** la plantilla del propósito rechazada por Meta, antes o después de programar
- **WHEN** se programa o despacha
- **THEN** la notificación queda `BLOQUEADA`, el proveedor no recibe nada y se emite `notificacion.bloqueada`

### Requirement: Registro de intentos y reintentos
Cada intento SHALL registrarse en `notificacion_intentos` (append-only) con número, resultado, `proveedor_message_id` y error. Los errores se reintentan con espera lineal hasta `notificaciones.max-intentos`; luego la notificación pasa a `FALLIDA` y se emite `notificacion.fallida`. Lo ya enviado MUST NOT reenviarse. La programación es idempotente por turno, propósito e instante.

#### Scenario: Reintento hasta el máximo
- **GIVEN** un proveedor que falla
- **WHEN** se agotan los intentos configurados
- **THEN** quedan registrados todos los intentos y la notificación queda `FALLIDA`

### Requirement: Webhook firmado e idempotente
El webhook SHALL verificar la firma HMAC-SHA256 (`sha256=<hex>`) del cuerpo crudo con `WHATSAPP_APP_SECRET`; sin secreto, sin firma o con firma inválida MUST descartarse sin efectos y auditarse `whatsapp.webhook.rechazado`. Cada evento se aplica una sola vez; los estados no retroceden y la respuesta de botón a una confirmación la marca `CONFIRMADA`.

#### Scenario: Webhook sin firma
- **WHEN** llega un webhook sin firma válida
- **THEN** se rechaza, no cambia ninguna notificación y queda auditado el rechazo

#### Scenario: Reintento duplicado del proveedor
- **WHEN** el mismo evento llega dos veces
- **THEN** se aplica una sola vez

### Requirement: Proveedor no configurado falla cerrado
Fuera de `dev`/`test`, mientras el adaptador real sea `(TBD)`, todo envío SHALL registrarse como intento con error `proveedor-whatsapp-no-configurado`; nunca se simula un envío exitoso.

#### Scenario: Perfil productivo sin adaptador
- **GIVEN** un perfil distinto de `dev`/`test`
- **WHEN** se despacha una notificación
- **THEN** el intento queda con error y no se contacta a terceros
