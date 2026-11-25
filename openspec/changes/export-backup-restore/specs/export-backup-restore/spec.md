# Spec Delta (propuesta)

## ADDED Requirements

### Requirement: Exportación completa trazable
El sistema SHALL generar una exportación completa con manifiesto y checksum SHA-256, registrar su solicitud y su finalización en el log append-only y marcarla `VENCIDA` si no se completa dentro del plazo `(TBD)`. Las secciones cuyo dominio todavía no expone datos MUST figurar como pendientes en el manifiesto.

#### Scenario: Exportación completa
- **GIVEN** un administrador autorizado
- **WHEN** solicita la exportación completa
- **THEN** se genera el archivo con manifiesto y checksum, la exportación queda `COMPLETADA` y existen los eventos de solicitud y finalización

#### Scenario: Plazo vencido
- **GIVEN** una exportación `SOLICITADA` cuyo `vence_en` ya pasó
- **WHEN** corre el control de plazos
- **THEN** queda `VENCIDA` y se audita

### Requirement: Respaldo programado con alerta
El sistema SHALL ejecutar respaldos cifrados según `BACKUP_CRON` y, ante un fallo, MUST registrar `FALLIDO`, alertar a administración y auditar el evento.

#### Scenario: Fallo de respaldo
- **GIVEN** un motor de respaldo que falla
- **WHEN** se ejecuta el respaldo programado
- **THEN** el respaldo queda `FALLIDO`, se emite la alerta y existe el evento `respaldo.fallido`

### Requirement: Restore probado en entorno aislado
El sistema MUST restaurar primero en una base aislada y verificar conteos y cadena de auditoría. Un restore sobre producción MUST rechazarse sin un restore aislado verificado del mismo respaldo y sin aprobación registrada.

#### Scenario: Restore aislado verificado
- **GIVEN** un respaldo `OK`
- **WHEN** se restaura en el entorno aislado
- **THEN** la verificación de conteos y de la cadena de auditoría es exitosa y queda registrada

#### Scenario: Restore a producción sin verificación previa
- **GIVEN** un respaldo sin restore aislado verificado
- **WHEN** se intenta restaurar sobre la base de usuario
- **THEN** el sistema lo rechaza y audita el intento
