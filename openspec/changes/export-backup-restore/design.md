# Design (propuesta)

## Modelo propuesto (V030)

| Tabla | Campos clave | Constraints |
|---|---|---|
| `exportaciones` | `tipo` (`COMPLETA`, `HISTORIA_PACIENTE`), `paciente_id` nullable, `solicitada_en`, `vence_en`, `completada_en`, `estado` (`SOLICITADA`, `COMPLETADA`, `VENCIDA`, `FALLIDA`), `formato`, `checksum_sha256`, `destino` | `HISTORIA_PACIENTE` exige `paciente_id`; `COMPLETADA` exige checksum |
| `respaldos` | `iniciado_en`, `finalizado_en`, `estado` (`EN_CURSO`, `OK`, `FALLIDO`), `ubicacion`, `cifrado`, `checksum_sha256`, `retener_hasta`, `error` | `OK` exige `cifrado=true` y checksum |
| `restauraciones` | `respaldo_id`, `entorno` (`AISLADO`, `PRODUCCION`), `verificado`, `resultado`, `aprobado_por` | `PRODUCCION` exige un restore `AISLADO` verificado previo del mismo respaldo y `aprobado_por` |

## Componentes propuestos

- `ExportacionService` + puerto `SeccionExportable` (una implementación por dominio: agenda/identidad aquí; clínica desde C-13; financiero desde C-15).
- `RespaldoService` programado por `BACKUP_CRON` + puerto `MotorRespaldo` (implementación concreta `(TBD)`: p. ej. `pg_dump` cifrado; depende del proveedor de B6).
- `RestoreService` que restaura en una base aislada, verifica conteos y la cadena con `AuditoriaIntegridadService` y recién entonces habilita el restore sobre producción.
- Puerto `AlertaAdministracion` (canal `(TBD)`).

## Preguntas para el PO (bloquean el apply)

| # | Pregunta | Propuesta (no aplicada) |
|---|---|---|
| P1 | Plazo de exportación/copia de historia | 48 h corridas, como el informe; ¿o hábiles? |
| P2 | Formato de exportación | JSON por entidad + manifiesto con checksums |
| P3 | Frecuencia de respaldo (`BACKUP_CRON`) | Diario fuera de horario de atención |
| P4 | Retención | 10 años (informe); confirmar base legal con owner legal |
| P5 | Destino y cifrado en reposo | Bucket del proveedor cloud región AR (B6), cifrado del proveedor + checksum |
| P6 | Canal de alerta ante fallo | Email a administración o WhatsApp vía C-10 |
| P7 | ¿Quién aprueba un restore sobre producción? | Rol administrador, con motivo registrado |

## Riesgos

- Sin C-13/C-15 la exportación completa sale con secciones pendientes: debe quedar visible en el manifiesto, nunca omitirse en silencio.
- Los tests de restore necesitan una segunda base PostgreSQL aislada en Testcontainers.
- `AuthMigrationV002Test` (Parte 1) fija `List.of(1, 2, 3, 4, 5, 6)` como versiones aplicadas: V030/V031 obligan a actualizar esa lista, coordinándolo con el dueño de la Parte 1.
