# Spec Delta

## ADDED Requirements

### Requirement: Adjudicación de hueco liberado desde la waitlist

El sistema SHALL permitir reocupar un turno cancelado cuya ocupación esté desactivada mediante el motor de waitlist, creando la ocupación por el camino de servicio ya existente y validando disponibilidad con las restricciones de solapamiento vigentes. Una misma liberación SHALL adjudicarse como máximo una vez; un reenvío o segunda adjudicación MUST NOT crear otra ocupación ni otro turno. Queda prohibido insertar directamente en `turnos`, `bloqueos` u `ocupaciones`.

#### Scenario: Adjudicación de un hueco liberado

- **GIVEN** un turno cancelado con su ocupación desactivada y un candidato elegible en la waitlist
- **WHEN** se adjudica el hueco liberado
- **THEN** se crea una sola ocupación activa para el candidato y el turno queda confirmado en la agenda

#### Scenario: Reenvío sin doble adjudicación

- **GIVEN** un hueco ya adjudicado a un candidato
- **WHEN** se repite la adjudicación con la misma clave
- **THEN** no se crea otra ocupación ni otro turno y la operación se rechaza sin efectos

#### Scenario: Conflicto de solapamiento sin efecto

- **GIVEN** un hueco liberado que vuelve a estar ocupado por otra operación
- **WHEN** se intenta adjudicar al candidato
- **THEN** la operación se rechaza, no se escribe en la agenda y no se emite evento de adjudicación
