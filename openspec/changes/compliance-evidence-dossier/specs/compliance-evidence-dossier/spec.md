# Spec Delta — compliance-evidence-dossier

## ADDED Requirements

### Requirement: Dossier mínimo con pendientes explícitos
El sistema SHALL generar un dossier con los ítems exigidos (DPA, RNBD/AAIP, cláusulas AAIP 198/2023, roles, logs, exportación, respaldo/restore, retención, ubicación, owner legal, marco normativo e INTIMA/CONSTA), su estado y la evidencia de cada uno. Los ítems sin evidencia MUST figurar como pendientes; ninguna evidencia SHALL sembrarse por defecto.

#### Scenario: Dossier inicial
- **GIVEN** una instalación nueva
- **WHEN** se descarga el dossier
- **THEN** solo la matriz RBAC figura presente, el resto figura en pendientes y la descarga queda auditada

### Requirement: Evidencia faltante bloquea la publicación
El sistema MUST rechazar la publicación del dossier mientras exista un ítem obligatorio pendiente y SHALL auditar el intento bloqueado.

#### Scenario: Publicación bloqueada
- **GIVEN** ítems obligatorios pendientes
- **WHEN** se intenta publicar
- **THEN** la publicación se rechaza listando los pendientes y se registra `dossier.publicacion_bloqueada`

#### Scenario: Publicación completa
- **GIVEN** toda la evidencia manual registrada, un subprocesador con cláusulas, una exportación completa y un restore verificado
- **WHEN** se publica
- **THEN** el dossier se publica con completitud 100 % y se registra `dossier.publicado`

### Requirement: Subprocesadores sin cláusulas excluidos
El sistema SHALL excluir del dossier a los subprocesadores sin referencia a cláusulas AAIP 198/2023 publicadas y mantener pendiente el ítem correspondiente.

#### Scenario: Subprocesador sin cláusulas
- **GIVEN** un subprocesador con cláusulas y otro sin ellas
- **WHEN** se genera el dossier
- **THEN** solo el primero figura incluido y el ítem de subprocesadores sigue pendiente

### Requirement: Evidencia manual trazable
El registro de evidencia de un ítem manual MUST exigir una referencia no vacía y SHALL guardar verificador y fecha; los ítems automáticos MUST NOT poder marcarse a mano.

#### Scenario: Evidencia manual
- **GIVEN** el ítem `REGISTRO_RNBD_AAIP`
- **WHEN** se registra con una referencia documental
- **THEN** queda presente con verificador y fecha, y se audita `dossier.evidencia_registrada`
