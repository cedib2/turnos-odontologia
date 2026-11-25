# Proposal

> **Estado: PROPUESTA — esperando revisión.** Governance ALTO: no se escribe código, migración ni configuración hasta la aprobación explícita del PO y la designación del owner legal (pendiente de C-01 §D).

## Why

El cumplimiento **demostrable** es un diferenciador real: la declaración de Ley 25.326 ya es habitual en la competencia (RN-SE-04, DD-06). C-01 §D=b aprobó un **dossier mínimo con listado explícito de pendientes** (cumplimiento progresivo) y dejó `(TBD)` el owner legal que lo firma. C-22 ya espera la métrica `evidencia_cumplimiento` por el puerto `FuenteMetrica`.

## What Changes

- Nueva capability `compliance-evidence-dossier`.
- Migración `V031__compliance_evidence_dossier.sql`: `evidencias_dossier` (ítem, estado `PRESENTE`/`PENDIENTE`, referencia y checksum del documento, vigencia, cargado por) y `subprocesadores` (nombre, servicio, ubicación, cláusulas AAIP 198/2023 publicadas y su referencia).
- Composición del dossier: DPA responsable/encargado, registro AAIP/RNBD, cláusulas AAIP 198/2023 por subprocesador, roles y permisos (C-04), verificación de integridad del log (C-05), última exportación y último respaldo/restore verificado (C-17), retención y marco normativo aplicable `(TBD)`.
- **Bloqueo**: un subprocesador sin cláusulas publicadas no se incluye como cubierto y deja el dossier `INCOMPLETO`; el dossier MUST NOT presentarse como completo ni firmado mientras falten ítems exigidos o no haya owner legal designado.
- El dossier incompleto se puede descargar **con la lista de pendientes visible** (coherente con D=b); cada descarga se audita (`dossier.descargado`).
- Implementa `FuenteMetrica` `evidencia_cumplimiento` para C-22: `100 × ítems presentes y verificables / ítems exigidos`.

**Fuera de alcance**: redacción de los documentos legales, trámite RNBD/INTIMA/CONSTA, UI.

## Capabilities

### New Capabilities
- `compliance-evidence-dossier`: composición, bloqueo y descarga auditada del dossier.

### Modified Capabilities
- `adoption-metrics`: la métrica `evidencia_cumplimiento` pasa de `NO_DISPONIBLE` a calculada.
- `audit-append-only-log`: el catálogo admite `dossier.evidencia.cargada` y `dossier.descargado`, a aprobar por el PO según la Decisión 1 de C-05.

## Impact

- Esquema: `V031__compliance_evidence_dossier.sql`.
- Dependencias: C-01 (owner legal), C-12 (Parte 2: firma/consentimientos como evidencia), C-17 (este mismo bloque).
- Governance: ALTO — evidencia legal.
