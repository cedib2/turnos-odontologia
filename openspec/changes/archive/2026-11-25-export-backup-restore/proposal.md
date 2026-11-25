# Proposal

## Why

El sistema todavía no produce evidencia descargable de exportación, respaldo y recuperación: RN-SE-03/04/05 y US-014 exigen copia de la historia en un plazo, retención demostrable y un procedimiento de restore probado antes de escribir sobre datos de usuario. C-05 ya entrega un registro append-only real con cadena SHA-256, pero su catálogo estaba cerrado en cinco eventos; C-13 y C-15 aún no están en esta rama. El PO cerró el 2026-11-24/25 las siete decisiones que bloqueaban la planeación: migración, catálogo de eventos, plazo, formatos, storage, parámetros operativos y adaptadores por defecto. El PO pidió explícitamente **generar los artefactos de C-17 y frenar antes del apply** (governance ALTO): este documento solo planea, no autoriza implementación; sin apply hasta revisión del PO.

## What Changes

- Nueva capability `export-backup-restore` con contratos de exportación desacoplados: `ClinicalExportPort`, `FinancialExportPort`, `BackupStoragePort`, `OpsAlertPort` y `ExportSnapshotAssembler`, con DTO de dominio propios. Los adaptadores por defecto a C-13/C-15 devuelven colecciones vacías con `partialExport: true` y `unintegratedModules` (`clinical`, `financial`): el núcleo se ejecuta y testea hoy sin excepción bloqueante, y una exportación `PARCIAL` nunca se presenta como `COMPLETA`.
- Registro append-only de cada exportación, respaldo y restauración a través del puerto `AuditEventPublisher` de C-05, con los nombres inmutables aprobados por el PO: `EXPORT_REQUESTED`, `EXPORT_COMPLETED`, `EXPORT_FAILED`, `BACKUP_EXECUTED`, `BACKUP_FAILED`, `RESTORE_DRY_RUN_VERIFIED` y `RESTORE_EXECUTED`. Cada evento conserva `correlationId`, actor, `occurredAt`, motivo/metadata y resultado. El catálogo se extiende con un delta `MODIFIED` dentro de este change; sin endpoints REST de auditoría ni permisos nuevos.
- Exportación que compone identidad, agenda, clínico, financiero y auditoría, con `generadoEn`, zona horaria, alcance, conteos y distinción `COMPLETA`/`PARCIAL`; excluye credenciales, tokens y firmas de webhook. Formatos obligatorios: `JSON` estructurado con campo `schemaVersion` en el contrato versionado (sin número inventado) y `ZIP` autocontenido con `manifest.json`, checksum SHA-256 y adjuntos/CSV resumido.
- Copia de la historia clínica conservando anamnesis mínima versionada, odontograma FDI con piezas temporales y estado, evoluciones con firma y matrícula, y consentimientos. Plazo resuelto: **48 horas corridas** desde la solicitud del titular/consultorio, interpretadas en `America/Argentina/Buenos_Aires` sin cómputo de feriados (IN-05/IN-06 cerrados), con alerta preventiva a las **36 horas** por `OpsAlertPort`.
- Respaldo diario a las 03:00 (`BACKUP_CRON="0 0 3 * * *"`) con retención `EXPORT_RETENTION_YEARS=10` configurable vía properties, almacenamiento aislado por `tenant_id` bajo `BackupStoragePort`, cifrado AES-256 en reposo y verificación SHA-256. Implementación local/en memoria para tests y contrato listo para Object Storage en región Argentina sin subprocesadores no declarados. Sin borrado físico ni valores secretos en el repositorio.
- Restore en entorno aislado: verificación de la cadena de auditoría y de los conteos antes de cualquier escritura; la verificación ocurre fuera del entorno de datos de usuario y publica `RESTORE_DRY_RUN_VERIFIED` antes de `RESTORE_EXECUTED`.
- Migración `V030__export_backup_restore.sql` para el registro de operaciones. V001–V009 pertenecen al Tronco Base de Lautaro (Parte 1) y ya dejaron reservados los rangos; V030 garantiza cero colisiones al mergear. El archivo NO se crea en este propose y la lista `[1..6]` de `AuthMigrationV002Test:79` NO cambia.
- Sin cambios breaking en specs principales: la extensión del catálogo vive como delta en este change y se sincroniza recién en archive. Sin UI: `Decision: no (no UI)`.

**Diferido (fuera de alcance)**: implementación real de los adaptadores a C-13/C-15 (los adaptadores por defecto ya permiten operar), migración a Object Storage externo con proveedor declarado, dossier de C-18 y declaración de cualquier subprocesador nuevo antes de usarlo.

**Riesgo ALTO**: el PO autorizó solo la generación de artefactos. Queda prohibido apply, código de producción, tests de implementación, suite de tests, commit o modificación de `CHANGES.md`, `knowledge-base/`, specs principales ni cambios de otros changes.

## Capabilities

### New Capabilities

- `export-backup-restore`: evidencia de exportación completa y copia de historia, respaldo programado con retención y cifrado en reposo, restore aislado verificado y trazabilidad append-only de las tres operaciones, con contratos desacoplados de los dominios clínico y financiero.

### Modified Capabilities

- `audit-event-emission`: delta `MODIFIED` en este change que extiende el catálogo con los siete nombres inmutables aprobados por el PO (2026-11-24). La spec principal `openspec/specs/audit-event-emission/spec.md` NO se edita; el sync ocurre recién en archive. `agenda` no se reabre y su deuda de warnings >500 chars no se agrava.

## Impact

- Código: paquete propio de C-17 (p. ej. `com.turnosodontologia.application.export`) con puertos y DTO (`ClinicalExportPort`, `FinancialExportPort`, `BackupStoragePort`, `OpsAlertPort`); adaptadores por defecto parciales a C-13/C-15; uso del puerto `AuditEventPublisher` de C-05 y de la cadena existente para verificación en restore. `AuthMigrationV002Test:79` conserva `[1..6]`.
- Persistencia: migración `V030__export_backup_restore.sql`, sin crear en este propose. PostgreSQL 17, snake_case, sin borrado físico, cifrado AES-256 en reposo y verificación SHA-256; `spring.flyway.out-of-order: true` ya documentado en dev/test, sin duplicar.
- Configuración: solo nombres de variables (`BACKUP_CRON`, `EXPORT_RETENTION_YEARS`, ya presentes en `.env.example`); los valores aprobados figuran solo en los artefactos, nunca en archivos de configuración del repo. Sin credenciales reales ni nuevas literales de seguridad fuera de `auth`.
- Tests previstos para el apply posterior: `ExportSnapshotAssemblerTest`, `HistoriaClinicaExportCopyTest`, `BackupScheduleTest`, `RestoreIsolatedTest`, `ExportAuditTrailTest`, `AuditEventCatalogTest` y `ExportSecretsSurfaceTest`, con PostgreSQL/Testcontainers (`PostgresFixture`) para cadena, concurrencia y restore. Mapa escenario→test en `tasks.md`.
- Cobertura planificada: 6 requirements ADDED / 29 escenarios en `export-backup-restore`, más 1 requirement MODIFIED / 5 escenarios nuevos en el delta de `audit-event-emission` (34 escenarios nuevos del change, 34/34 mapeados). Sin deltas en `agenda`.
- Gobernanza: ALTO — artifact generation autorizada por el PO el 2026-11-24/25; las siete decisiones están cerradas; apply FRENADO: sin apply hasta revisión del PO.

---

**Decisor del pedido y de las 7 decisiones**: Lautaro Ellerbach, PO · **Fechas**: 2026-11-24/25 · **Gobernanza**: ALTO — solo planeación; sin apply hasta revisión del PO, sin código, tests ni commits.
