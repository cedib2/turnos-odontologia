-- C-17: unificación de las dos implementaciones de export-backup-restore.
-- Se conserva application/cumplimiento (tabla cumplimiento_operaciones). La tabla
-- export_backup_restore_operacion pertenecía al paquete export/, que se elimina.
-- V030 no se edita porque ya puede estar aplicada (regla de CHANGES.md).
DROP TABLE IF EXISTS export_backup_restore_operacion;
DROP FUNCTION IF EXISTS export_operacion_rechaza_modificacion();
