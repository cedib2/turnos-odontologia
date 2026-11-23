# agenda Specification

## Purpose

Define el comportamiento observable de la agenda operativa: turnos con duración variable, bloqueos, rechazo de solapamientos ante escrituras concurrentes, sobreturnos auditados y consulta día/semana por profesional y sillón.

## Requirements

### Requirement: Creación de turno con duración variable
El sistema SHALL crear un turno con `inicio`, `fin` y `duracion_minutos` coherentes, derivando la duración de la configuración de tratamiento vigente (C3=b, RN-AG-02). La duración MUST ser positiva. Los valores concretos por tratamiento permanecen `(TBD)` y no se inventan por convención.

#### Scenario: Turno con duración configurada
- **GIVEN** un tratamiento con duración configurable positiva y un hueco libre
- **WHEN** se crea un turno para ese tratamiento
- **THEN** el sistema persiste `fin = inicio + duracion_minutos` y el turno aparece en la agenda

#### Scenario: Duración no positiva
- **GIVEN** una solicitud de turno con duración menor o igual a cero
- **WHEN** se intenta crear el turno
- **THEN** el sistema rechaza la operación y no persiste ningún turno

### Requirement: Anti-solapamiento de profesional y sillón
El sistema MUST impedir que un turno o bloqueo solape otro turno o bloqueo del mismo profesional **y** del mismo sillón (RN-AG-01). La garantía SHALL existir en la capa de persistencia, no solo en la interfaz o en la validación de la aplicación.

#### Scenario: Solapamiento del mismo profesional
- **GIVEN** un turno ya reservado para un profesional en un intervalo
- **WHEN** se intenta crear otro turno para el mismo profesional en un intervalo que lo solapa
- **THEN** el sistema rechaza la creación y el turno existente permanece sin cambios

#### Scenario: Solapamiento del mismo sillón con profesional distinto
- **GIVEN** un turno ocupa un sillón en un intervalo
- **WHEN** se intenta crear un turno para otro profesional en ese mismo sillón e intervalo solapado
- **THEN** el sistema rechaza la creación

#### Scenario: Intervalos contiguos
- **GIVEN** un turno que termina exactamente cuando empieza otro
- **WHEN** se crean ambos turnos para el mismo profesional y sillón
- **THEN** el sistema los acepta porque no hay solapamiento

### Requirement: Escrituras concurrentes sobre el mismo hueco
El sistema MUST garantizar que, ante dos escrituras concurrentes que disputan el mismo hueco de profesional o sillón, como máximo una sea confirmada y la otra sea rechazada con un error de conflicto.

#### Scenario: Dos reservas simultáneas
- **GIVEN** dos solicitudes concurrentes para el mismo profesional, sillón e intervalo
- **WHEN** ambas transacciones intentan confirmarse
- **THEN** exactamente una persiste su turno y la otra recibe un rechazo por conflicto de agenda

### Requirement: Bloqueos que ocupan agenda
El sistema SHALL permitir bloqueos por profesional, por sillón o por ambos en un período, con motivo obligatorio. Un bloqueo MUST ocupar la agenda: impide crear turnos solapados y SHALL excluir ese período de la disponibilidad.

#### Scenario: Turno dentro de un bloqueo
- **GIVEN** un bloqueo activo sobre un sillón en un período
- **WHEN** se intenta crear un turno solapado en ese sillón
- **THEN** el sistema rechaza el turno

#### Scenario: Disponibilidad con bloqueo
- **GIVEN** un bloqueo que cubre parte de la jornada de un profesional
- **WHEN** se consulta la disponibilidad de ese profesional
- **THEN** el período bloqueado no se ofrece como hueco reservable

#### Scenario: Bloqueo sin profesional ni sillón
- **GIVEN** una solicitud de bloqueo sin profesional y sin sillón
- **WHEN** se intenta crear el bloqueo
- **THEN** el sistema lo rechaza por no identificar qué recurso ocupa

### Requirement: Sobreturno como excepción auditada
El sistema SHALL permitir un sobreturno solo como excepción explícita: el turno existente se desplaza a otro hueco libre en la misma operación, con `motivo` y `actor` obligatorios y un evento de auditoría emitido (C1=b, IN-04, RN-GL-01). La restricción anti-solapamiento MUST permanecer vigente; no se relaja por ser sobreturno.

#### Scenario: Sobreturno con desplazamiento atómico
- **GIVEN** un turno existente y un hueco libre alternativo para el mismo profesional
- **WHEN** se solicita un sobreturno con motivo y actor
- **THEN** el turno se desplaza al nuevo hueco, se marca como sobreturno y se emite un evento con el motivo y el actor

#### Scenario: Sobreturno sin override
- **GIVEN** un turno existente y un hueco ocupado
- **WHEN** se solicita un sobreturno sin motivo o sin actor
- **THEN** el sistema rechaza la operación y prevalece RN-AG-01

#### Scenario: Sobreturno sin destino libre
- **GIVEN** un turno existente y ningún hueco libre para desplazarlo
- **WHEN** se solicita un sobreturno con motivo y actor
- **THEN** el sistema rechaza la operación sin modificar el turno original

### Requirement: Reprogramación atómica
El sistema MUST reprogramar un turno liberando el hueco original y ocupando el nuevo en una sola operación. Si el nuevo hueco no está disponible, el turno original SHALL permanecer intacto.

#### Scenario: Reprogramación exitosa
- **GIVEN** un turno confirmado y un hueco libre
- **WHEN** se reprograma el turno a ese hueco
- **THEN** el hueco original queda libre, el nuevo queda ocupado y no existe solapamiento en ninguno

#### Scenario: Reprogramación a hueco ocupado
- **GIVEN** un turno confirmado y un hueco ya ocupado
- **WHEN** se intenta reprogramar a ese hueco
- **THEN** el sistema rechaza la operación y el turno original conserva su fecha, profesional y sillón

### Requirement: Write-back real a la agenda
El sistema SHALL reflejar de inmediato en la agenda interna cada creación, reprogramación, sobreturno y bloqueo confirmado (RN-AG-06). La agenda MUST ser la fuente de estado real, no un aviso descriptivo de una reserva externa.

#### Scenario: Cambio visible tras reprogramación
- **GIVEN** una agenda consultada por profesional
- **WHEN** se reprograma un turno de ese profesional
- **THEN** una nueva consulta muestra el turno solo en el nuevo horario y el hueco anterior disponible

### Requirement: Vista día/semana por profesional y sillón
El sistema SHALL exponer una vista de agenda por día y por semana, filtrable por profesional y por sillón, que incluya turnos y bloqueos (US-001). Los intervalos SHALL interpretarse en la zona horaria `America/Argentina/Buenos_Aires` (C2=a).

#### Scenario: Consulta diaria por sillón
- **GIVEN** turnos y bloqueos en un día para varios sillones
- **WHEN** se consulta la vista día de un sillón
- **THEN** se devuelven solo los turnos y bloqueos de ese sillón en ese día

#### Scenario: Consulta semanal por profesional
- **GIVEN** turnos repartidos en varias semanas
- **WHEN** se consulta la vista semana de un profesional
- **THEN** se devuelven solo los registros de esa semana para ese profesional

### Requirement: Catálogo mínimo de scheduling como slice previo
El sistema SHALL proveer la configuración mínima de tratamientos con duración, horarios laborales, feriados y zona horaria que la creación de turnos y la disponibilidad necesitan (C-07). En este change el sistema SHALL exponer en perfiles `dev`/`test` operaciones administrativas (CRUD) para `tratamientos`, `horarios` y `feriados` sobre las tablas existentes de V005, sin nuevas migraciones ni seeds. La zona horaria de negocio sigue siendo `America/Argentina/Buenos_Aires` (C2=a). Los valores concretos de seed de duraciones, las reglas de calendario nacional vs. provincial, la anticipación mínima/máxima, el solapamiento de franjas, permisos/RBAC, autenticación y auditoría persistente permanecen `(TBD)` y fuera de este change. `ConfiguracionSchedulingService` y el comportamiento de lectura usado por agenda/disponibilidad MUST mantener su semántica de C-08 (no se filtra por `activo`, no se interpreta `aplica_a_todos` en este change).

#### Scenario: Disponibilidad fuera de horario o en feriado
- **GIVEN** un feriado configurado o un horario laboral que no cubre un intervalo
- **WHEN** se consulta la disponibilidad para ese intervalo
- **THEN** el sistema no lo ofrece como hueco reservable

#### Scenario: CRUD de tratamientos disponible en dev/test
- **GIVEN** el perfil `dev` o `test` activo
- **WHEN** se invoca la API indicativa de tratamientos para crear, listar, consultar, actualizar o eliminar un tratamiento
- **THEN** el sistema valida entradas básicas (código no vacío y único, `duracionMinutos` > 0, campos requeridos según la entidad) y persiste los cambios en las tablas de V005 sin requerir migraciones nuevas

#### Scenario: CRUD de horarios y feriados disponible en dev/test
- **GIVEN** el perfil `dev` o `test` activo
- **WHEN** se invoca la API indicativa de horarios o feriados
- **THEN** el sistema permite gestionar horarios con `profesionalId` opcional (global o por profesional), `diaSemana` entre 1 y 7 y `desde` anterior a `hasta`, y feriados con `fecha` única y `descripcion`; cualquier regla adicional no decidida permanece `(TBD)`

#### Scenario: Sin efecto en lectura de disponibilidad de C-08
- **GIVEN** datos de catálogo cargados mediante el CRUD de este change
- **WHEN** se consulta disponibilidad o se crean turnos usando `ConfiguracionSchedulingService` como en C-08
- **THEN** el comportamiento observable de esas lecturas no cambia en este change (se mantiene la semántica vigente)

### Requirement: Superficie HTTP indicativa solo de dev/test
El sistema SHALL exponer operaciones de agenda bajo las rutas indicativas `/agenda`, `/turnos`, `/bloqueos` y `/sobreturnos` únicamente en perfiles dev/test. En el mismo ámbito (perfiles `dev`/`test`) el sistema MAY exponer rutas indicativas adicionales para el CRUD del catálogo de scheduling (`/tratamientos`, `/horarios`, `/feriados`). Estas rutas MUST NOT incluir autenticación, RBAC, headers, tokens, cookies ni rutas `/auth/*`; el actor se obtiene del proveedor simulado cuando una operación lo requiera.

#### Scenario: Operación en perfil dev/test
- **GIVEN** el perfil dev/test activo
- **WHEN** se consulta `/agenda` con filtros de profesional y día
- **THEN** el sistema devuelve la vista correspondiente usando el actor simulado

#### Scenario: Sin superficie de autenticación
- **GIVEN** la superficie HTTP de este change
- **WHEN** se inspeccionan sus rutas
- **THEN** no existe ninguna ruta de autenticación, token, cookie ni cabecera de identidad

#### Scenario: Rutas de catálogo en dev/test sin autenticación
- **GIVEN** el perfil dev/test activo
- **WHEN** se invocan rutas indicativas de catálogo (`/tratamientos`, `/horarios`, `/feriados`)
- **THEN** el sistema no exige autenticación ni RBAC y no expone rutas `/auth/*` asociados a este CRUD

### Requirement: Campos semánticos sin enum inventado
El sistema SHALL almacenar `estado` y `origen` del turno como cadenas no vacías. Sus valores semánticos y los del seed de duraciones permanecen `(TBD)`; el sistema MUST NOT fijarlos por convención (RN-GL-03).

#### Scenario: Turno sin estado definido por convención
- **GIVEN** una solicitud válida de creación de turno
- **WHEN** el sistema persiste el turno
- **THEN** `estado` y `origen` contienen valores no vacíos cuya semántica exacta queda registrada como `(TBD)`

### Requirement: Aislamiento de la superficie autenticada
El sistema SHALL mantener `/agenda`, `/turnos`, `/bloqueos`, `/sobreturnos` y catálogos sin autenticación, RBAC ni lectura de identidad en dev/test, mientras `/auth/**` pertenece al paquete de autenticación. La cadena SHALL restringirse a `/auth/**` y MUST NOT proteger estas rutas. C-04 entrega infraestructura RBAC pero aún NO protege la agenda: su protección queda para un change posterior.

#### Scenario: Agenda y catálogos siguen sin autenticación
- **GIVEN** el perfil dev/test activo
- **WHEN** se invocan rutas de agenda o catálogo
- **THEN** responden sin credenciales, sin cabeceras de identidad y con el mismo actor simulado de C-08

#### Scenario: Sin rutas de autenticación en la superficie de agenda
- **GIVEN** los controladores de agenda y catálogo
- **WHEN** se inspeccionan sus mappings y anotaciones
- **THEN** no exponen `/auth/**`, tokens, cookies ni lectura de identidad; esas rutas viven solo en el paquete de autenticación

#### Scenario: Protección de agenda diferida
- **GIVEN** el alcance declarado de C-04 en este delta y en el documento de decisiones
- **WHEN** se inspecciona la declaración de alcance
- **THEN** declara que C-04 entrega infraestructura RBAC pero aún no protege `/agenda`, `/turnos`, `/bloqueos`, `/sobreturnos` ni catálogos, cuya protección queda para un change posterior

#### Scenario: Delta puramente aditivo
- **GIVEN** los 11 requisitos y todos los escenarios actuales de `agenda`
- **WHEN** se inspecciona este delta
- **THEN** permanecen intactos: no hay `MODIFIED` ni `REMOVED`, solo este requisito `ADDED`

### Requirement: Superficie pública con token sin credenciales de staff
La superficie pública que consuma tokens de `public-access-tokens` SHALL validar el token sin credenciales de staff, sin `Usuario`/`Rol` de pacientes y sin leer request headers de identidad. Este requisito NO modifica la superficie dev/test de `/agenda`, `/turnos`, `/bloqueos`, `/sobreturnos` ni catálogos, que conserva su semántica actual; los endpoints de negocio públicos se introducen en C-09 y C-13.

#### Scenario: Consumo público sin credenciales de staff
- **GIVEN** un token vigente de C-06 presentado en path o query y ninguna credencial de staff
- **WHEN** la superficie pública valida el token para una operación de reserva o historia
- **THEN** la validación se resuelve por el token sin exigir login, RBAC ni request headers de identidad, y los controladores existentes de agenda y catálogos permanecen sin cambios

#### Scenario: Delta aditivo sin debilitar guardias
- **GIVEN** los 12 requisitos y todos los escenarios actuales de `agenda` y las guardias de C-04
- **WHEN** se inspecciona este delta
- **THEN** permanecen intactos: no hay `MODIFIED` ni `REMOVED`, `AgendaSurfaceTest` y `CurrentActorProviderTest` no se ajustan y `audit-event-emission` sigue en modo `SIMULADO`

### Requirement: Ciclo de vida público del turno en la agenda
Para turnos creados desde la superficie pública, el sistema SHALL persistir `estado=reservado` y `origen=link` al crear; la confirmación SHALL cambiar solo `estado` a `confirmado`; la cancelación dentro de ventana SHALL fijar `estado=cancelado` y `activo=false` y desactivar su ocupación en la misma transacción; la reprogramación SHALL conservar `estado` y `origen`. Valores fijados por decisión del PO de C-09 (RN-GL-03); otros orígenes siguen `(TBD)`.

#### Scenario: Alta pública visible en agenda
- **GIVEN** una reserva pública creada con token `RESERVAR`
- **WHEN** se consulta la agenda del profesional
- **THEN** el turno aparece con `estado=reservado`, `origen=link` y su ocupación activa

#### Scenario: Confirmación sin mover el hueco
- **GIVEN** un turno público `reservado`
- **WHEN** se confirma con token `GESTIONAR`
- **THEN** solo cambia su `estado` a `confirmado` y conserva inicio, fin, profesional y sillón

#### Scenario: Cancelación que libera el hueco
- **GIVEN** un turno público activo dentro de la ventana de 48 hs hábiles
- **WHEN** se cancela
- **THEN** quedan inactivos el turno y su ocupación en la misma transacción y el intervalo vuelve a ofrecerse como libre

#### Scenario: Reprogramación que conserva la semántica pública
- **GIVEN** un turno público confirmado o reservado
- **WHEN** se reprograma dentro de ventana
- **THEN** mantiene su `estado` y `origen`, libera el hueco anterior y ocupa el nuevo sin solapamiento

#### Scenario: Delta puramente aditivo
- **GIVEN** los 13 requisitos y todos los escenarios actuales de `agenda`
- **WHEN** se inspecciona este delta
- **THEN** permanecen intactos: no hay `MODIFIED` ni `REMOVED` y las guardias de C-04 y C-06 siguen vigentes

### Requirement: Adjudicación de hueco liberado desde la waitlist

El sistema SHALL permitir reocupar un turno cancelado cuya ocupación esté desactivada mediante el motor de waitlist, creando la ocupación por el camino de servicio ya existente y validando disponibilidad con las restricciones de solapamiento vigentes. Una misma liberación SHALL adjudicarse como máximo una vez; un reenvío o segunda adjudicación MUST NOT crear otra ocupación ni otro turno. Queda prohibido insertar directamente en `turnos`, `bloqueos` u `ocupaciones`.

#### Scenario: Adjudicación de un hueco liberado

- **GIVEN** un turno cancelado con su ocupación desactivada y un candidato elegible en la waitlist
- **WHEN** se adjudica el hueco liberado
- **THEN** se crea una sola ocupación activa para el candidato y el turno queda confirmado en la agenda

#### Scenario: Reenvío sin doble adjudicación

- **GIVEN** un hueco ya adjudicado a un candidato
- **WHEN** se repite la adjudicación con la misma clave
- **THEN** no se crea otra ocupación ni otro turno y la operación se rechaza sin efectos

#### Scenario: Conflicto de solapamiento sin efecto

- **GIVEN** un hueco liberado que vuelve a estar ocupado por otra operación
- **WHEN** se intenta adjudicar al candidato
- **THEN** la operación se rechaza, no se escribe en la agenda y no se emite evento de adjudicación
