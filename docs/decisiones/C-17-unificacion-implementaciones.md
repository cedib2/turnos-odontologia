# C-17 — Unificación de las dos implementaciones de `export-backup-restore`

- **Fecha**: 2026-11-25
- **Decisor**: José Donato Fornasari Donadel, en la integración de `demo-integracion`
- **Governance**: ALTO (exportación, respaldo y restore)

## Contexto

Al integrar la Parte 4 en `demo-integracion` quedaron dos implementaciones de C-17, ambas de Joaquín Adrián Velázquez Carrera:

| | `feat/parte4-Joaquin` | `feat/Parte4-J-Velazquez` |
|---|---|---|
| Código | paquete `export/` | `application/cumplimiento/` |
| Tabla (V030) | `export_backup_restore_operacion` | `cumplimiento_operaciones` |
| Cableado en Spring | ninguno: sin beans, la aplicación no la usa | servicios, respaldo programado y controlador dev/test |
| Cifrado en reposo | puerto de almacenamiento | AES-256-GCM, falla sin clave |
| Restore | conteos sobre un puerto | base aislada real, verificación fila por fila y guardián contra la base productiva |
| Dependientes | solo el catálogo de auditoría | C-18 (dossier) y C-22 (métricas) |
| openspec | `archive/2026-11-25-export-backup-restore` | change activo `export-backup-restore` |

El merge combinó las dos tablas en una misma V030 y dejó C-17 duplicado en `CHANGES.md` y openspec.

## Decisión

Se conserva `application/cumplimiento` y se retira el paquete `export/`.

- Se borran `export/` y sus 9 clases de test (34 tests).
- `AuditoriaCatalogo` deja de incluir los 7 eventos `EXPORT_*`, `BACKUP_*` y `RESTORE_*`. Quedan los eventos de cumplimiento (`exportacion.*`, `respaldo.*`, `restore.*`), que ya estaban en el catálogo.
- `ExportSecretsSurfaceTest` se porta como `cumplimiento/CumplimientoSuperficieTest`. Además se corrige un defecto: su filtro de `src/main` nunca encontraba archivos, así que tres aserciones no verificaban nada.
- **V032** elimina `export_backup_restore_operacion` y su función de trigger. V030 no se edita porque ya puede estar aplicada: cambiar su checksum rompe la validación de Flyway (regla de `CHANGES.md`).
- openspec: `specs/export-backup-restore` y `specs/audit-event-emission` pasan a describir la implementación vigente; el change activo se archiva como `archive/2026-11-25-export-backup-restore-unificado`, y el archivo anterior queda marcado como reemplazado.

## Alternativa descartada

Conservar `export/`: habría roto C-18 y C-22, que dependen de `application/cumplimiento`, y habría requerido cablear `export/` en Spring desde cero.

## Consecuencias

- La suite pasa de 502 a 471 tests, todos en verde.
- Pendiente: ningún componente implementa `ProveedorHistoriaClinica`, así que la copia por paciente declara la historia clínica como pendiente aunque C-11 y C-13 ya estén implementados. Hay que conectar ese contrato con C-13.
