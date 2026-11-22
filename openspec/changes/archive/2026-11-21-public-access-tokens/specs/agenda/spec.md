# Spec Delta

## ADDED Requirements

### Requirement: Superficie pública con token sin credenciales de staff
La superficie pública que consuma tokens de `public-access-tokens` SHALL validar el token sin credenciales de staff, sin `Usuario`/`Rol` de pacientes y sin leer request headers de identidad. Este requisito NO modifica la superficie dev/test de `/agenda`, `/turnos`, `/bloqueos`, `/sobreturnos` ni catálogos, que conserva su semántica actual; los endpoints de negocio públicos se introducen en C-09 y C-13.

#### Scenario: Consumo público sin credenciales de staff
- **GIVEN** un token vigente de C-06 presentado en path o query y ninguna credencial de staff
- **WHEN** la superficie pública valida el token para una operación de reserva o historia
- **THEN** la validación se resuelve por el token sin exigir login, RBAC ni request headers de identidad, y los controladores existentes de agenda y catálogos permanecen sin cambios

#### Scenario: Delta aditivo sin debilitar guardias
- **GIVEN** los 12 requisitos y todos los escenarios actuales de `agenda` y las guardias de C-04
- **WHEN** se inspecciona este delta
- **THEN** permanecen intactos: no hay `MODIFIED` ni `REMOVED`, `AgendaSurfaceTest` y `CurrentActorProviderTest` no se ajustan y `audit-event-emission` sigue en modo `SIMULADO`
