# Tasks (propuesta — no iniciar sin aprobación del PO)

## 0. Gate

- [ ] 0.1 Aprobación explícita del PO, owner legal designado (o explícitamente pendiente) y respuestas Q1–Q6, registradas en `docs/decisiones/C-18-decisiones-compliance-evidence-dossier.md`.
- [ ] 0.2 C-17 aplicado y verificado.

## 1. Esquema

- [ ] 1.1 Test de migración V031 y `V031__compliance_evidence_dossier.sql`.

## 2. Composición y bloqueo

- [ ] 2.1 Tests: dossier completo; ítem faltante → `INCOMPLETO` con pendiente; subprocesador sin cláusulas bloqueado; sin owner legal no hay firma.
- [ ] 2.2 `DossierService` y carga de evidencias.

## 3. Descarga y métricas

- [ ] 3.1 Tests: descarga auditada; métrica `evidencia_cumplimiento` calculada en C-22.
- [ ] 3.2 Descarga y `FuenteMetrica` `evidencia_cumplimiento`.

## 4. Cierre

- [ ] 4.1 Suite completa en verde y deltas de `adoption-metrics` y `audit-append-only-log`.
