# Tasks (propuesta — no iniciar sin aprobación del PO)

## 0. Gate

- [ ] 0.1 Aprobación explícita del PO de esta propuesta y respuestas a P1–P7 de `design.md`, registradas en `docs/decisiones/C-17-decisiones-export-backup-restore.md`.

## 1. Esquema

- [ ] 1.1 Test de migración V030 (constraints de tabla `exportaciones`, `respaldos`, `restauraciones`).
- [ ] 1.2 `V030__export_backup_restore.sql`.
- [ ] 1.3 Coordinar con la Parte 1 la actualización de la lista de versiones en `AuthMigrationV002Test`.

## 2. Exportación

- [ ] 2.1 Tests: exportación completa con manifiesto y checksum; secciones pendientes visibles; vencimiento de plazo; auditoría.
- [ ] 2.2 `ExportacionService` y puerto `SeccionExportable`.

## 3. Respaldo

- [ ] 3.1 Tests: respaldo OK cifrado; fallo → estado `FALLIDO`, alerta y auditoría.
- [ ] 3.2 `RespaldoService`, `MotorRespaldo`, `AlertaAdministracion`.

## 4. Restore

- [ ] 4.1 Tests: restore en base aislada con verificación; restore a producción rechazado sin aislado verificado ni aprobación.
- [ ] 4.2 `RestoreService`.

## 5. Cierre

- [ ] 5.1 Suite completa en verde y delta de `audit-append-only-log`.
