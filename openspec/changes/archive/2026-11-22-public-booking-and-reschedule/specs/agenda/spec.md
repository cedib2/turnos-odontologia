# Spec Delta

## ADDED Requirements

### Requirement: Ciclo de vida público del turno en la agenda
Para turnos creados desde la superficie pública, el sistema SHALL persistir `estado=reservado` y `origen=link` al crear; la confirmación SHALL cambiar solo `estado` a `confirmado`; la cancelación dentro de ventana SHALL fijar `estado=cancelado` y `activo=false` y desactivar su ocupación en la misma transacción; la reprogramación SHALL conservar `estado` y `origen`. Valores fijados por decisión del PO de C-09 (RN-GL-03); otros orígenes siguen `(TBD)`.

#### Scenario: Alta pública visible en agenda
- **GIVEN** una reserva pública creada con token `RESERVAR`
- **WHEN** se consulta la agenda del profesional
- **THEN** el turno aparece con `estado=reservado`, `origen=link` y su ocupación activa

#### Scenario: Confirmación sin mover el hueco
- **GIVEN** un turno público `reservado`
- **WHEN** se confirma con token `GESTIONAR`
- **THEN** solo cambia su `estado` a `confirmado` y conserva inicio, fin, profesional y sillón

#### Scenario: Cancelación que libera el hueco
- **GIVEN** un turno público activo dentro de la ventana de 48 hs hábiles
- **WHEN** se cancela
- **THEN** quedan inactivos el turno y su ocupación en la misma transacción y el intervalo vuelve a ofrecerse como libre

#### Scenario: Reprogramación que conserva la semántica pública
- **GIVEN** un turno público confirmado o reservado
- **WHEN** se reprograma dentro de ventana
- **THEN** mantiene su `estado` y `origen`, libera el hueco anterior y ocupa el nuevo sin solapamiento

#### Scenario: Delta puramente aditivo
- **GIVEN** los 13 requisitos y todos los escenarios actuales de `agenda`
- **WHEN** se inspecciona este delta
- **THEN** permanecen intactos: no hay `MODIFIED` ni `REMOVED` y las guardias de C-04 y C-06 siguen vigentes
