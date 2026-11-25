# Design — C-18

## Context
C-01 D=b aprobó un **dossier mínimo con pendientes explícitos**. La evidencia proviene de dos fuentes: lo que el sistema puede comprobar solo (C-04, C-05, C-17) y documentación legal que solo una persona puede aportar.

## Decisiones

### 1. Catálogo de ítems en base de datos (V031)
`dossier_items` siembra los 12 ítems exigidos (RN-SE-04/05, C-01 D) como `PENDIENTE`, con `base_normativa` y `orden`. Los `CHECK` garantizan que un ítem `PRESENTE` tenga referencia, verificador y fecha, y que un ítem `AUTOMATICA` nunca quede marcado a mano. Alternativa descartada: lista en código, porque no deja rastro de quién verificó cada evidencia.

### 2. Evaluación automática al generar
`DossierService.generar()` evalúa cada ítem `AUTOMATICA` en el momento:
- `ROLES_RBAC`: todos los roles activos tienen al menos un permiso.
- `LOG_AUDITORIA_INTEGRO`: `AuditoriaIntegridadService.verificar()` (C-05) da una cadena válida y no vacía.
- `EXPORTACION_COMPLETA` y `RESPALDO_RESTORE_VERIFICADO`: última operación `COMPLETADA` en `cumplimiento_operaciones` (C-17).
- `CLAUSULAS_SUBPROCESADORES`: hay al menos un subprocesador activo y todos tienen cláusulas AAIP 198/2023.

No se persiste el resultado: un dossier viejo no puede afirmar evidencia que ya no se cumple.

### 3. Descarga vs. publicación
- **Descarga** (`dossier.descargado`): permitida aunque esté incompleto, con los pendientes listados (D=b).
- **Publicación**: bloqueada con `DossierIncompletoException` (HTTP 409) si queda algún pendiente obligatorio. El evento `dossier.publicacion_bloqueada` se registra en su propia transacción antes de lanzar la excepción, así el intento queda auditado aunque la operación falle.

### 4. Completitud como métrica auditable
`100 × presentes / exigidos`, con fórmula, metodología y meta `(TBD)` (C-01 E). C-22 la reutiliza como métrica «Evidencia de cumplimiento».

## Riesgos y límites
- La evidencia manual es una **referencia** (expediente, URL, archivo), no el documento: su autenticidad la valida quien la registra, cuyo nombre queda guardado.
- Hasta designar al owner legal (C-01 D), el dossier no se puede publicar.
