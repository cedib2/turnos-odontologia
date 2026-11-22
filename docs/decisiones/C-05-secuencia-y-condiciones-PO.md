# Decisión del PO — Secuencia de la Parte 1 y condiciones para C-05

- **Fecha**: 2026-11-21
- **Decisor**: Lautaro Ellerbach, Product Owner
- **Alcance**: Parte 1 — Tronco Base (`C-04`, `C-05`, `C-06`, `C-07`, `C-08`)

## Contexto

Se aceptan las recomendaciones del explore de `C-05 audit-append-only-log` con una modificación en el punto 1 (secuencia de ejecución). El explore se registró en Engram bajo `opsx/C-05/explore` y no escribió código ni artefactos OpenSpec.

## Decisión 1 — Secuencia operativa (modificación del punto 1)

**Se aprueba el orden: `C-04 → C-06 → C-05`.**

- Supersede el orden previo `C-05 → C-04 → C-06`.
- **Motivo del PO**: `C-06` desbloquea `C-09` y `C-13` de los compañeros; `C-05` puede reemplazarse después sobre el puerto `AuditEventPublisher` (hoy simulado en C-08).
- **Verificación de dependencia**: según `CHANGES.md`, el scope de `C-06 public-access-tokens` declara `Dependencias: C-04` y **no** lista `C-05`. Por lo tanto `C-06` no depende de `C-05`.
- `C-07` y `C-08` ya están cerrados. `C-08` permanece `[ ]` en `CHANGES.md` hasta que C-04 y C-05 estén implementados y verificados.

## Decisiones 2 a 7 — Recomendaciones del explore aceptadas

| # | Tema | Decisión aprobada |
|---|---|---|
| 2 | Catálogo de eventos | Tabla/vista validada en aplicación, de modo que nuevos eventos no exijan una migración. La lista exacta se pedirá antes del propose de C-05. |
| 3 | Actor | FK al usuario **más** snapshot inmutable y campo `modo` (`REAL` / `SIMULADO` / `SYSTEM` / `PACIENTE`). |
| 4 | Contexto de la fila | IP `INET` nullable, dispositivo opcional y `metadata` JSONB acotado con campos prohibidos. |
| 5 | Hash | HMAC-SHA256 si existe gestor de secretos aprobado; en su defecto SHA-256 con control de acceso estricto y documentación del límite. Orden canónico por secuencia `id`, nunca por `fecha`. |
| 6 | Consulta de auditoría | REST read-only para `auditor` y `administrador`, con PII enmascarada y registro de quién consulta. **Queda diferible** según el pedido del PO (ver abajo). |
| 7 | Retención e inmutabilidad | 10 años con archivado sin borrado físico, sujeto a confirmación legal de IN-05/IN-06. Trigger + `REVOKE` + cobertura de `TRUNCATE`, documentando límites de superuser y DDL. |

## Pedidos del PO para C-05 (cuando se retome)

1. **Lista exacta del catálogo de eventos**: nombres y granularidad (una fila por operación o por cambio de campo). No inventarla ni resolverla por convención (RN-GL-03).
2. **Campos prohibidos en `metadata`** (datos clínicos/PII) con un test que verifique el rechazo.
3. **Documentar el límite del hash sin clave**: si se usa SHA-256 por ausencia de gestor de secretos, dejar constancia explícita en la evidencia/dossier.
4. **Marcar la consulta REST como diferible**: no es bloqueante para C-05; permanece `(TBD)` y se traslada a un change posterior.

## Impacto en archivos

- `CHANGES.md`: nueva sección «Orden operativo de la Parte 1 (decisión del PO, 2026-11-21)», notas de orden en C-06 y C-05, y pedidos del PO agregados al scope de C-05.
- `.active-orchestrator-state.json`: secuencia actualizada; próxima acción `explore` de C-04.
- La migración reservada `V003__audit_append_only_log.sql` sigue sin escribirse.

## Regla de gobernanza

C-04, C-05 y C-06 son **CRITICO**: solo análisis y aprobación humana explícita antes de escribir código. Esta decisión autoriza la secuencia y el explore de C-04; **no** autoriza propose ni apply de ninguno de ellos.

---

**Decisor**: Lautaro Ellerbach (PO) · **Fecha**: 2026-11-21
