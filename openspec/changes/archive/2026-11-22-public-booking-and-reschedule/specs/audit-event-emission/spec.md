# Spec Delta

## ADDED Requirements

### Requirement: Eventos de la superficie pública de reserva
El sistema SHALL emitir `turno.confirmado` y `turno.cancelado` para las operaciones públicas confirmadas, y SHALL reutilizar `turno.creado` y `turno.reprogramado` cuando el turno se originó en enlace. El actor SHALL registrar modo `PACIENTE` sin identidad verificada. Una operación rechazada MUST NOT emitir evento de confirmación.

#### Scenario: Confirmación y cancelación públicas
- **GIVEN** una confirmación y una cancelación pública exitosas
- **WHEN** se consulta el registro de eventos
- **THEN** se encuentran `turno.confirmado` y `turno.cancelado`, uno por operación, con actor modo `PACIENTE`

#### Scenario: Reprogramación pública
- **GIVEN** una reprogramación pública exitosa
- **WHEN** se inspecciona el evento
- **THEN** usa el tipo existente `turno.reprogramado` y conserva el modo `PACIENTE`

#### Scenario: Operación rechazada sin evento
- **GIVEN** una operación pública rechazada por token inválido, hueco tomado, fuera de ventana o reenvío duplicado
- **WHEN** se consulta el registro de eventos
- **THEN** no existe evento de confirmación, cancelación ni creación para esa operación

### Requirement: Metadata sanitizada de eventos públicos
La metadata de los eventos públicos SHALL limitarse a `turnoId`, `canal=link`, `origen=link`, hash de la `idempotency_key` y `motivo` cuando corresponda. MUST NOT incluir el token, su `jti`, `telefono`, `email`, `dni` ni otros datos personales; el `correlationId` SHALL ser distinto por operación. No se crea tipo de auditoría para notificaciones.

#### Scenario: Metadata permitida
- **GIVEN** una operación pública confirmada
- **WHEN** se inspecciona la metadata del evento
- **THEN** contiene solo las claves permitidas y un `correlationId` propio de la operación

#### Scenario: Sin token ni datos personales
- **GIVEN** una operación pública que transporta token e `idempotency_key`
- **WHEN** se serializa el evento
- **THEN** no aparecen el token, su `jti`, la clave en claro ni datos de contacto o documento del paciente

#### Scenario: Sin evento de entrega
- **GIVEN** la solicitud de notificación simulada de una operación pública
- **WHEN** se consulta el catálogo de tipos de auditoría
- **THEN** no existe un tipo de notificación o entrega y los eventos no acreditan envío real de WhatsApp
