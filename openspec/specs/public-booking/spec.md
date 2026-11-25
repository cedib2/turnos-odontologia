# public-booking Specification

## Purpose

Define el comportamiento observable de la reserva pública por enlace: consulta de huecos, creación, confirmación, cancelación y reprogramación con token, con write-back real en agenda, idempotencia, alternativas ante hueco tomado y ventana de 48 hs hábiles.

## Requirements

### Requirement: Superficie pública versionada y aislada
El sistema SHALL exponer la reserva pública bajo `/publico/v1/turnos`, con `GET` para disponibilidad y `POST` para crear, confirmar, cancelar y reprogramar. El token SHALL viajar en la query `token`. La superficie MUST NOT exigir credenciales de staff, RBAC ni cabeceras de identidad, y MUST NOT modificar las rutas `dev`/`test` `/agenda`, `/turnos`, `/bloqueos`, `/sobreturnos` ni catálogos.

#### Scenario: Acceso sin credenciales de staff
- **GIVEN** un token público vigente y ninguna credencial de staff
- **WHEN** se invoca una operación de `/publico/v1/turnos`
- **THEN** la operación se resuelve por el token sin login, RBAC ni cabeceras de identidad

#### Scenario: Superficie existente intacta
- **GIVEN** los controladores `dev`/`test` de agenda, turnos, bloqueos, sobreturnos y catálogos y la cadena `/auth/**`
- **WHEN** se inspecciona el cambio
- **THEN** permanecen sin modificar, conservando sus perfiles, guardias y semántica actual

#### Scenario: Error público con headers de confidencialidad
- **GIVEN** una operación pública rechazada por token inválido, conflicto, vencimiento o entidad inexistente
- **WHEN** se inspecciona la respuesta
- **THEN** usa `{"error":"<motivo>"}` con `400`, `409`, `410` o `404` según el caso e incluye `Referrer-Policy: no-referrer` y `Cache-Control: no-store`

### Requirement: Scopes por operación y vinculación al turno
El sistema SHALL aceptar `RESERVAR` solo para consultar disponibilidad y crear turno, y `GESTIONAR` solo para confirmar, cancelar y reprogramar el turno ligado al claim `turnoId`. MUST NOT aceptar otros scopes ni autorizar operación cuyo `turnoId` de ruta difiera del claim. No se crea un scope `CONFIRMAR`.

#### Scenario: Reserva con scope correcto
- **GIVEN** un token `RESERVAR` vigente
- **WHEN** se consulta disponibilidad o se crea un turno
- **THEN** la operación se autoriza sin exigir identidad verificada

#### Scenario: Gestión ligada al turno
- **GIVEN** un token `GESTIONAR` vigente con `turnoId`
- **WHEN** se confirma, cancela o reprograma ese mismo turno
- **THEN** la operación se autoriza por el token

#### Scenario: Scope o turno ajeno rechazado
- **GIVEN** un token con scope distinto al de la operación o cuyo `turnoId` no coincide con el de la ruta
- **WHEN** se intenta la operación
- **THEN** el sistema la rechaza sin efectos de negocio y sin exponer el motivo del fallo de firma

### Requirement: Consulta de disponibilidad pública
El sistema SHALL ofrecer solo huecos calculados con la configuración vigente de tratamiento, horario y feriado, excluyendo bloqueos y turnos activos, interpretados en `America/Argentina/Buenos_Aires`. La consulta MUST NOT crear turnos ni consumir el token `RESERVAR`. La anticipación mínima y máxima sigue `(TBD)` fuera de este change.

#### Scenario: Huecos ofrecidos
- **GIVEN** un profesional, un tratamiento con duración configurada y una fecha con horario laboral
- **WHEN** se consulta la disponibilidad pública
- **THEN** se devuelven los intervalos libres calculados con esa duración en la zona acordada

#### Scenario: Feriado o bloqueo sin huecos
- **GIVEN** un feriado configurado o un bloqueo que cubre la jornada
- **WHEN** se consulta la disponibilidad pública
- **THEN** esos intervalos no se ofrecen como reservables

#### Scenario: Consulta sin consumo de token
- **GIVEN** un token `RESERVAR` vigente
- **WHEN** se consulta la disponibilidad dos veces
- **THEN** ambas responden y el token sigue disponible para crear el turno

### Requirement: Creación atómica con write-back
El sistema SHALL crear el turno público en una sola transacción, persistiendo `estado=reservado`, `origen=link`, el `jti` del token de gestión y la ocupación que reserva profesional y sillón, junto con el registro de idempotencia. Si la escritura falla por solapamiento, MUST NOT publicar evento ni dejar ocupación alguna.

#### Scenario: Creación exitosa
- **GIVEN** un token `RESERVAR` vigente, un paciente existente y un hueco libre
- **WHEN** se crea el turno público con `idempotency_key`
- **THEN** se persisten turno y ocupación, el turno aparece en la agenda y la respuesta entrega el token `GESTIONAR` una sola vez

#### Scenario: Fallo sin efectos
- **GIVEN** un hueco ocupado por otro turno o bloqueo
- **WHEN** se intenta crear el turno
- **THEN** no se persisten turno ni ocupación, no se emite evento y se responde conflicto

### Requirement: Hueco tomado y alternativas
Si el hueco deja de estar libre antes de la escritura, el sistema MUST rechazar la creación o la reprogramación con conflicto atómico, sin turno, ocupación ni evento, y SHALL devolver hasta 3 alternativas posteriores del mismo profesional y tratamiento en la fecha pedida. Sin alternativas, la lista SHALL ser vacía y la waitlist no se activa.

#### Scenario: Alternativas ofrecidas
- **GIVEN** un hueco tomado en el momento de crear y otros huecos libres posteriores del mismo profesional y tratamiento
- **WHEN** se rechaza la reserva
- **THEN** la respuesta de conflicto incluye hasta 3 alternativas ordenadas por inicio

#### Scenario: Sin alternativas
- **GIVEN** un hueco tomado y ningún otro hueco libre en la fecha
- **WHEN** se rechaza la reserva
- **THEN** la lista de alternativas es vacía y no se crea entrada de waitlist

#### Scenario: Carrera por el mismo hueco
- **GIVEN** dos solicitudes públicas concurrentes sobre el mismo profesional, sillón e intervalo
- **WHEN** ambas intentan persistir
- **THEN** exactamente una crea turno y ocupación y la otra recibe conflicto sin efectos

### Requirement: Idempotencia de operaciones públicas
Las operaciones `POST` SHALL requerir `idempotency_key` en el cuerpo, nunca en cabecera, con nombres de operación `turno.crear`, `turno.confirmar`, `turno.cancelar` y `turno.reprogramar`. Un reenvío con la misma operación y clave MUST NOT producir un segundo efecto y SHALL responderse como `operacion_duplicada` con `409`. La misma clave SHALL poder reutilizarse en operaciones distintas.

#### Scenario: Reenvío duplicado
- **GIVEN** una creación de turno ya ejecutada con una `idempotency_key`
- **WHEN** llega un reenvío idéntico
- **THEN** no se crea otro turno ni ocupación y la respuesta es `409` con `{"error":"operacion_duplicada"}`

#### Scenario: Misma clave en otra operación
- **GIVEN** una clave usada en `turno.crear`
- **WHEN** se usa la misma clave en `turno.cancelar`
- **THEN** la operación se procesa normalmente porque la unicidad es por operación y clave

### Requirement: Ciclo de vida del token en la reserva
El token `RESERVAR` SHALL permanecer reutilizable para consultas hasta su `exp` y SHALL marcarse como usado solo después de una creación exitosa. El token `GESTIONAR` SHALL conservarse vigente hasta `exp`: C-09 MUST NOT rotarlo automáticamente. Un token vencido SHALL rechazarse con `410` e `requiereNuevoEnlace=true`, sin reemisión en este change.

#### Scenario: Creación agota el token de reserva
- **GIVEN** un token `RESERVAR` usado en consultas y una creación exitosa
- **WHEN** se intenta crear otro turno con el mismo token
- **THEN** el token se rechaza por ya usado sin crear segundo turno

#### Scenario: Gestión sin rotación automática
- **GIVEN** un token `GESTIONAR` utilizado para confirmar
- **WHEN** se cancela o reprograma con el mismo token antes de `exp`
- **THEN** la operación se autoriza sin exigir una nueva emisión

#### Scenario: Token vencido
- **GIVEN** un token firmado cuyo `exp` ya pasó
- **WHEN** se presenta en cualquier operación pública
- **THEN** se rechaza con `410`, `requiereNuevoEnlace=true`, sin efectos y sin endpoint de reemisión en C-09

### Requirement: Ventana de 48 hs hábiles
«48 hs hábiles» SHALL significar 2 días hábiles completos inmediatamente anteriores al día local del `inicio` del turno en `America/Argentina/Buenos_Aires`, omitiendo sábados, domingos y las fechas del catálogo de feriados. El vencimiento SHALL ser `23:59:59` del segundo día hábil anterior, y la cancelación o reprogramación está dentro de ventana si `now <= vencimiento`: el paciente puede hacerla por link hasta 2 días hábiles antes del turno y, después, la hace recepción (RN-AG-05; decisión del 2026-11-25, `docs/decisiones/C-09-sentido-ventana-48hs.md`). La confirmación de asistencia no está sujeta a la ventana.

#### Scenario: Dentro de ventana
- **GIVEN** un turno con inicio a más de 2 días hábiles y una cancelación anterior al vencimiento
- **WHEN** se solicita cancelar o reprogramar
- **THEN** la operación se acepta

#### Scenario: Fines de semana y feriados
- **GIVEN** un turno con inicio posterior a un fin de semana o feriado cargado
- **WHEN** se calcula la ventana
- **THEN** sábados, domingos y feriados no cuentan como días hábiles

#### Scenario: Límite inclusive y fuera de rango
- **GIVEN** una operación exactamente en `23:59:59` del segundo día hábil anterior y otra 1 segundo después
- **WHEN** se evalúan ambas
- **THEN** la primera está dentro de ventana y la segunda se rechaza

### Requirement: Confirmación pública
El sistema SHALL cambiar el `estado` del turno a `confirmado` al recibir un `GESTIONAR` vigente antes del `inicio` del turno, sin depender de la ventana de 48 hs, sin modificar el hueco ni la ocupación, y SHALL emitir el evento de confirmación. Si el turno ya empezó, MUST responder `409` con `derivarARecepcion=true`. Una operación rechazada MUST NOT cambiar el estado ni emitir evento.

#### Scenario: Confirmación exitosa
- **GIVEN** un turno `reservado` y un token `GESTIONAR` vigente
- **WHEN** se confirma el turno
- **THEN** el estado pasa a `confirmado`, el horario no cambia y se emite `turno.confirmado`

#### Scenario: Confirmación rechazada
- **GIVEN** un token vencido, un turno ajeno al claim o un turno que ya empezó
- **WHEN** se intenta confirmar
- **THEN** el turno conserva su estado y no se emite evento

### Requirement: Cancelación pública y liberación del hueco
El sistema SHALL cancelar el turno dentro de ventana marcando `estado=cancelado`, `activo=false` y desactivando su ocupación en la misma transacción; el hueco SHALL quedar libre y visible de inmediato. Fuera de ventana, MUST responder `409` con `derivarARecepcion=true` sin liberar el hueco, cambiar el estado ni emitir evento de cancelación.

#### Scenario: Cancelación dentro de ventana
- **GIVEN** un turno activo y un token `GESTIONAR` vigente
- **WHEN** se cancela dentro de la ventana
- **THEN** turno y ocupación quedan inactivos en una transacción, el hueco vuelve a ofrecerse y se emite `turno.cancelado`

#### Scenario: Cancelación fuera de ventana
- **GIVEN** un turno cuyo inicio está a menos de 2 días hábiles
- **WHEN** se solicita cancelar
- **THEN** se responde `409` con `derivarARecepcion=true`, el turno sigue activo y el hueco no se libera

### Requirement: Reprogramación pública
El sistema SHALL reprogramar el turno dentro de ventana ocupando el nuevo hueco y liberando el original en una sola transacción, conservando `estado` y `origen`. El nuevo intervalo MUST respetar horarios y feriados y MUST NOT solapar; si no está libre, SHALL aplicarse la respuesta de hueco tomado.

#### Scenario: Reprogramación exitosa
- **GIVEN** un turno activo, un token `GESTIONAR` vigente y un hueco libre válido
- **WHEN** se reprograma
- **THEN** el hueco original se libera, el nuevo queda ocupado, el estado se conserva y se emite `turno.reprogramado`

#### Scenario: Destino ocupado
- **GIVEN** un turno activo y un nuevo hueco ya ocupado
- **WHEN** se intenta reprogramar
- **THEN** se responde conflicto con alternativas, el turno original permanece intacto y no se emite evento

### Requirement: Solicitud de notificación simulada
Tras crear, confirmar, cancelar o reprogramar, el sistema SHALL registrar una solicitud de notificación al paciente mediante un puerto con implementación simulada en dev/test. El sistema MUST NOT enviar mensajes reales, invocar WhatsApp ni afirmar entrega en este change; el adaptador real queda para C-10.

#### Scenario: Solicitud registrada
- **GIVEN** una operación pública confirmada
- **WHEN** se inspecciona el resultado de la operación
- **THEN** existe una solicitud de notificación simulada ligada al turno, sin prueba de entrega

#### Scenario: Sin envío real
- **GIVEN** la implementación del puerto en este change
- **WHEN** ocurre cualquier operación pública
- **THEN** no se llama a WhatsApp ni a otro proveedor externo y no se registra un evento de auditoría de entrega
