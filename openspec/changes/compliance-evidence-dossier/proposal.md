# Proposal — C-18 `compliance-evidence-dossier`

> Parte 4 — autor: Joaquín Adrián Velázquez Carrera. Governance **ALTO**: implementado en `feat/Parte4-J-Velazquez`, **pendiente de revisión**. Depende de C-12 (firma) solo para un ítem futuro: el dossier actual no requiere firma clínica.

## Why

La declaración de Ley 25.326 ya es habitual en la competencia (DD-06): el diferenciador es **cumplimiento demostrable con evidencia**. C-01 (decisión D=b) aprobó un **dossier mínimo con pendientes explícitos**: se descarga aunque esté incompleto, pero no se publica como completo mientras falte evidencia obligatoria.

## What Changes

- Migración **V031**: `dossier_items` (12 ítems exigidos sembrados como `PENDIENTE`) y `subprocesadores`.
- Ítems **MANUALES** (owner legal, marco normativo, RNBD/AAIP, DPA, INTIMA/CONSTA, retención, ubicación de datos): requieren referencia, verificador y fecha; ninguno se siembra como presente.
- Ítems **AUTOMÁTICOS** evaluados al generar: matriz RBAC, integridad de la cadena de auditoría (C-05), exportación completa y respaldo + restore verificado (C-17), subprocesadores con cláusulas AAIP 198/2023.
- Subprocesadores sin cláusulas publicadas: se registran pero quedan **excluidos** y el ítem sigue pendiente.
- Descarga auditada (`dossier.descargado`); publicación bloqueada con pendientes (`dossier.publicacion_bloqueada`, HTTP 409) o publicada (`dossier.publicado`).
- Métrica «Evidencia de cumplimiento» de C-01 E: `100 × presentes / exigidos`, con metodología y meta `(TBD)`.

## Sin inventar valores

El owner legal sigue sin designar (C-01 D): el ítem `OWNER_LEGAL` queda pendiente hasta que se registre su evidencia. No se cargan nombres, expedientes ni URLs de ejemplo.

## Impact

`application/cumplimiento/Dossier*`, `interfaces/web/DossierController` (dev/test), 5 tipos de auditoría nuevos.
