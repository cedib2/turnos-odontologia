# Proposal — C-17 `export-backup-restore`

> Parte 4 — autor: Joaquín Adrián Velázquez Carrera. Governance **ALTO**: implementado en la rama `feat/Parte4-J-Velazquez`. Revisado y archivado el 2026-11-25 al unificar C-17: esta es la implementación vigente; la del paquete `export/` (`archive/2026-11-25-export-backup-restore`) quedó retirada.

## Why

RN-SE-03 exige exportación completa y respaldo periódico, y RN-SE-04 exige que el cumplimiento sea **demostrable**. Hoy no existe forma de exportar los datos del consultorio, entregar la copia de un paciente, respaldar la base ni probar que un respaldo se puede restaurar. Sin eso, el dossier de C-18 no tiene evidencia que mostrar.

## What Changes

- Migración **V030** (rango reservado de la Parte 4): `cumplimiento_operaciones`, registro de cada exportación, respaldo y restore con estado, checksum SHA-256, plazo, retención y destino. Transición única `SOLICITADA → COMPLETADA | FALLIDA`; estados finales inmutables y sin `DELETE` (trigger).
- **Exportación** completa y **copia por paciente** (`pacientes`, `turnos` y la historia vía el contrato `ProveedorHistoriaClinica` de C-13). Excluye material de credenciales (`auth_*`, `public_token_jti`, `public_idempotencia`, `usuarios.hash_password`).
- **Respaldo** completo en instantánea consistente (`REPEATABLE READ`), **cifrado AES-256-GCM en reposo** (RN-SE-05). Sin clave configurada el respaldo **falla** y no se escribe nada en claro.
- **Restore probado en entorno aislado**: se restaura en una base descartable, se verifica fila por fila contra el respaldo y se rechaza si el entorno resulta ser la base productiva.
- Respaldo programado solo si `BACKUP_CRON` está definido; alerta ante fallo por el puerto `AlertaRespaldo` (canal real `(TBD)`).
- Auditoría append-only (C-05) de cada solicitud, resultado, descarga y fallo.

## Sin inventar valores (RN-GL-03)

Plazo de copia (48 h), retención (10 años), frecuencia, formato oficial y destino siguen `(TBD, IN-05/IN-06)`: son parámetros. Sin configurar, el plazo y la retención quedan `null` y el dossier los reporta como pendientes.

## Impact

- Código: `application/cumplimiento/*`, `interfaces/web/CumplimientoController` (dev/test), `audit/AuditoriaCatalogo` (8 tipos nuevos).
- Variables nuevas propuestas: `BACKUP_ENCRYPTION_KEY`, `BACKUP_DIR`, `RESTORE_SANDBOX_URL`/`_USER`/`_PASSWORD`, más `BACKUP_CRON` ya listada en la KB.
- Fuera de alcance: destino cloud definitivo (B6), gestión de claves/KMS, UI.
