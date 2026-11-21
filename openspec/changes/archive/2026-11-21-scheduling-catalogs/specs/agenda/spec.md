# Spec Delta

## MODIFIED Requirements

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