# Tasks

> Gobernanza ALTO: el PO cerró las siete decisiones el 2026-11-24/25 y pidió **generar artefactos y frenar antes del apply**. Todas las casillas quedan `[ ]`. No ejecutar apply, no escribir código de producción ni tests, no correr la suite, no hacer commit. Fuente estable de escenarios: `openspec/specs/**` tras el archive; ningún test debe leer `openspec/changes/<change-activo>/`. **Sin apply hasta revisión del PO.**
>
> Notas TDD para el apply posterior: baseline informado de 194 tests en verde; un ciclo ROJO→VERDE por grupo con un test por escenario (34 escenarios: 29 de `export-backup-restore` y 5 nuevos del delta `MODIFIED` de `audit-event-emission`), sin triangulación extra ni fase REFACTOR separada; `mvn test -Dtest=...` por grupo y suite completa una sola vez al cierre; PostgreSQL 17/Testcontainers con `PostgresFixture` compartido para cadena, respaldo y restore; H2/dobles solo para lógica unitaria en el límite de los puertos. Las decisiones ya están cerradas: nada se resuelve por convención ni se inventa un valor.

## 1. Decisiones cerradas por el PO — verificación previa al apply

- [x] 1.1 Verificar que `proposal.md`, `design.md`, specs y tareas referencian solo `V030__export_backup_restore.sql`, sin propuesta V007 ni alternativa; decisor PO 2026-11-24. Verificación: 0 coincidencias de V007 como propuesta y `AuthMigrationV002Test:79` conserva `[1..6]` porque V030 no altera esa lista, con `spring.flyway.out-of-order: true` ya existente en dev/test y sin duplicarlo.
- [x] 1.2 Verificar que los siete nombres inmutables (`EXPORT_REQUESTED`, `EXPORT_COMPLETED`, `EXPORT_FAILED`, `BACKUP_EXECUTED`, `BACKUP_FAILED`, `RESTORE_DRY_RUN_VERIFIED`, `RESTORE_EXECUTED`) figuran como constantes/contrato exacto en spec y design y que el catálogo se extiende con un delta `MODIFIED` dentro de este change; decisor PO 2026-11-24. Verificación: `openspec validate export-backup-restore --strict` en verde, `openspec/specs/**` sin cambios y 0 endpoints REST ni permisos nuevos.
- [x] 1.3 Verificar el plazo de 48 horas corridas desde la solicitud del titular/consultorio, interpretado en `America/Argentina/Buenos_Aires`, sin feriados (IN-05/IN-06 cerrados), con alerta preventiva a las 36 horas por `OpsAlertPort`; decisor PO 2026-11-24. Verificación: 0 ocurrencias de `(TBD, IN-05/IN-06)` en los artefactos.
- [x] 1.4 Verificar los formatos obligatorios `JSON` con campo `schemaVersion` en el contrato versionado y `ZIP` con `manifest.json`, checksum SHA-256 y adjuntos/CSV resumido, sin número de versión inventado; decisor PO 2026-11-24/25. Verificación: contrato presente en spec y design y ninguna literal de versión arbitraria.
- [x] 1.5 Verificar `BackupStoragePort` con aislamiento por `tenant_id`, cifrado AES-256 en reposo, verificación SHA-256, implementación local/en memoria para tests y contrato listo para Object Storage en región Argentina sin subprocesadores no declarados; decisor PO 2026-11-25. Verificación: ninguna dependencia o servicio externo nuevo y declaración explícita de cualquier proveedor futuro.
- [x] 1.6 Verificar los parámetros aprobados `BACKUP_CRON="0 0 3 * * *"`, `EXPORT_RETENTION_YEARS=10` y el canal `OpsAlertPort` (log estructurado + webhook/email desacoplado); decisor PO 2026-11-25. Verificación: los valores figuran solo en artefactos, `.env.example` intacto y 0 valores nuevos en archivos de configuración del repositorio.
- [x] 1.7 Verificar los adaptadores por defecto con colecciones vacías, `partialExport: true` y `unintegratedModules` (`clinical`, `financial`), sin excepción bloqueante, y la distinción `COMPLETA`/`PARCIAL`; decisor PO 2026-11-25. Verificación: la exportación parcial nunca se presenta como completa y siguen fallando secretos/firmas de webhook, cadena inválida, conteos divergentes y destino de restore en producción.

## 2. Contratos, exportación parcial y formatos — 6 escenarios de R2

- [x] 2.1 Escribir `ExportSnapshotAssemblerTest` y `ExportFormatTest` con un test por escenario: snapshot con cinco dominios, adaptadores por defecto sin excepción, marcadores `partialExport`/`unintegratedModules`, artefacto sin secretos, JSON con `schemaVersion` y ZIP con `manifest.json`/SHA-256; verificación: `mvn test -Dtest=ExportSnapshotAssemblerTest,ExportFormatTest` en rojo.
- [x] 2.2 Definir `ClinicalExportPort`, `FinancialExportPort`, `BackupStoragePort`, `OpsAlertPort`, `ExportSnapshotAssembler` y `ExportSnapshot` en el paquete propio de C-17, con `generadoEn`, zona, alcance, conteos y resultado `COMPLETA`/`PARCIAL`; verificación: en verde y sin imports de entidades de C-13/C-15.
- [x] 2.3 Implementar adaptadores por defecto que devuelvan colecciones vacías con `partialExport: true` y `unintegratedModules` (`clinical`, `financial`), sin excepción bloqueante, y rechazo de credenciales, tokens y firmas de webhook; verificación: en verde, ciclo completo ejecutable y ninguna exportación parcial presentada como completa.
- [x] 2.4 Implementar los formatos `JSON` con `schemaVersion` y `ZIP` autocontenido con `manifest.json`, checksum SHA-256 y adjuntos/CSV resumido; verificación: `mvn test -Dtest=ExportFormatTest` en verde, sin versión inventada.

## 3. Copia de historia, 48 h corridas y alerta — 4 escenarios de R3

- [x] 3.1 Escribir `HistoriaClinicaExportCopyTest` con un test por escenario: copia fiel versionada/FDI/firma/consentimiento, copia dentro de 48 h corridas, alerta a las 36 h por `OpsAlertPort` y rechazo de evolución sin firma o matrícula; verificación: `mvn test -Dtest=HistoriaClinicaExportCopyTest` en rojo.
- [x] 3.2 Implementar la copia conservando anamnesis mínima versionada, odontograma FDI con piezas temporales y estado, evoluciones con firma y matrícula y consentimientos, sin sobrescribir la historia original; verificación: en verde y comparación del original antes/después.
- [x] 3.3 Implementar la evaluación de 48 horas corridas en `America/Argentina/Buenos_Aires` sin feriados y la alerta preventiva a las 36 h por `OpsAlertPort`, sin sobrescribir ni borrar versiones; verificación: en verde con los valores tomados de la decisión del PO y no de literales inventados.

## 4. Migración V030 y trazabilidad append-only — 5 escenarios de R1

- [x] 4.1 Escribir `ExportAuditTrailTest` sobre PostgreSQL 17/Testcontainers (`PostgresFixture`) con un test por escenario: exportación con `EXPORT_REQUESTED`/`EXPORT_COMPLETED`, `EXPORT_FAILED`, respaldo con `BACKUP_EXECUTED`/`BACKUP_FAILED`, restore con `RESTORE_DRY_RUN_VERIFIED`/`RESTORE_EXECUTED` y correlaciones distintas; verificación: `mvn test -Dtest=ExportAuditTrailTest` en rojo.
- [x] 4.2 Crear en el apply `V030__export_backup_restore.sql` para el registro de operaciones, sin editar migraciones ya aplicadas y sin alternativa V007; verificación: `mvn test -Dtest=ExportAuditTrailTest,AuthMigrationV002Test` en verde, `AuthMigrationV002Test:79` con `[1..6]` y `flyway migrate` sin errores sobre contenedor compartido.
- [x] 4.3 Integrar la publicación con `AuditEventPublisher` de C-05 usando los siete nombres inmutables aprobados, conservando `correlationId`, actor, `occurredAt`, motivo/metadata y resultado fallido; verificación: en verde, una fila por operación y cadena SHA-256 íntegra verificada por el servicio interno.
- [x] 4.4 Confirmar que no se crean `/audit`, `/auditoria`, endpoints REST de auditoría ni permisos nuevos; verificación: búsqueda en controladores y configuración con 0 coincidencias y `openspec/specs/**` sin cambios hasta el archive.

## 5. Respaldo programado, BackupStoragePort y retención — 7 escenarios de R4

- [x] 5.1 Escribir `BackupScheduleTest` y `BackupStoragePortTest` con un test por escenario: respaldo diario 03:00, aislamiento por `tenant_id`, cifrado AES-256/SHA-256, retención de 10 años sin borrado físico, fallo con `BACKUP_FAILED` y alerta, destino local/en memoria con contrato para Object Storage y sin subprocesadores no declarados; verificación: en rojo.
- [x] 5.2 Implementar la programación con `BACKUP_CRON="0 0 3 * * *"` en `America/Argentina/Buenos_Aires` y la retención `EXPORT_RETENTION_YEARS=10` vía properties, sin literales de valor en el repositorio; verificación: `mvn test -Dtest=BackupScheduleTest,SecretsInventoryTest` en verde y `.env.example` sin cambios.
- [x] 5.3 Implementar `BackupStoragePort` con aislamiento por `tenant_id`, cifrado AES-256 en reposo, verificación SHA-256, implementación local/en memoria para tests y contrato listo para Object Storage en región Argentina sin proveedor externo no declarado; verificación: `mvn test -Dtest=BackupStoragePortTest` en verde y 0 servicios externos instalados.
- [x] 5.4 Implementar publicación de `BACKUP_FAILED` y alerta por `OpsAlertPort` ante fallo de destino, sin borrado físico de datos de usuario; verificación: `mvn test -Dtest=BackupScheduleTest,ExportAuditTrailTest` en verde y ninguna sentencia `DELETE`/`TRUNCATE` sobre datos de usuario.

## 6. Restore aislado con verificación previa — 4 escenarios de R5

- [x] 6.1 Escribir `RestoreIsolatedTest` con PostgreSQL/Testcontainers y un test por escenario: restore verificado en aislado, cadena inválida, conteos divergentes y destino de producción rechazado; verificación: `mvn test -Dtest=RestoreIsolatedTest` en rojo.
- [x] 6.2 Implementar validación de destino aislado antes de abrir conexión de escritura, verificación previa de la cadena de C-05 y de los conteos por dominio contra el snapshot, y publicación de `RESTORE_DRY_RUN_VERIFIED`; verificación: en verde y 0 escrituras en los tres casos de rechazo.
- [x] 6.3 Confirmar que la verificación de cadena sigue siendo servicio interno sin HTTP, que `RESTORE_EXECUTED` solo se publica tras verificación y que una exportación parcial no oculta cadena inválida, conteos divergentes ni destino de producción; verificación: `mvn test -Dtest=RestoreIsolatedTest,ExportAuditTrailTest` en verde y 0 rutas nuevas.

## 7. Seguridad y guardias — 3 escenarios de R6

- [x] 7.1 Escribir `ExportSecretsSurfaceTest` con un test por escenario: configuración con nombres de variables, sin literales de seguridad en superficie de agenda y sin superficie HTTP nueva de auditoría; verificación: `mvn test -Dtest=ExportSecretsSurfaceTest` en rojo.
- [x] 7.2 Implementar la configuración del change usando solo nombres de variables en el repositorio, sin introducir `Authorization`, `Bearer`, `SecurityContext`, `@RequestHeader` ni `/auth` fuera de `auth`; verificación: `mvn test -Dtest=ExportSecretsSurfaceTest,AgendaSurfaceTest,CurrentActorProviderTest,SecretsInventoryTest,PublicDeltaInvariantsTest` en verde.
- [x] 7.3 Confirmar que `PublicDeltaInvariantsTest` sigue leyendo solo `openspec/specs/**` y fijando agenda en 13 requirements/31 escenarios, y que C-04/C-05/C-08 no se reabren; verificación: `mvn test -Dtest=PublicDeltaInvariantsTest` en verde y 0 diferencias en esos specs.

## 8. Extensión del catálogo audit-event-emission — 5 escenarios nuevos del delta MODIFIED

- [x] 8.1 Escribir `AuditEventCatalogTest` con un test por escenario nuevo del delta: eventos inmutables de exportación, fallo de exportación, eventos de respaldo, eventos de restore y correlación distinta entre operaciones; verificación: `mvn test -Dtest=AuditEventCatalogTest` en rojo.
- [x] 8.2 Verificar que los siete nombres son constantes/contrato exacto e inmutable y que cada evento conserva `correlationId`, actor, `occurredAt`, motivo/metadata y resultado; verificación: `mvn test -Dtest=AuditEventCatalogTest,ExportAuditTrailTest` en verde y 0 nombres alternativos.
- [x] 8.3 Confirmar que la spec principal `openspec/specs/audit-event-emission/spec.md` no se edita en este change y que el delta `MODIFIED` se sincroniza recién en archive, sin endpoints REST ni permisos nuevos; verificación: `openspec validate export-backup-restore --strict` en verde y `git diff` sin tocar `openspec/specs/**`.

## 9. Integración final y auditoría previa al archive

- [x] 9.1 Ejecutar la suite completa una sola vez al cerrar todos los grupos; verificación: `mvn test` con los 194 tests previos y los nuevos, 0 failures y 0 errors.
- [x] 9.2 Completar el mapa escenario→test siguiente y la lista de escenarios sin test; verificación: 34/34 escenarios cubiertos (29 de `export-backup-restore` + 5 del delta) y lista vacía.
- [x] 9.3 Ejecutar `openspec validate export-backup-restore --strict`, `openspec status --change export-backup-restore --json`, `openspec list --json` y `openspec validate --specs`; verificación: change válido, deuda preexistente de `spec/agenda` documentada y sin cambios en esa spec.

## Mapa de cobertura (a confirmar en el apply)

| Escenario del spec | Test propuesto |
|---|---|
| R1 Exportación con `EXPORT_REQUESTED` y `EXPORT_COMPLETED` | `ExportAuditTrailTest.exportacionConEventosRequestedYCompleted()` |
| R1 Exportación fallida con `EXPORT_FAILED` | `ExportAuditTrailTest.exportacionFallidaConExportFailed()` |
| R1 Respaldo con `BACKUP_EXECUTED` y `BACKUP_FAILED` | `ExportAuditTrailTest.respaldoConEventosExecutadoYFallido()` |
| R1 Restore con `RESTORE_DRY_RUN_VERIFIED` y `RESTORE_EXECUTED` | `ExportAuditTrailTest.restoreConDryRunYExecuted()` |
| R1 Correlaciones distintas entre operaciones distintas | `ExportAuditTrailTest.correlacionesDistintasEntreOperaciones()` |
| R2 Snapshot con los cinco dominios y metadatos de alcance | `ExportSnapshotAssemblerTest.snapshotConCincoDominiosYMetadatos()` |
| R2 Adaptadores por defecto sin excepción bloqueante | `ExportSnapshotAssemblerTest.adaptadoresPorDefectoSinExcepcion()` |
| R2 Exportación PARCIAL marcada explícitamente | `ExportSnapshotAssemblerTest.exportacionPartialMarcada()` |
| R2 Artefacto sin secretos ni firmas de webhook | `ExportSnapshotAssemblerTest.artefactoSinSecretosNiFirmas()` |
| R2 Exportación JSON con `schemaVersion` | `ExportFormatTest.jsonConSchemaVersion()` |
| R2 Paquete ZIP con `manifest.json`, SHA-256 y adjuntos/CSV resumido | `ExportFormatTest.zipConManifestYChecksum()` |
| R3 Copia fiel de la historia versionada | `HistoriaClinicaExportCopyTest.copiaFielDeLaHistoria()` |
| R3 Copia dentro de las 48 horas corridas en `America/Argentina/Buenos_Aires` | `HistoriaClinicaExportCopyTest.copiaDentroDe48HorasCorridas()` |
| R3 Alerta preventiva a las 36 horas por `OpsAlertPort` | `HistoriaClinicaExportCopyTest.alertaPreventivaALas36Horas()` |
| R3 Evolución sin firma o matrícula | `HistoriaClinicaExportCopyTest.evolucionSinFirmaOMatriculaSeRechaza()` |
| R4 Respaldo diario a las 03:00 según `BACKUP_CRON` | `BackupScheduleTest.respaldoDiario0300()` |
| R4 Almacenamiento aislado por `tenant_id` bajo `BackupStoragePort` | `BackupStoragePortTest.aislamientoPorTenant()` |
| R4 Cifrado AES-256 en reposo y verificación SHA-256 | `BackupStoragePortTest.cifradoAes256YSha256()` |
| R4 Retención de 10 años parametrizada sin borrado físico | `BackupScheduleTest.retencionDiezAniosSinBorradoFisico()` |
| R4 Fallo de respaldo con `BACKUP_FAILED` y alerta por `OpsAlertPort` | `BackupScheduleTest.falloConBackupFailedYAlerta()` |
| R4 Destino local/en memoria para tests y contrato listo para Object Storage en región Argentina | `BackupStoragePortTest.destinoLocalYContratoObjectStorage()` |
| R4 Sin subprocesadores no declarados | `BackupStoragePortTest.sinSubprocesadoresNoDeclarados()` |
| R5 Restore verificado en entorno aislado | `RestoreIsolatedTest.restoreVerificadoEnAislado()` |
| R5 Cadena de auditoría inválida | `RestoreIsolatedTest.cadenaInvalidaAbortaSinEscribir()` |
| R5 Conteos divergentes | `RestoreIsolatedTest.conteosDivergentesAbortanSinEscribir()` |
| R5 Destino de producción rechazado | `RestoreIsolatedTest.destinoProduccionRechazado()` |
| R6 Configuración con nombres de variables | `ExportSecretsSurfaceTest.configuracionSoloConNombres()` |
| R6 Sin literales de seguridad en la superficie de agenda | `ExportSecretsSurfaceTest.sinLiteralesDeSeguridadFueraDeAuth()` |
| R6 Sin superficie HTTP nueva de auditoría | `ExportSecretsSurfaceTest.sinSuperficieHttpNueva()` |
| AUD-EMI Eventos inmutables de exportación | `AuditEventCatalogTest.eventosInmutablesDeExportacion()` |
| AUD-EMI Evento de fallo de exportación | `AuditEventCatalogTest.eventoDeFalloDeExportacion()` |
| AUD-EMI Eventos de respaldo ejecutado y fallido | `AuditEventCatalogTest.eventosDeRespaldoEjecutadoYFallido()` |
| AUD-EMI Eventos de restore verificado y ejecutado | `AuditEventCatalogTest.eventosDeRestoreVerificadoYEjecutado()` |
| AUD-EMI Correlación distinta entre operaciones de exportación y respaldo | `AuditEventCatalogTest.correlacionDistintaEntreOperaciones()` |

**Total: 34/34 escenarios mapeados** (29 de `export-backup-restore` + 5 nuevos del delta `audit-event-emission`).

## Workflow follow-up

- Archive del change solo tras la revisión y aprobación explícitas del PO; **sin apply hasta revisión del PO**. Las siete decisiones ya están registradas con decisor y fecha 2026-11-24/25.
- En archive se sincronizan los deltas, incluido el `MODIFIED` de `audit-event-emission`; recién entonces marcar C-17 en `CHANGES.md`.
- Sin commit, push ni PR sin pedido explícito del PO.
