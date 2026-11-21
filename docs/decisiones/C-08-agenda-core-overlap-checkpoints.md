# C-08 — Checkpoints MEDIO: `agenda-core-overlap`

- **Change**: `agenda-core-overlap` (OpenSpec)
- **Governance**: MEDIO
- **Fecha**: 2026-11-20
- **Decisor**: PO (aprobación de defaults recibida antes de este apply; no se volvió a preguntar)
- **Alcance registrado**: tareas 3.2 y 7.2 del plan; este archivo se actualiza al llegar a cada checkpoint.

## Checkpoint MEDIO 1 — tarea 3.2

**Estado**: aprobado por defaults del PO; evidencia técnica registrada antes de continuar con T4.

**Revisado**
- Migración `src/main/resources/db/migration/V005__scheduling_catalogs.sql`: catálogos mínimos sin seeds (`tratamientos`, `horarios`, `feriados`), zona `America/Argentina/Buenos_Aires`.
- Migración `src/main/resources/db/migration/V006__agenda_core.sql`: `turnos`, `bloqueos` y `ocupaciones`, con `btree_gist`, `tstzrange` y exclusion constraints por profesional y sillón; intervalos `[)` contiguos permitidos.
- Modelo: `Tratamiento`, `HorarioAtencion`, `Feriado`, `Turno`, `Bloqueo`, `Ocupacion`, con mapeo explícito `snake_case` (D4), `AuditableEntity` y soft delete (D5); sin borrado físico.
- Anti-solapamiento RN-AG-01: enforceado en PostgreSQL, no solo en la aplicación; tabla de ocupación cubre turno ↔ bloqueo en ambos sentidos.

**Evidencia**
- `mvn -q -Dtest=OcupacionExclusionTest -DfailIfNoSpecifiedTests=false test`: RED con `relation "ocupaciones" does not exist` / `no exclusion constraint`.
- `mvn -q -Dtest=OcupacionExclusionTest,Migration006Test -DfailIfNoSpecifiedTests=false test`: GREEN, `Tests run: 6, Failures: 0, Errors: 0`.
- `mvn -q -Dtest=TurnoBloqueoPersistenceTest,OcupacionExclusionTest,Migration006Test -DfailIfNoSpecifiedTests=false test`: GREEN, `Tests run: 8, Failures: 0, Errors: 0`.
- Infra previa validada: `postgres:17` en Docker Desktop con `btree_gist 1.7`; `application-test.yml` con `ddl-auto: validate` y Flyway `clean-disabled: false`.

**Defaults confirmados por el PO**
- Sin autenticación, RBAC, sesiones, tokens, cookies ni `/auth/*`.
- Actor fijo `recepcionista`/`system`, modo `SIMULADO`.
- Auditoría en memoria/no-op, sin persistencia ni API de consulta.
- Sin UI, sin secretos en el repo, sin commits y sin marcar `C-08` en `CHANGES.md`.

## Checkpoint MEDIO 2 — tarea 7.2

**Estado**: aprobado por defaults del PO; evidencia técnica registrada antes de continuar con T8.

**Revisado**
- `SobreturnoService.reprogramarConSobreturno`: desplazamiento atómico del turno existente; libera la ocupación original, valida feriado/horario/destino libre con la misma regla RN-AG-01 y crea la nueva ocupación en una sola transacción.
- `motivo` y `actor` son obligatorios; se aceptan solo los actores simulados `recepcionista`/`system`, con `modo=SIMULADO`.
- Evento `turno.sobreturno` emitido después del commit con `motivo`, `actorAutorizo` y `correlationId`; no se persiste ni se presenta como evidencia de cumplimiento.
- Frente a IN-04/RN-GL-01: prevalece el anti-solapamiento; el override no desactiva la exclusion constraint, solo mueve el turno a un hueco válido.
- Operación rechazada: sin modificación del turno, sin nueva ocupación activa y sin evento de confirmación.

**Evidencia**
- `mvn -q -Dtest=SobreturnoServiceTest,SobreturnoIntegrationTest -DfailIfNoSpecifiedTests=false test`: GREEN, `Tests run: 5, Failures: 0, Errors: 0`.
- Casos cubiertos: destino libre con motivo/actor, rechazo sin motivo/actor, destino ocupado sin mutar el turno, sobreturno confirmado y destino ocupado por bloqueo sin evento.

**Defaults confirmados por el PO**
- Misma política del checkpoint 1: sin auth/RBAC, actor simulado, auditoría no persistente, sin UI, sin secretos, sin commits y `C-08` sigue `[ ]`.

## Cierre técnico del apply — tareas 10.1 a 10.3

**Estado**: verificación integral completada; el change queda listo para revisión/archive; no se declara despliegue ni cumplimiento.

**Evidencia**
- `mvn -q test`: `BUILD SUCCESS`; 36 suites y 82 tests, `Failures: 0, Errors: 0, Skipped: 0`.
- Cobertura verificada: concurrencia real en PostgreSQL con `CyclicBarrier`, bloqueos, reprogramación atómica, sobreturno, write-back interno, vistas día/semana, eventos de auditoría emitidos después del commit y superficie HTTP dev/test.
- Harness C-02: `javac -cp target\classes -d <temp> tests\SmokeTest.java` + `java -cp target\classes;<temp> SmokeTest`: `9/9 cases passed`.
- `openspec validate agenda-core-overlap --strict`: `Change 'agenda-core-overlap' is valid`.
- `openspec instructions apply --change "agenda-core-overlap" --json`: 23/23 checks completos, `state: ready`.

**Hallazgo resuelto durante la verificación**
- Los tests H2 `@DataJpaTest` sin perfil no encontraban `CurrentActorProvider` ni `AuditEventPublisher` porque sus implementaciones están restringidas a `dev/test`; se agregaron beans condicionales (`@ConditionalOnMissingBean`) en `src/test/java/com/turnosodontologia/TestJpaConfig.java`. No cambia la política de perfiles en producción.

**Verificación explícita de cierre**
- Los checkpoints 3.2 y 7.2 ya tenían evidencia registrada antes de marcarse; no se marcaron checkpoints sin revisión.
- `CHANGES.md` conserva `### [C-08] agenda-core-and-overlap-prevention` con `- **Estado**: [ ] pendiente`.
- No se agregó autenticación, RBAC, lectura de identidad, persistencia de auditoría, UI, secretos ni dependencias de seguridad.
- No se afirma despliegue a producción, implementación de C-04/C-05 ni cumplimiento regulatorio; la auditoría sigue siendo emisión en memoria/no-op.

## Archive

- **Fecha**: 2026-11-21
- **Decisor**: Orquestador, dentro del alcance delegado por el PO ("sigamos"); no es una decisión CRITICO.
- **Decisión registrada**: sincronizar los deltas antes de archivear (opción A, recomendada). Fundamento: el usuario pidió el ciclo OPSX completo para cada change y `archive` incluye sincronizar specs; sincronizar crea la línea base de `agenda` y `audit-event-emission` que C-04/C-05/C-07 podrán modificar con deltas `MODIFIED` al reemplazar los puertos simulados. No sincronizar haría desaparecer ese contrato del árbol de specs principal. El modo `SIMULADO` y la nota "C-05 sigue pendiente / sin valor de cumplimiento" ya están explícitos en el delta, así que no se acredita auditoría ni despliegue. Si el PO revierte la decisión, se ajusta en el delta del siguiente change.

**Evidencia**
- `mvn test`: `BUILD SUCCESS`; `Tests run: 82, Failures: 0, Errors: 0, Skipped: 0`; PostgreSQL 17.12 vía Testcontainers/Docker Desktop disponible durante toda la corrida (incluidos `OcupacionExclusionTest` y las migraciones V005/V006). No se sustituyó PostgreSQL por H2 en tests de concurrencia.
- `openspec validate --strict` sin argumento indicó "Nothing to validate"; `openspec validate agenda-core-overlap --strict`: `Change 'agenda-core-overlap' is valid`.
- `openspec status --change "agenda-core-overlap" --json`: cuatro artefactos `done`, `isComplete: true`; `openspec list --json`: `23/23` tareas completas.
- `openspec instructions specs --change "agenda-core-overlap" --json`: ejecutado una vez antes de escribir las specs principales; sin reglas adicionales que aplicar.
- Sync inline: creadas `openspec/specs/agenda/spec.md` (11 requisitos) y `openspec/specs/audit-event-emission/spec.md` (5 requisitos), con los `## Purpose` copiados verbatim y sin encabezados delta. `openspec validate --specs`: 2 passed, 0 failed.
- Verificación del contrato sincronizado: `SIMULADO` aparece en el requisito "Actor con modo simulado explícito", en el escenario de sobreturno y en "Sin identidad verificada"; "C-05 sigue pendiente" y la prohibición de presentar los eventos como evidencia de cumplimiento permanecen en "Emisión sin persistencia ni valor de cumplimiento". Consecuencia para C-04/C-05: sus deltas `MODIFIED` deberán reemplazar esos contratos simulados; C-05 sigue pendiente y sigue sin acreditar auditoría ni despliegue.
- Archive: `openspec/changes/agenda-core-overlap/` movido a `openspec/changes/archive/2026-11-21-agenda-core-overlap/`; se conservaron `proposal.md`, ambos deltas, `design.md`, `tasks.md` y `.openspec.yaml`.
- Verificación posterior: `openspec list --json` → `"changes": []`; `openspec list --specs` → `agenda` 11 requisitos y `audit-event-emission` 5 requisitos; `openspec validate --all --strict` → 2 passed, 0 failed.
- `CHANGES.md:297` permanece `- **Estado**: [ ] pendiente`. Sin commits, sin push, sin secretos y sin cambios en C-04, C-05, C-06, C-07 ni changes de compañeros.
