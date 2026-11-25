# C-22 — Propuesta de evento de auditoría `metricas.consultadas`

- **Change**: `C-22 adoption-metrics-dashboard`
- **Régimen**: Decisión 1 de `docs/decisiones/C-05-ajustes-pre-apply-PO.md` (catálogo cerrado; ampliarlo requiere decisión del PO)
- **Propuesto por**: responsable de la Parte 4
- **Fecha**: 2026-11-25
- **Estado**: **PENDIENTE DE APROBACIÓN DEL PO**

## Contexto

El scope de C-22 exige que «cada consulta sensible quede registrada en auditoría». El evento se agregó al catálogo durante el apply sin la aprobación previa que pide la Decisión 1 de C-05. Esta propuesta regulariza esa omisión. C-22 **no se archiva** hasta que el PO resuelva.

## Evento propuesto

| Campo | Valor |
|---|---|
| `accion` | `metricas.consultadas` |
| `entidad` | `metricas` |
| `entidad_id` | `null` (no hay entidad agregada concreta, igual que `publictoken.historia.denegado`) |
| Actor | `CurrentActorProvider.current()` |
| `metadata` | `desde`, `hasta` y el estado por métrica (`CALCULADA`, `SIN_DATOS`, `NO_DISPONIBLE`); sin datos personales ni clínicos |
| Granularidad | una fila por consulta válida; ninguna si el período es inválido |
| Transacción | la consulta y el evento comparten `@Transactional` (requirement «Transacción compartida» de C-05) |

## Artefactos actualizados

- `openspec/changes/adoption-metrics-dashboard/specs/audit-append-only-log/spec.md`: `MODIFIED` de «Catálogo mínimo y granularidad por operación» y «Trazabilidad mínima de cada fila».
- Tests: `MetricasAdopcionIntegrationTest.cadaConsultaQuedaAuditadaConActorYPeriodo`, `MetricasAuditoriaPersistidaTest`, `MetricasTransaccionTest`.

## Opciones para el PO

1. **Aprobar** `metricas.consultadas` tal como está.
2. **Aprobar con otro nombre o forma** (el código cambia una constante).
3. **Rechazar**: se quita el evento del catálogo y C-22 deja de auditar consultas, lo que contradice su scope; requiere enmendar C-22.

## Aprobación

- Decisor: `(pendiente)`
- Fecha: `(pendiente)`
