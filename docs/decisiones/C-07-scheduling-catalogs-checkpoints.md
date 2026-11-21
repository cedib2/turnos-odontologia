# C-07 — Checkpoints BAJO: `scheduling-catalogs`

- **Change**: `scheduling-catalogs` (OpenSpec, schema `spec-driven`)
- **Governance**: BAJO
- **Fecha**: 2026-11-21
- **Decisor**: Orquestador por delegación del PO (governance BAJO)
- **Alcance registrado**: CRUD administrativo del catálogo de scheduling sobre V005; cierre técnico y archive.

## Decisiones

- **Sin V007 ni seeds.** No se agregó ni editó ninguna migración: `V005__scheduling_catalogs.sql` sigue intacta y `Migration005Test.java:38` continúa asumiendo `COUNT(tratamientos)==0`. No se insertaron datos de ejemplo; duraciones/calendario/anticipación permanecen `(TBD)`.
- **Sin cambios en `ConfiguracionSchedulingService`.** No se filtra por `activo`, no se interpreta `aplica_a_todos` y no cambia la semántica de lectura usada por agenda/disponibilidad en C-08. Los controladores delegan en `CatalogoSchedulingService`, separado del servicio de configuración.
- **Superficie dev/test sin autenticación.** Rutas indicativas `/tratamientos`, `/horarios` y `/feriados` con `@Profile({"dev","test"})`; sin auth, RBAC, headers, tokens, cookies ni rutas `/auth/*`. Queda para C-04/C-05/C-06.
- **Sin UI.** `Decision: no (no UI)`; no se crearon `design-system/`, branding ni mockups (`visual-design=deferred`).
- **Sincronización de specs en archive.** Política aplicada: sincronizar specs (igual que C-08). Los dos requisitos `MODIFIED` del delta quedaron reflejados en `openspec/specs/agenda/spec.md`; no hay capabilities nuevas y `audit-event-emission` no se tocó.

## Evidencia de verify

- `mvn -q test`: GREEN, 93/93 tests, `Failures: 0, Errors: 0, Skipped: 0`; PostgreSQL 17.12 vía Testcontainers/Docker Desktop (29.9.3) disponible durante toda la corrida. No se usó H2 para concurrencia.
- Cobertura del delta: 7/7 escenarios (4 del requisito de catálogo + 3 de superficie HTTP); las tres suites de catálogo y `CatalogoLecturaC08Test` verification la semántica de lectura de C-08.
- `openspec validate scheduling-catalogs --strict`: `Change 'scheduling-catalogs' is valid`.
- `openspec status --change "scheduling-catalogs" --json`: cuatro artefactos `done`, `isComplete: true`; `openspec list --json`: 10/10 checkboxes, sin hallazgos blocker/major.
- Sin commits, sin push, sin secretos; C-04, C-05, C-06, C-08 y los changes de los compañeros quedan intactos.

## Archive

- **Fecha**: 2026-11-21
- **Resultado**: change movido a `openspec/changes/archive/2026-11-21-scheduling-catalogs/`; `CHANGES.md` registra C-07 como `[x]` con verificación técnica pasada. C-08 conserva su nota acordada en `[ ]`.
- **Consecuencia**: C-08 ya puede apoyarse en el CRUD de catálogo; la auditoría persistente y el RBAC siguen pendientes de C-05 y C-04, respectivamente. No se afirma despliegue ni cumplimiento.
