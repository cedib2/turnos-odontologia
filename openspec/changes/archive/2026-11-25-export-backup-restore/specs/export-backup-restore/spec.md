# Spec Delta

## Purpose

Define la evidencia observable de exportación completa o parcial, copia de historia clínica, respaldo programado y restauración aislada, incluyendo trazabilidad append-only con los siete eventos aprobados y verificación previa a cualquier escritura de datos de usuario.

## ADDED Requirements

### Requirement: Trazabilidad append-only de operaciones de exportación, respaldo y restore
El sistema SHALL publicar una fila de auditoría por exportación, respaldo o restore vía `AuditEventPublisher` de C-05 con contrato inmutable: `EXPORT_REQUESTED`, `EXPORT_COMPLETED`, `EXPORT_FAILED`, `BACKUP_EXECUTED`, `BACKUP_FAILED`, `RESTORE_DRY_RUN_VERIFIED`, `RESTORE_EXECUTED`. Cada evento SHALL incluir `correlationId`, actor, `occurredAt`, motivo/metadata y resultado. El catálogo SHALL extenderse por delta `MODIFIED`; MUST NOT crear endpoints REST ni permisos nuevos.

#### Scenario: Exportación con `EXPORT_REQUESTED` y `EXPORT_COMPLETED`
- **GIVEN** una exportación solicitada por un actor con su correlación
- **WHEN** se consulta el registro append-only de C-05
- **THEN** existen `EXPORT_REQUESTED` y `EXPORT_COMPLETED` con actor, `occurredAt`, motivo/metadata y resultado exitoso, conservando la correlación

#### Scenario: Exportación fallida con `EXPORT_FAILED`
- **GIVEN** una exportación que termina en error
- **WHEN** se consulta el registro append-only de C-05
- **THEN** existe `EXPORT_FAILED` con la correlación, el actor, `occurredAt` y el motivo del error, y no se reporta éxito

#### Scenario: Respaldo con `BACKUP_EXECUTED` y `BACKUP_FAILED`
- **GIVEN** un respaldo programado ejecutado y un respaldo posterior que falla por indisponibilidad del destino
- **WHEN** se consulta el registro append-only de C-05
- **THEN** existen `BACKUP_EXECUTED` y `BACKUP_FAILED` con sus correlaciones, motivo/metadata y resultados correspondientes

#### Scenario: Restore con `RESTORE_DRY_RUN_VERIFIED` y `RESTORE_EXECUTED`
- **GIVEN** un restore en entorno aislado tras verificar cadena y conteos
- **WHEN** se consulta el registro append-only de C-05
- **THEN** existen `RESTORE_DRY_RUN_VERIFIED` y `RESTORE_EXECUTED` vinculados a la misma correlación, con actor, `occurredAt` y resultado verificado

#### Scenario: Correlaciones distintas entre operaciones distintas
- **GIVEN** una exportación, un respaldo y un restore ejecutados en secuencia
- **WHEN** se consulta el registro append-only de C-05
- **THEN** existen filas con correlaciones distintas y ninguna se reutiliza como resultado de otra operación

### Requirement: Exportación desacoplada, distinción COMPLETA/PARCIAL y formatos
El sistema SHALL componer un snapshot con los cinco dominios, `generadoEn`, zona, alcance y conteos, y distinguir `COMPLETA` de `PARCIAL`. Los adaptadores por defecto SHALL devolver colecciones vacías con `partialExport: true` y `unintegratedModules` (`clinical`, `financial`) sin bloqueo. SHALL soportar `JSON` con `schemaVersion` y `ZIP` con `manifest.json`, SHA-256 y adjuntos/CSV, sin versión inventada. MUST NOT presentar parcial como completa ni incluir secretos ni firmas de webhook.

#### Scenario: Snapshot con los cinco dominios y metadatos de alcance
- **GIVEN** adaptadores disponibles para identidad, agenda, clínico, financiero y auditoría
- **WHEN** se genera el snapshot de exportación
- **THEN** el resultado contiene los cinco dominios, `generadoEn`, la zona horaria aplicada, el alcance y un conteo por dominio, y se clasifica como `COMPLETA`

#### Scenario: Adaptadores por defecto sin excepción bloqueante
- **GIVEN** adaptadores por defecto de `ClinicalExportPort` y `FinancialExportPort` sin C-13/C-15 disponibles
- **WHEN** se genera una exportación
- **THEN** los adaptadores devuelven colecciones vacías sin lanzar excepción y el core completa el ciclo de exportación, respaldo y restore

#### Scenario: Exportación PARCIAL marcada explícitamente
- **GIVEN** una exportación generada con dominios `clinical` y `financial` no integrados
- **WHEN** se inspecciona el resultado
- **THEN** está marcada con `partialExport: true` y `unintegratedModules` con esos dominios, y nunca se presenta como `COMPLETA`

#### Scenario: Artefacto sin secretos ni firmas de webhook
- **GIVEN** un snapshot generado con datos financieros y de notificaciones
- **WHEN** se inspecciona el artefacto exportado
- **THEN** no contiene credenciales, tokens, secretos ni firmas de webhook, y conserva solo identificadores y datos de negocio permitidos

#### Scenario: Exportación JSON con `schemaVersion`
- **GIVEN** una exportación estructurada solicitada en formato `JSON`
- **WHEN** se genera el artefacto
- **THEN** el contrato incluye el campo `schemaVersion` legible por máquina, sin asignar un número de versión inventado

#### Scenario: Paquete ZIP con `manifest.json`, SHA-256 y adjuntos/CSV resumido
- **GIVEN** una exportación solicitada en formato `ZIP`
- **WHEN** se genera el paquete
- **THEN** el paquete es autocontenido e incluye `manifest.json`, checksum SHA-256 y adjuntos o CSV resumido

### Requirement: Copia de historia clínica con versionado, FDI, firma y plazo de 48 horas corridas
La copia SHALL conservar anamnesis versionada, odontograma FDI con temporales y estado, evoluciones con firma y matrícula, y consentimientos, sin sobrescribir la historia (RN-CL-01/02/03). El plazo SHALL ser de 48 horas corridas desde la solicitud, en `America/Argentina/Buenos_Aires`, sin feriados (IN-05/IN-06 cerrados), con alerta a las 36 h por `OpsAlertPort`. MUST NOT reportar como completa una copia con evolución sin firma o matrícula.

#### Scenario: Copia fiel de la historia versionada
- **GIVEN** una ficha con anamnesis versionada, odontograma FDI con pieza temporal, evolución firmada con matrícula y consentimiento registrado
- **WHEN** se genera la copia de historia
- **THEN** la copia conserva cada versión, la notación FDI con carácter temporal y estado, la firma y matrícula de la evolución y el consentimiento, sin alterar el original

#### Scenario: Copia dentro de las 48 horas corridas en `America/Argentina/Buenos_Aires`
- **GIVEN** una solicitud del titular/consultorio y su marca de tiempo interpretada en `America/Argentina/Buenos_Aires`
- **WHEN** se evalúa la copia
- **THEN** se considera dentro de plazo si han transcurrido 48 horas corridas o menos, sin aplicar feriados ni cómputo de días hábiles

#### Scenario: Alerta preventiva a las 36 horas por `OpsAlertPort`
- **GIVEN** una solicitud de copia sin resolver y 36 horas corridas transcurridas desde ella
- **WHEN** se evalúa el SLA de alerta preventiva
- **THEN** se emite una alerta por `OpsAlertPort` con la correlación de la solicitud, antes de vencer el plazo de 48 horas

#### Scenario: Evolución sin firma o matrícula
- **GIVEN** una ficha con una evolución que carece de firma o matrícula
- **WHEN** se intenta emitir la copia como completa
- **THEN** la emisión se rechaza y no se reporta una copia completa, aunque la exportación general sea parcial

### Requirement: Respaldo programado con BackupStoragePort, cifrado, integridad y retención
El sistema SHALL respaldar diariamente a las 03:00 (`BACKUP_CRON="0 0 3 * * *"`) y retener 10 años (`EXPORT_RETENTION_YEARS=10`) sin borrado físico. SHALL usar `BackupStoragePort` por `tenant_id`, con AES-256 en reposo y SHA-256, implementación local/en memoria para tests y contrato para Object Storage en región Argentina sin subprocesadores no declarados. Los valores aprobados SHALL figurar solo en artefactos; el fallo SHALL publicar `BACKUP_FAILED` y alertar por `OpsAlertPort`.

#### Scenario: Respaldo diario a las 03:00 según `BACKUP_CRON`
- **GIVEN** el valor aprobado `BACKUP_CRON="0 0 3 * * *"` interpretado en `America/Argentina/Buenos_Aires`
- **WHEN** se dispara la programación configurada
- **THEN** se ejecuta un respaldo a las 03:00 de cada día y se registra su resultado

#### Scenario: Almacenamiento aislado por `tenant_id` bajo `BackupStoragePort`
- **GIVEN** respaldos de dos tenants distintos
- **WHEN** se almacenan y consultan mediante `BackupStoragePort`
- **THEN** cada artefacto queda aislado por `tenant_id` y un tenant no puede leer el artefacto del otro

#### Scenario: Cifrado AES-256 en reposo y verificación SHA-256
- **GIVEN** un respaldo persistido mediante `BackupStoragePort`
- **WHEN** se inspecciona el almacenamiento y se valida su integridad
- **THEN** el artefacto está cifrado AES-256 en reposo y su checksum SHA-256 coincide con el registrado

#### Scenario: Retención de 10 años parametrizada sin borrado físico
- **GIVEN** el valor aprobado `EXPORT_RETENTION_YEARS=10` leído de properties
- **WHEN** se aplica la política de retención
- **THEN** el respaldo se conserva durante diez años configurables y no se ejecuta borrado físico de datos de usuario

#### Scenario: Fallo de respaldo con `BACKUP_FAILED` y alerta por `OpsAlertPort`
- **GIVEN** un respaldo programado que falla por indisponibilidad del destino
- **WHEN** se detecta el fallo
- **THEN** se emite una alerta por `OpsAlertPort`, se publica `BACKUP_FAILED` con el motivo y no se afirma que el respaldo se completó

#### Scenario: Destino local/en memoria para tests y contrato listo para Object Storage en región Argentina
- **GIVEN** el puerto `BackupStoragePort` en un test con implementación local/en memoria
- **WHEN** se ejecuta el ciclo de respaldo y restore
- **THEN** el ciclo funciona sin servicios externos y el mismo contrato es aplicable a Object Storage en región Argentina

#### Scenario: Sin subprocesadores no declarados
- **GIVEN** la lista de destinos y proveedores usados por `BackupStoragePort`
- **WHEN** se revisa la configuración y la documentación de este change
- **THEN** no aparece ningún proveedor externo no declarado y no se instala ni configura servicio externo nuevo

### Requirement: Restore en entorno aislado con verificación previa
El sistema SHALL ejecutar restauraciones solo en un entorno aislado y SHALL verificar, antes de escribir datos de usuario, la cadena de integridad de C-05 y la coincidencia de conteos con el snapshot; la verificación SHALL publicar `RESTORE_DRY_RUN_VERIFIED`.
Ante cadena inválida, conteos divergentes o destino de producción, MUST abortar sin escritura; MUST NOT presentar un restore no verificado como exitoso ni dejar que una exportación parcial oculte esos fallos.

#### Scenario: Restore verificado en entorno aislado
- **GIVEN** un artefacto válido, la cadena íntegra de C-05 y conteos iguales a los del snapshot
- **WHEN** se ejecuta el restore en el entorno aislado
- **THEN** la restauración finaliza con éxito, publica `RESTORE_EXECUTED` y el registro de auditoría queda encadenado

#### Scenario: Cadena de auditoría inválida
- **GIVEN** un artefacto de respaldo cuya cadena de auditoría falla la verificación
- **WHEN** se intenta el restore
- **THEN** la operación aborta antes de escribir y no se modifica ningún dato de usuario

#### Scenario: Conteos divergentes
- **GIVEN** un artefacto con conteos que no coinciden con los esperados del snapshot
- **WHEN** se intenta el restore
- **THEN** la operación aborta antes de escribir y se informa el dominio con divergencia

#### Scenario: Destino de producción rechazado
- **GIVEN** una solicitud de restore cuyo destino es el entorno de producción de datos de usuario
- **WHEN** se valida el destino
- **THEN** la solicitud se rechaza antes de abrir conexión de escritura y no se realiza ninguna restauración

### Requirement: Superficie segura y secretos solo por nombre
La configuración SHALL referenciar variables de entorno solo por nombre en el repositorio; los valores aprobados de `BACKUP_CRON` y `EXPORT_RETENTION_YEARS` aparecen solo en artefactos y `.env.example` no se modifica.
El change MUST NOT introducir literales de seguridad fuera de `auth`, abrir rutas `/audit` ni `/auditoria`, crear endpoints REST de auditoría ni permisos nuevos, ni reabrir C-04 o C-05.

#### Scenario: Configuración con nombres de variables
- **GIVEN** la configuración del change revisada antes del apply
- **WHEN** se inspeccionan las variables declaradas en el repositorio
- **THEN** aparecen solo nombres como `BACKUP_CRON` y `EXPORT_RETENTION_YEARS`, sin valores de cron, credenciales ni secretos

#### Scenario: Sin literales de seguridad en la superficie de agenda
- **GIVEN** el código de la superficie de agenda y el proveedor de actor actual
- **WHEN** se ejecutan las guardias correspondientes
- **THEN** no aparecen `Authorization`, `Bearer`, `SecurityContext`, `@RequestHeader` ni `/auth` fuera de `auth`

#### Scenario: Sin superficie HTTP nueva de auditoría
- **GIVEN** los controladores y la configuración de rutas del aplicativo
- **WHEN** se enumeran las rutas expuestas
- **THEN** no existe `/audit`, `/auditoria`, endpoint REST de auditoría ni permiso de lectura nuevo introducido por este change
