# Design

## Context

Ver `proposal.md` para la motivación. Estado actual verificado: Java 17, Spring Boot 3.5.16, Spring Data JPA, Flyway, PostgreSQL 17 con fallback 16 (D1), H2 solo en tests, paquete `com.turnosodontologia` (C-02). C-03 entregó `AuditableEntity` (`created_by`/`updated_by` son `String`), entidades base, repositorios y la migración `V001__core_models.sql`; `Turno` y `Bloqueo` quedaron fuera de alcance (D10). No existe spec `openspec/specs/` previa.

Restricciones vigentes: consultorio único (A2/D3), snake_case plural y mapeo explícito (D4), sin borrado físico (D5), repositorios por agregado con `@Transactional` en servicios (D6), permisos planos sin herencia (D8), RN-AG-01 en C-08 (D10), `America/Argentina/Buenos_Aires` y feriados nacionales (C2=a), duración configurable por tratamiento (C3=b), sobreturno auditado (C1=b). C-04 y C-05 están diferidos por aprobación del PO (`CHANGES.md:277`, `:304`).

Las skills de dominio sugeridas por el ecosistema (`domain-modeling`, `codebase-design`, `web-security-review`, `test-driven-development`) están **NO INSTALADAS** según `.atl/skill-registry.md`; no se invocan ni se generan reglas compactas para ellas. La base de este diseño es la KB, las decisiones aprobadas y el código existente.

## Goals / Non-Goals

**Goals:**
- Hacer cumplir RN-AG-01 en la capa de datos, incluida la coexistencia turno ↔ bloqueo y escrituras concurrentes.
- Proveer operaciones de agenda coherentes y atómicas: alta con duración variable, bloqueos, sobreturno con override, reprogramación y vista día/semana.
- Aislar actor y emisión de auditoría detrás de puertos explícitos, con implementaciones simuladas claramente marcadas.
- Verificar el comportamiento crítico con PostgreSQL real, dejando H2 para tests unitarios.

**Non-Goals:**
- Autenticación, RBAC, tokens, cookies, headers de identidad, secretos ni rutas `/auth/*` (C-04).
- Log append-only, hash encadenado, retención, exportación ni API de consulta de auditoría (C-05).
- CRUD administrativo completo de C-07, link/QR público (C-06), WhatsApp (C-10), historia clínica (C-11), waitlist (C-19).
- Ventana de 48 hs hábiles aplicada a cancelaciones del paciente: corresponde a C-09; aquí solo se consume el calendario de C-07 para disponibilidad.
- Despliegue a producción ni acreditación de cumplimiento; UI, branding o mockups.

## UI / design-system

`Decision: no (no UI)`. El change es backend/API-only y `design-system` está `deferred`; no se crean `design-system/`, branding ni mockups.

## Decisions

### D-1 — Slice real de C-07 antes que un stub
Se crea la migración 005 con `tratamientos` (duración configurable positiva), `horarios`, `feriados` y zona horaria `America/Argentina/Buenos_Aires`, más un servicio de consulta de configuración. Alternativas: (a) stub de duración fija — descartada porque contradice C3=b y RN-AG-02; (b) ejecutar C-07 completo — descartada por alcance (CRUD admin y seeds `(TBD)` siguen en su change). Este change es dueño de la migración 005; C-07 no la reedita.

### D-2 — RN-AG-01 con `btree_gist` + tabla de ocupación
Se habilita la extensión `btree_gist` (permiso aprobado) y se usa una exclusion constraint sobre rangos `tstzrange` para cada recurso, más una tabla de ocupación que materializa los intervalos de turnos y bloqueos. Una sola exclusion constraint por tabla no cubre el solapamiento cruzado turno ↔ bloqueo; por eso la tabla de ocupación es la frontera común. Descartadas: validación solo en UI (DD-04), *advisory locks* como mecanismo único (no dejan restricción declarativa persistente), y H2 como motor de verificación (no soporta exclusion constraints de PostgreSQL).

### D-3 — Sobreturno = desplazamiento atómico con override
Ante un conflicto, el caso de uso desplaza el turno existente a un hueco libre dentro de la misma transacción, exigiendo `motivo` y `actor`, y emite el evento de override. La restricción de la BD no se relaja ni se desactiva: el hueco destino se valida con la misma regla. Alternativas descartadas: permitir solapamiento directo (rompe RN-AG-01) y tratar todo sobreturno como turno en otro recurso (no cubre la práctica declarada en IN-04/C1=b).

### D-4 — Puertos `CurrentActorProvider` y `AuditEventPublisher`
`CurrentActorProvider` devuelve `recepcionista`/`system` fijos solo en perfiles dev/test; `AuditEventPublisher` es un recorder en memoria/no-op. El evento incluye `tipo`, entidad e id, actor con `modo=SIMULADO`, `occurredAt`, motivo/metadata y `correlationId`. No se consulta ni persiste auditoría: C-05 sigue pendiente y este change no es desplegable a producción. Alternativa descartada: derivar el actor de headers/tokens/cookies — sería auth encubierta sin aprobación.

### D-5 — Transaccionalidad y write-back
Los servicios de aplicación con `@Transactional` escriben el turno/bloqueo y su fila de ocupación en la misma unidad de trabajo (D6). El evento se publica después de la confirmación de la transacción, de modo que una operación rechazada o revertida no emita evento de éxito. La agenda es la fuente de estado real: no existe un registro paralelo de "reservas externas" que luego se sincronice.

### D-6 — `estado` y `origen` sin enum inventado
Ambos se modelan como `VARCHAR NOT NULL` con valor no vacío; la semántica concreta y el seed de duraciones quedan `(TBD)` y se registran como tal. No se define enum, checklist ni valor por convención (RN-GL-03). Los tests usan fixtures explícitos, no valores inventados como predeterminados del producto.

### D-7 — Endpoints indicativos y perfil
Las rutas `/agenda`, `/turnos`, `/bloqueos` y `/sobreturnos` son la superficie HTTP indicativa de dev/test. No se agrega dependencia de seguridad, filtro de autenticación ni lectura de identidad de la request. Verbos, versionado y paths definitivos se apartan en C-09/C-04.

### D-8 — Estrategia de verificación
Los tests de concurrencia y de la exclusion constraint corren contra una instancia PostgreSQL real provista por la dependencia/servicio de CI aprobado. H2 se reserva para tests unitarios de dominio y validaciones que no dependan de la restricción. Alternativa descartada: simular concurrencia solo con mocks: no probaría la garantía en capa de datos.

## Risks / Trade-offs

- **[Extensión `btree_gist` sin permisos en el entorno] →** verificar CREATE EXTENSION en la migración y fallar temprano con mensaje explícito; el aprovisionamiento de PostgreSQL en CI debe confirmarse antes del checkpoint T1.
- **[Desincronización entre tablas de agenda y ocupación] →** una única ruta de escritura transaccional desde los servicios, FKs a las entidades y tests que consulten ambas tablas tras cada operación; prohibir escrituras directas a la tabla de ocupación fuera de esa ruta.
- **[Actor simulado interpretado como identidad real] →** campo `modo=SIMULADO` en cada evento, sin headers/tokens/cookies y bloqueo explícito de despliegue a producción.
- **[Eventos interpretados como auditoría] →** contrato sin persistencia, sin API de consulta y documentación cruzada a C-05 en el propio spec; no se reportan métricas de cumplimiento.
- **[H2 no reproduce la constraint] →** separar la suite unitaria de la suite de integración PostgreSQL; no marcar RN-AG-01 como verificado solo con H2.
- **[Migración 005 compartida con C-07] →** este change crea y es dueño de esa migración; C-07 debe reutilizarla y no regenerarla cuando implemente su CRUD admin.
- **[Valores `(TBD)` resueltos por error] →** ninguna duración, estado, origen ni feriado se fija por defecto en el producto; los tests los suministran como datos de prueba explícitos.
- **[Sin UI ni RBAC] →** la superficie HTTP es indicativa y no apta para operación real; C-04 y la capa visual son requisitos previos al despliegue.

## Migration Plan

1. Aplicar `V005__scheduling_catalogs.sql` (slice C-07: tratamientos con duración, horarios, feriados, zona horaria).
2. Aplicar `V006__agenda_core.sql`: extensión `btree_gist`, secuencias, `turnos`, `bloqueos`, tabla de ocupación, índices y constraints.
3. Verificar con Flyway sobre PostgreSQL real: migración limpia desde cero y datos de prueba que comprueben rechazo de solapamiento y turno ↔ bloqueo.
4. Ejecutar la suite de integración concurrente; luego la suite unitaria con H2.
5. No hay despliegue a producción en este change: el rollback es no aplicar/retirar el entorno de prueba; no se escriben datos de usuario (D5). Cualquier restauración sobre datos reales sigue el procedimiento aislado de C-17.

## Open Questions

These can be answered later without changing the specs, approach, or task breakdown:

- Mecanismo concreto de aprovisionamiento de la instancia PostgreSQL efímera en CI (dependencia o servicio ya aprobado; se fija durante el apply).
- Nombres y conjuntos de valores de `estado` y `origen`, y seed definitivo de duraciones y feriados: permanecen `(TBD)` y se resuelven con el PO en su change correspondiente.
- Detalle de `aplica_a_todos` para feriados y anticipación mínima/máxima: pertenece al CRUD completo de C-07, no a este slice.
