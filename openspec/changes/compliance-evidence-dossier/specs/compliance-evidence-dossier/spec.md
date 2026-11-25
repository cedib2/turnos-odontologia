# Spec Delta (propuesta)

## ADDED Requirements

### Requirement: Dossier con pendientes explícitos
El sistema SHALL componer el dossier con todos los ítems exigidos, marcando cada uno `PRESENTE` o `PENDIENTE`. Si falta algún ítem, el dossier MUST quedar `INCOMPLETO` y listar los pendientes; MUST NOT presentarse como completo.

#### Scenario: Dossier completo
- **GIVEN** todos los ítems exigidos presentes y verificables
- **WHEN** se compone el dossier
- **THEN** su estado es `COMPLETO_SIN_FIRMA` o `COMPLETO_FIRMADO` según exista owner legal

#### Scenario: Evidencia faltante bloquea la publicación como completo
- **GIVEN** un ítem exigido sin evidencia
- **WHEN** se compone el dossier
- **THEN** el estado es `INCOMPLETO` y el ítem figura como pendiente

### Requirement: Subprocesadores sin cláusulas bloqueados
Un subprocesador sin cláusulas AAIP 198/2023 publicadas MUST NOT figurar como cubierto en el dossier.

#### Scenario: Subprocesador sin cláusulas
- **GIVEN** un subprocesador con `clausulas_publicadas=false`
- **WHEN** se compone el dossier
- **THEN** figura como bloqueado y el dossier queda `INCOMPLETO`

### Requirement: Descarga auditada
Cada descarga del dossier SHALL publicar `dossier.descargado` con actor y estado del dossier.

#### Scenario: Trazabilidad de descargas
- **GIVEN** un administrador autorizado
- **WHEN** descarga el dossier
- **THEN** existe el evento `dossier.descargado` con su actor y el estado descargado
