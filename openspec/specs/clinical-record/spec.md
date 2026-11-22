# clinical-record Specification

## Purpose
Ficha clínica por paciente con anamnesis mínima versionada, odontograma FDI con piezas temporales, evoluciones y consentimientos, sin sobrescritura y con trazabilidad en auditoría (C-11; RN-CL-01, RN-CL-02, C-01 C4).

## Requirements

### Requirement: Nomenclatura FDI con piezas temporales
El sistema SHALL sembrar el catálogo FDI con 32 piezas permanentes (cuadrantes 1-4, posiciones 1-8) y 20 temporales (cuadrantes 5-8, posiciones 1-5) y MUST rechazar cualquier registro de odontograma con una pieza fuera de ese catálogo.

#### Scenario: Pieza temporal válida
- **WHEN** se registra la pieza 55
- **THEN** se acepta y la vista la marca como temporal

#### Scenario: Código fuera de FDI
- **WHEN** se registra la pieza 19, 56 o 91
- **THEN** se rechaza sin escribir

### Requirement: Historia versionada sin sobrescritura
Cada anamnesis, registro de odontograma (por pieza y cara), evolución y consentimiento SHALL guardarse como versión nueva; la anterior se conserva. La base MUST impedir `UPDATE`/`DELETE` sobre esos registros y el borrado de fichas (borrado físico `(TBD)`, RN-CL-05). Modificar una evolución crea la versión siguiente enlazada por `reemplaza_id`, sin ramas paralelas.

#### Scenario: Intento de sobrescribir
- **GIVEN** la pieza 36 mesial registrada como caries
- **WHEN** se registra obturada
- **THEN** existe la versión 2 vigente y la versión 1 sigue guardada

### Requirement: Ficha única abierta desde el turno
El sistema SHALL mantener una ficha por paciente; abrirla desde un turno exige que el turno sea del mismo paciente y es idempotente.

#### Scenario: Turno de otro paciente
- **WHEN** se abre la ficha de un paciente con el turno de otro
- **THEN** se rechaza

### Requirement: Permisos por rol y auditoría
Las escrituras SHALL exigir `ficha_clinica:crear` y las lecturas `ficha_clinica:leer` según `rol_permiso`; solo actores de staff (`REAL`/`SIMULADO`) operan por esta vía. Cada denegación se audita (`clinico.acceso.denegado`) y cada cambio emite su evento con `historiaVersion`, sin contenido clínico en la metadata.

#### Scenario: Rol sin permiso
- **WHEN** un auditor intenta registrar, o el actor de sistema o un paciente intentan leer
- **THEN** se deniega y queda auditado
