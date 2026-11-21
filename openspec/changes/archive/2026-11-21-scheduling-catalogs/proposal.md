# Proposal

## Why

El catálogo mínimo de scheduling (tratamientos con duración, horarios laborales, feriados y zona horaria de negocio) ya existe como tablas y acceso de lectura para turnos y disponibilidad (C-08, V005), pero no hay forma administrativa de gestionarlo en este slice. C-07 completa ese slice previo agregando CRUD en el backend para cargar y mantener esos datos, sin imponer decisiones aún no tomadas (duraciones, seeds, calendarios, permisos, auditoría persistente ni UX).

## What Changes

- Agregar controladores HTTP indicativos, solo perfiles `dev`/`test`, para:
  - Tratamientos (`/tratamientos`): crear/listar/consultar/actualizar/eliminar (o desactivar) con validaciones de entrada básicas coherentes con la entidad existente (`codigo` único, `duracionMinutos > 0`, campos requeridos).
  - Horarios de atención (`/horarios`): CRUD con `profesionalId` opcional (global o por profesional), `diaSemana` 1..7, `desde < hasta`, sin reglas extra de solapamiento ni negocio no decidido `(TBD)`.
  - Feriados (`/feriados`): CRUD con `fecha` única y `descripcion`; `aplica_a_todos` se expone como campo presente pero sin interpretación nueva en este change `(TBD)`.
- Reutilizar entidades, repositorios y la capa de servicios existente (`ConfiguracionSchedulingService`) para lecturas donde aplique; no cambiar el comportamiento de lectura usado por agenda/disponibilidad en C-08 (incluye no filtrar por `activo`, no interpretar `aplica_a_todos`).
- Validaciones de dominio/entrada cubiertas por pruebas unitarias (H2) y pruebas de integración PostgreSQL/Testcontainers para constraints reales (unicidad `codigo`/`fecha`, FKs/longitudes según esquema actual) cuando corresponda.
- **Sin** nuevas migraciones Flyway (se usan tablas de V005 tal cual). **Sin** seeds. **Sin** autenticación, RBAC, headers/tokens/cookies ni rutas `/auth/*` (superficie dev/test con actor simulado existente si se necesita contexto). **Sin** auditoría persistente ni eventos nuevos. **Sin** UI ni artefactos de `design-system/`.

## Capabilities

### New Capabilities
- Ninguna. No se introduce una nueva capacidad de dominio; se completa el CRUD del slice previo de `agenda` ya definido en specs.

### Modified Capabilities
- `agenda`: modificar el requisito “Catálogo mínimo de scheduling como slice previo” para que pase de describir solo la configuración mínima leída por turnos/disponibilidad a incluir el CRUD administrativo de ese catálogo en superficie dev/test, manteniendo explícitos los diferimientos (`(TBD)`) para duraciones/semillas, reglas de solapamiento de franjas, calendario y permisos/auditoría. También ajustar el requisito “Campos semánticos sin enum inventado” solo si se requiere aclarar que los valores nuevos de `estado/origen` de turno no se ven afectados (sin cambios semánticos); en principio no es necesario modificarlo.

## Impact

- Código: `src/main/java/com/turnosodontologia/interfaces/web/*` (nuevos controladores/DTOs), posiblemente `application/*` (servicios CRUD finos) si se prefiere no llamar repos desde controladores; `src/test/java/...` (unitarios e integración). No se tocan migraciones en `src/main/resources/db/migration`.
- APIs indicativas (dev/test): `GET/POST/PUT/PATCH/DELETE` (según se defina en design/tasks) bajo `/tratamientos`, `/horarios`, `/feriados`.
- Dependencias/sistemas: sin nuevas dependencias externas; reutiliza Spring Data JPA + Flyway existentes.
- Fuera de alcance: C-04 (RBAC), C-05 (auditoría persistente), C-06 (notificaciones/QR), cambios en `ConfiguracionSchedulingService`, seguridad HTTP real.