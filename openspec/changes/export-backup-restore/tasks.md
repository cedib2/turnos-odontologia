# Tasks — C-17

- [x] 1. Migración V030 `cumplimiento_operaciones` con checks de consistencia y trigger de inmutabilidad.
- [x] 2. `Paquete` + `VolcadoBaseDatos` (instantánea REPEATABLE READ, orden por PK, secuencias).
- [x] 3. `CifradorArtefactos` AES-256-GCM; sin clave no se escribe artefacto.
- [x] 4. `ExportacionService` (completa / por paciente, plazo configurable, vencidas, descarga verificada).
- [x] 5. `RespaldoService` (respaldo, restore en entorno aislado con guardián y verificación fila a fila, alerta).
- [x] 6. `RespaldoProgramadoConfig` activo solo con `BACKUP_CRON`.
- [x] 7. Catálogo de auditoría ampliado y controlador dev/test.
- [x] 8. Tests: `ExportacionRespaldoRestoreTest` (7), `CumplimientoFallosTest` (2), `CifradorArtefactosTest` (3).
- [ ] 9. Revisión humana (governance ALTO) y archivo del change.
