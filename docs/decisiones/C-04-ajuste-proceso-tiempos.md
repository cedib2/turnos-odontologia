# Ajuste de proceso de C-04 por motivos de tiempo

- **Fecha**: 2026-11-21
- **Decisor**: `Lautaro Ellerbach, PO`
- **Alcance**: `C-04 auth-rbac` (Parte 1 — Tronco Base)
- **Referencia**: [C-04-decisiones-auth-rbac.md](C-04-decisiones-auth-rbac.md)

## Motivo

El ciclo completo de Strict TDD está tardando demasiado. El PO decide aligerar el proceso de trabajo a partir de cualquier retrabajo de C-04, sin alterar los resultados ya obtenidos.

## Ajuste aprobado

1. Un test por escenario del spec; sin casos de triangulación extra salvo para **rate limiting** y **concurrencia del refresh**.
2. Un ciclo por grupo: escribir tests, implementar, correr una vez. Sin fases `REFACTOR` separadas.
3. Durante el desarrollo correr solo los tests del grupo con `mvn test -Dtest=...`. La suite completa se corre al cerrar **T10** y en **T11**.
4. Revisar si cada clase de test levanta su propio contenedor PostgreSQL y, si es así, compartir un único contenedor entre las clases.

## Invariantes sin cambios

- Cobertura de cada escenario del spec.
- 93 tests previos en verde en T11.
- `openspec validate auth-rbac --strict` válido.

## Estado de aplicación

Este ajuste queda **registrado después de completar el apply** de C-04. El apply de T1..T11 ya fue ejecutado y verificado: **no hay grupo en curso** y no se reabre ni se desmarca ninguna tarea. La decisión es **no retroactiva** sobre T1..T11: los checkboxes permanecen `[x]` (49/49), con 147 tests totales (93 previos + 54 nuevos), 0 failures/errors y la ejecución detenida antes del archive. El proceso descrito arriba rige para cualquier retrabajo de C-04 desde ahora.

## Verificación del contenedor PostgreSQL compartido

Se verificó el estado actual sin realizar cambios: existe `src/test/java/com/turnosodontologia/support/PostgresFixture.java` con un `static final GenericContainer<?>` iniciado en un bloque `static {}`. Las clases Postgres lo reutilizan vía `PostgresFlywayTest` (por ejemplo, `AuthMigrationV002Test`, integraciones de agenda y tests de migración) y mediante llamadas directas a `PostgresFixture` (por ejemplo, `AuthRefreshRotationPostgresTest` y `BtreeGistExtensionTest`). Por lo tanto **ya hay un único contenedor compartido**, sin acción correctiva pendiente para el punto 4.

---

**Decisor**: `Lautaro Ellerbach, PO` · **Fecha**: 2026-11-21
