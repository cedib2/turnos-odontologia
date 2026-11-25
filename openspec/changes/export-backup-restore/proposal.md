# Proposal

> **Estado: PROPUESTA — esperando revisión.** Governance ALTO: no se escribe código, migración ni configuración hasta la aprobación explícita del PO de esta propuesta y de las decisiones de `design.md` §Preguntas para el PO.

## Why

RN-SE-03 y US-014 exigen exportación completa y respaldo periódico, y la regla dura del proyecto pide **restore probado en entorno aislado antes de escribir sobre datos de usuario**. Sin C-17 no hay evidencia de exportación ni de respaldo para el dossier (C-18) y el cumplimiento sigue siendo declarativo (RN-SE-04, DD-06). C-01 B5/B6 ya fijaron infraestructura mínima en cloud región AR con DPA; los plazos, formatos, frecuencia y retención siguen `(TBD)`.

## What Changes

- Nueva capability `export-backup-restore`.
- Migración `V030__export_backup_restore.sql` (rango Parte 4): registro de `exportaciones`, `respaldos` y `restauraciones` con estado, checksum SHA-256, plazo, destino y retención. Solo metadatos: nunca el contenido exportado.
- **Exportación completa** de datos del consultorio y **copia de historia** del paciente (consumiendo la interfaz de C-13), con vencimiento de plazo `(TBD, propuesta del informe: 48 h)` y estado `VENCIDA` si no se completa a tiempo.
- **Respaldo programado** con `BACKUP_CRON` (nombre ya en `.env.example`, valor `(TBD)`), cifrado en reposo y retención `(TBD, propuesta: 10 años)`; **alerta** a administración ante fallo por un puerto `AlertaAdministracion` (canal `(TBD)`).
- **Restore probado**: restauración siempre primero en una base aislada, con verificación de integridad (conteos + cadena de auditoría de C-05). Restaurar sobre la base de usuario MUST requerir un restore aislado verificado previo y una aprobación explícita registrada.
- Cada exportación, respaldo y restore publica eventos en el log append-only de C-05 (`exportacion.*`, `respaldo.*`, `restore.*`).
- Aporta a C-18 los identificadores de la última exportación y del último respaldo verificado.

**Fuera de alcance**: proveedor cloud concreto, UI, verificación de identidad del paciente (C-13), datos financieros más allá de lo que exponga C-15.

## Capabilities

### New Capabilities
- `export-backup-restore`: exportación, respaldo, restore aislado y su trazabilidad.

### Modified Capabilities
- `audit-append-only-log`: el catálogo admite los eventos de exportación, respaldo y restore.

## Impact

- Esquema: `V030__export_backup_restore.sql`.
- Dependencias: C-05 (disponible), C-13 (Parte 2: interfaz de historia), C-15 (Parte 3: datos de señas). Se consumen por puertos; mientras no existan, la exportación completa los lista como secciones pendientes, sin inventar datos.
- Governance: ALTO — backup/restore y configuración que afecta datos de usuario.
