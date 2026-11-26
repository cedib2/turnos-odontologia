# export-backup-restore Specification

## Purpose

Define la evidencia observable de exportación completa y por paciente, respaldo cifrado en reposo y restore probado en un entorno aislado, con un registro de operaciones inmutable en la base de datos y trazabilidad append-only en la auditoría (C-05). La implementación vigente es `application/cumplimiento` sobre la tabla `cumplimiento_operaciones` (V030); la implementación alternativa del paquete `export/` se retiró al unificar C-17 (V032 elimina su tabla).

## Requirements

### Requirement: Registro de operaciones de cumplimiento
El sistema SHALL registrar cada exportación, respaldo y restore con tipo, alcance, estado, actor, fecha y checksum SHA-256. Una operación MUST transicionar solo de `SOLICITADA` a `COMPLETADA` o `FALLIDA`; los estados finales MUST NOT modificarse ni borrarse, y la restricción SHALL existir en la base de datos.

#### Scenario: Operación cerrada inmutable
- **GIVEN** un respaldo `COMPLETADA`
- **WHEN** se intenta actualizar su checksum o borrar la fila
- **THEN** la base de datos rechaza la sentencia

### Requirement: Exportación completa y copia por paciente
El sistema SHALL generar una exportación completa de los datos y una copia por paciente, cifradas en reposo. La copia por paciente SHALL incluir cada tabla alcanzable desde el paciente por sus claves (turno, ficha clínica, presupuesto o seña) y la historia clínica vigente que provee C-11/C-13; las filas sin un vínculo confirmado con el paciente MUST NOT incluirse. El plazo de entrega SHALL calcularse solo si está configurado `(TBD, IN-05)`. La exportación MUST NOT incluir hashes de contraseña ni tablas de sesiones, refresh tokens o tokens públicos. Cada solicitud, resultado y descarga SHALL quedar en el log append-only.

#### Scenario: Exportación completa sin credenciales
- **GIVEN** datos de pacientes, turnos y usuarios
- **WHEN** se exporta con alcance `COMPLETA` y se descarga
- **THEN** el contenido incluye pacientes, turnos y auditoría, excluye `auth_*` y `hash_password`, y el artefacto en disco no contiene datos personales en claro

#### Scenario: Copia de un paciente
- **GIVEN** dos pacientes con turnos, historia clínica, presupuesto, seña y notificaciones
- **WHEN** se exporta con alcance `PACIENTE` para uno de ellos
- **THEN** la copia contiene solo sus datos: paciente, turnos, historia clínica con sus versiones y firmas (C-11/C-12), presupuestos, señas y comprobantes (C-14/C-15/C-16) y notificaciones (C-10), más la vista vigente de la historia como anexo (C-13); no incluye el log de auditoría ni datos de otros pacientes

#### Scenario: Solicitud vencida
- **GIVEN** un plazo configurado y una solicitud sin completar
- **WHEN** se consultan las solicitudes vencidas después del plazo
- **THEN** la solicitud figura como vencida

### Requirement: Respaldo cifrado con alerta ante fallo
El sistema SHALL respaldar la base completa en una instantánea consistente y cifrada. Sin clave de cifrado configurada el respaldo MUST fallar sin escribir artefactos; todo fallo SHALL registrarse, auditarse y notificarse por el puerto de alerta. La programación SHALL activarse solo si `BACKUP_CRON` está definido.

#### Scenario: Fallo de respaldo
- **GIVEN** que no hay clave de cifrado configurada
- **WHEN** se ejecuta un respaldo
- **THEN** la operación queda `FALLIDA`, sin destino, con evento `respaldo.fallido` y una alerta emitida

### Requirement: Restore probado en entorno aislado
El sistema SHALL restaurar un respaldo solo en una base aislada, verificar el checksum del artefacto y comparar fila por fila lo restaurado con el respaldo. MUST rechazar el restore si la base aislada es la productiva, sin modificar datos de usuario.

#### Scenario: Restore verificado
- **GIVEN** un respaldo completado
- **WHEN** se restaura en el entorno aislado
- **THEN** cada tabla coincide con el respaldo, la operación queda `COMPLETADA` y se audita `restore.verificado`

#### Scenario: Artefacto alterado
- **GIVEN** un respaldo cuyo archivo fue modificado en el destino
- **WHEN** se intenta restaurar
- **THEN** la operación queda `FALLIDA` y se audita `restore.fallido`

#### Scenario: Entorno aislado igual a la base productiva
- **GIVEN** un entorno aislado configurado con la misma base productiva
- **WHEN** se intenta restaurar
- **THEN** el restore se rechaza antes de escribir y los datos productivos permanecen intactos
