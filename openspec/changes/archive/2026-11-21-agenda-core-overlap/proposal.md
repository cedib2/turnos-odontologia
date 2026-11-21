# Proposal

## Why

C-03 entregó los modelos base (paciente, profesional, consultorio, sillón, usuario/rol/permiso) pero dejó fuera `Turno` y `Bloqueo`; sin agenda no existe la capacidad central del producto ni se puede cumplir RN-AG-01. El problema operativo que motiva el MVP es la doble reserva: dos escrituras concurrentes pueden ocupar el mismo hueco de un profesional o de un sillón si la regla vive solo en la interfaz (B2, DD-04, RN-AG-01). Este change implementa el núcleo de agenda con la restricción en la capa de datos, de modo que los changes posteriores de reserva pública (C-09) y notificaciones (C-10) apoyen sobre una agenda que ya rechaza solapamientos.

C-04 (autenticación/RBAC) y C-05 (auditoría append-only) se difieren por decisión del PO: aquí se introducen puertos `CurrentActorProvider` y `AuditEventPublisher` con implementaciones simuladas solo de dev/test. En consecuencia este change **no** es desplegable a producción ni acredita cumplimiento; la resolución de RN-GL-01 se registra como evento emitido, no como evidencia de auditoría.

## What Changes

- **Migración 006 (agenda)**: tablas `turnos` y `bloqueos`, relación con feriados, índices `(profesional_id, inicio, fin)`, `(sillon_id, inicio, fin)`, `estado` y `token_publico`. Columnas `estado (TBD)` y `origen (TBD)` como `VARCHAR` no vacío, sin enum inventado; `es_sobreturno` y `token_publico` según la ficha C-08. Sin borrado físico (D5).
- **Anti-solapamiento en base de datos (RN-AG-01)**: `btree_gist` + tabla de ocupación para cubrir profesional y sillón, incluyendo la relación turno ↔ bloqueo; requiere permiso de extensión en PostgreSQL ya aprobado (D1). Rechazo de dos escrituras concurrentes sobre el mismo hueco.
- **Duración variable (RN-AG-02, C3=b)**: el turno persiste `duracion_minutos` e `inicio`/`fin` coherentes, calculados desde la configuración de tratamiento del slice C-07. Los valores de duración siguen `(TBD)`.
- **Bloqueos (RN-AG-03)**: creación de bloqueos por profesional/sillón/período que ocupan agenda y se respetan en disponibilidad.
- **Sobreturno auditado (C1=b, IN-04, RN-GL-01)**: desplazamiento atómico del turno existente en la misma transacción, con `motivo` y `actor` requeridos; prevalece RN-AG-01 salvo override explícito. No se relaja la restricción de la BD.
- **Reprogramación y write-back interno (RN-AG-06)**: mover un turno valida disponibilidad y aplica de nuevo anti-solapamiento en una sola transacción; la agenda refleja el cambio real sin convertirlo en un aviso.
- **Vista día/semana (US-001)**: consulta de agenda por profesional y por sillón, con turnos y bloqueos.
- **Endpoints indicativos solo dev/test**: `/agenda`, `/turnos`, `/bloqueos`, `/sobreturnos`. No se expone autenticación, RBAC, headers, tokens, cookies ni rutas `/auth/*`.
- **C-07 (parcial)**: slice real previo con migración 005 (`tratamientos`, `feriados`, `horarios`, duración configurable, zona horaria `America/Argentina/Buenos_Aires` según C2=a). El CRUD administrativo completo de C-07 y los seeds `(TBD)` quedan para su change.
- **Puertos de sustitución**: `CurrentActorProvider` devuelve un actor fijo (`recepcionista`/`system`) solo en dev/test; `AuditEventPublisher` emite a un recorder en memoria/no-op, sin tabla append-only, hash, retención ni API de consulta.
- **Pruebas**: concurrencia contra PostgreSQL real (H2 solo para unitarios), bloqueos, reprogramación atómica, sobreturno con override y emisión de eventos.

No hay cambios BREAKING: no existe API de agenda previa. Ningún valor `(TBD)` se resuelve por convención (RN-GL-03).

## Capabilities

### New Capabilities
- `agenda`: propiedad de turnos y bloqueos, duración variable, ocupación anti-solapamiento de profesional y sillón, sobreturno con override, reprogramación atómica, write-back interno, vista día/semana y superficie HTTP indicativa de dev/test.
- `audit-event-emission`: contrato del evento de auditoría emitido por operaciones de agenda (tipo, entidad/id, actor con modo simulado, `occurredAt`, motivo/metadata y `correlationId`), sin persistencia ni valor de cumplimiento.

### Modified Capabilities
- Ninguna. `openspec list --specs` no reporta spec actual; no hay requerimientos existentes que cambiar.

## Impact

- Código: `src/main/java/com/turnosodontologia/domain/agenda/`, `application/`, `infrastructure/persistence/`, `interfaces/`; reutilización de `AuditableEntity` (`created_by`/`updated_by` son `String`, compatibles con el actor simulado).
- Esquema: `src/main/resources/db/migration/V005__scheduling_catalogs.sql` (slice parcial) y `V006__agenda_core.sql` (incluye extensión `btree_gist` y tabla de ocupación).
- Dependencias: PostgreSQL 17 con fallback 16 (D1); extensión `btree_gist` habilitada; dependencia/servicio de PostgreSQL para tests de concurrencia aprobado; H2 restringido a tests unitarios.
- Artefactos de planeación: `CHANGES.md:277` y `:304` ya enmendados con el desacoplamiento de C-04/C-05 y la restricción de despliegue; `C-08` sigue pendiente hasta verificación técnica.
- Fuera de alcance: C-04, C-05, CRUD admin completo de C-07, token público de C-06, WhatsApp (C-10), historia clínica (C-11), UI/capa visual, despliegue a producción y evidencia de cumplimiento.
