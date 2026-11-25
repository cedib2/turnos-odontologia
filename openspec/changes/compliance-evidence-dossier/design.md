# Design (propuesta)

## Ítems exigidos propuestos

| Código | Ítem | Fuente |
|---|---|---|
| `dpa` | DPA responsable/encargado | Documento cargado |
| `rnbd_aaip` | Registro AAIP/RNBD del responsable | Documento cargado |
| `subprocesadores` | Cláusulas AAIP 198/2023 por subprocesador | Tabla `subprocesadores` |
| `roles` | Roles y permisos vigentes | Tablas de C-04 |
| `logs` | Integridad de la cadena de auditoría | `AuditoriaIntegridadService` (C-05) |
| `exportacion` | Última exportación completa | C-17 |
| `respaldo` | Último respaldo con restore aislado verificado | C-17 |
| `retencion` | Política de retención | `(TBD)` — C-17 P4 |
| `marco_normativo` | Aplicabilidad de Ley 25.326, 26.529, Dec. 1089/2012 y Ley 25.506 | `(TBD)` — owner legal |

## Estados del dossier

- `INCOMPLETO`: falta algún ítem o un subprocesador no tiene cláusulas; descargable con pendientes.
- `COMPLETO_SIN_FIRMA`: todos los ítems presentes, sin owner legal designado.
- `COMPLETO_FIRMADO`: requiere owner legal designado.

## Preguntas para el PO (bloquean el apply)

| # | Pregunta |
|---|---|
| Q1 | ¿Quién es el owner legal que firma el dossier? (pendiente de C-01 §D) |
| Q2 | ¿Es cerrada la lista de ítems exigidos de la tabla anterior? (C-01 §Métricas la deja `(TBD)`) |
| Q3 | ¿Se permite descargar el dossier `INCOMPLETO`? Propuesta: sí, con pendientes visibles (D=b) |
| Q4 | ¿Quién puede cargar evidencia y descargar el dossier? Propuesta: administrador |
| Q5 | Procedimiento INTIMA/CONSTA: ¿entra en el dossier o queda fuera? |
| Q6 | Formato de descarga (PDF, ZIP con documentos + manifiesto) |
