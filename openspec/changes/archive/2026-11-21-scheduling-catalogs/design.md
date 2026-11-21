# Design

## Context

C-08 (archivado) dejó en V005 las tablas `tratamientos`, `horarios` y `feriados` con entidades/repos y `ConfiguracionSchedulingService` para lecturas usadas por agenda/disponibilidad. Aún falta el CRUD administrativo de ese catálogo en este slice. Ver `proposal.md` (motivo y alcance). No se tocan migraciones, C-04/C-05/C-06 ni el comportamiento de lectura vigente.

## Goals / Non-Goals

**Goals:**
- Exponer CRUD HTTP indicativo en perfiles `dev`/`test` para `tratamientos`, `horarios`, `feriados` sobre el esquema V005 (sin V007/V008 nuevos).
- Mantener validaciones alineadas al dominio actual (`Tratamiento`, `HorarioAtencion`, `Feriado`) y cubrir con tests unitarios (H2) y de integración PostgreSQL/Testcontainers para constraints reales.
- Mantener explícitos `(TBD)` para decisiones no tomadas (duraciones/seed, calendario, anticipación, solapamiento de franjas, permisos, auditoría persistente, interpretación de `aplica_a_todos`).

**Non-Goals:**
- Autenticación, RBAC, headers/tokens/cookies ni rutas `/auth/*` (queda en C-04).
- Auditoría persistente o nuevos eventos de auditoría (queda en C-05).
- Cambios en `ConfiguracionSchedulingService` (no filtrar `activo`, no usar `aplica_a_todos`, no cambiar frescura/caché más allá de lectura directa existente).
- UI, `design-system/`, branding ni mockups (`visual-design=deferred`, `Decision: no (no UI)`).
- Seeds de datos; `Migration005Test.java:38` continúa asumiendo `COUNT(tratamientos)==0`.
- Solución de solapamiento de franjas de horarios, calendarios provinciales/nacionales, valores de negocio.

## Decisions

- **Sin nueva migración (Orquestador, 2026-11-21).** C-07 solo CRUD sobre V005; no se edita V005 ni se agrega V007. Si más adelante llegan valores/ajustes, se hará otro change con migración explícita.
- **Sin seeds (Orquestador, 2026-11-21).** No insertar datos de ejemplo; tests de migración existentes no se modifican.
- **Sin cambios en `ConfiguracionSchedulingService` (Orquestador, 2026-11-21).** Se evita alterar consumidores de C-08; cualquier filtro por `activo` o interpretación de `aplica_a_todos` queda `(TBD)`.
- **Superficie HTTP solo `dev`/`test` (Orquestador, 2026-11-21).** Rutas indicativas propuestas: `GET/POST /tratamientos`, `GET/PUT/PATCH/DELETE /tratamientos/{id}`; `GET/POST /horarios`, `GET/PUT/PATCH/DELETE /horarios/{id}`; `GET/POST /feriados`, `GET/PUT/PATCH/DELETE /feriados/{id}`. Controladores con `@RestController` y `@Profile({"dev","test"})`, siguiendo el patrón de `BloqueoController`. Sin headers/tokens/cookies.
- **Capa de aplicación opcional.** Se puede reutilizar repositorios directamente desde controladores si se mantiene simple; si aparecen reglas de orquestación, introducir servicios en `application` sin mezclar con `ConfiguracionSchedulingService`. Alternativa descartada: meter lógica CRUD en el servicio de configuración (confunde lecturas de disponibilidad con escrituras administrativas).
- **Validación.** Bean Validation en DTOs donde sea directo (`@NotBlank`, `@Positive`, etc.) y/o validaciones en entidades/servicios existentes; los mensajes de error se definen en `design.md`/tareas pero se prioriza consistencia con respuestas actuales del proyecto (sin inventar un formato global nuevo salvo lo mínimo necesario).
- **Concurrencia.** No se requiere lógica nueva de concurrencia en C-07; se confía en constraints de BD (unicidad). Tests de concurrencia siguen prohibidos con H2 (y no aplican aquí).

## Risks / Trade-offs

- **`[CRUD amplio sin permisos]` → Mitigación:** limitar a `dev`/`test`, documentar en specs/proposal que permisos/RBAC están en C-04. No exponer en perfiles productivos por defecto.
- **`[Semántica de `activo`/`aplica_a_todos` ambigua]` → Mitigación:** no usarlos en `ConfiguracionSchedulingService` ahora; dejar `(TBD)` en specs y no agregar filtros implícitos que rompan C-08.
- **`[Inconsistencia de forma de error HTTP]` → Mitigación:** reutilizar patrones existentes de controladores/`GlobalExceptionHandler` si existen; si no, mantener respuestas simples y documentar en tareas (sin agregar framework nuevo).
- **`[Cambios futuros en V005]` → Mitigación:** prohibido tocar V005 en este change; cualquier ajuste requiere nuevo change + migración.