# Spec Delta

## ADDED Requirements

### Requirement: Aislamiento de la superficie autenticada
El sistema SHALL mantener `/agenda`, `/turnos`, `/bloqueos`, `/sobreturnos` y catálogos sin autenticación, RBAC ni lectura de identidad en dev/test, mientras `/auth/**` pertenece al paquete de autenticación. La cadena SHALL restringirse a `/auth/**` y MUST NOT proteger estas rutas. C-04 entrega infraestructura RBAC pero aún NO protege la agenda: su protección queda para un change posterior.

#### Scenario: Agenda y catálogos siguen sin autenticación
- **GIVEN** el perfil dev/test activo
- **WHEN** se invocan rutas de agenda o catálogo
- **THEN** responden sin credenciales, sin cabeceras de identidad y con el mismo actor simulado de C-08

#### Scenario: Sin rutas de autenticación en la superficie de agenda
- **GIVEN** los controladores de agenda y catálogo
- **WHEN** se inspeccionan sus mappings y anotaciones
- **THEN** no exponen `/auth/**`, tokens, cookies ni lectura de identidad; esas rutas viven solo en el paquete de autenticación

#### Scenario: Protección de agenda diferida
- **GIVEN** el alcance declarado de C-04 en este delta y en el documento de decisiones
- **WHEN** se inspecciona la declaración de alcance
- **THEN** declara que C-04 entrega infraestructura RBAC pero aún no protege `/agenda`, `/turnos`, `/bloqueos`, `/sobreturnos` ni catálogos, cuya protección queda para un change posterior

#### Scenario: Delta puramente aditivo
- **GIVEN** los 11 requisitos y todos los escenarios actuales de `agenda`
- **WHEN** se inspecciona este delta
- **THEN** permanecen intactos: no hay `MODIFIED` ni `REMOVED`, solo este requisito `ADDED`
