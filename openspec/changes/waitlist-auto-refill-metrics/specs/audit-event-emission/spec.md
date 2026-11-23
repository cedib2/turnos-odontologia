# Spec Delta

## ADDED Requirements

### Requirement: Eventos de auditoría de waitlist

El sistema SHALL emitir y persistir `waitlist.creado`, `waitlist.ofrecido`, `waitlist.adjudicado` y `waitlist.expirado` con entidad `lista_espera`. Alta y ofrecimiento SHALL usar recepción `SIMULADO`; adjudicación y expiración SHALL usar `system` en modo `SYSTEM`. Cada operación confirmada SHALL generar una sola fila; rechazos y reenvíos MUST NOT generar otra. El registro MUST NOT utilizarse como disparador del relleno.

#### Scenario: Alta y ofrecimiento

- **GIVEN** un alta de waitlist confirmada y un hueco ofrecido al candidato
- **WHEN** se consulta el registro de eventos
- **THEN** se encuentran `waitlist.creado` y `waitlist.ofrecido`, uno por operación, con entidad `lista_espera` y modo `SIMULADO`

#### Scenario: Adjudicación y expiración

- **GIVEN** una oferta adjudicada y otra expirada sin pago en plazo
- **WHEN** se consulta el registro de eventos
- **THEN** se persisten `waitlist.adjudicado` y `waitlist.expirado` con actor `system` en modo `SYSTEM`

#### Scenario: Reenvío o rechazo sin fila adicional

- **GIVEN** una adjudicación ya registrada, su reenvío y una oferta rechazada
- **WHEN** se procesan esas tres peticiones
- **THEN** no se inserta ninguna fila adicional de waitlist

### Requirement: Eventos de auditoría de inasistencia y métricas

El sistema SHALL emitir y persistir `turno.inasistencia` con entidad `turno` y `metrica.publicada` con entidad `metrica`. La inasistencia SHALL usar recepción `SIMULADO`; la publicación SHALL usar `system` en modo `SYSTEM`. Cada operación confirmada SHALL generar una sola fila; un cálculo sin metodología o período MUST NOT emitir evento.

#### Scenario: Inasistencia y métrica publicada

- **GIVEN** una inasistencia registrada por recepción y una métrica calculada para el período
- **WHEN** se consulta el registro de eventos
- **THEN** se persisten `turno.inasistencia` con entidad `turno` y `metrica.publicada` con entidad `metrica`

#### Scenario: Publicación incompleta sin evento

- **GIVEN** un cálculo sin metodología ni período
- **WHEN** se intenta publicar
- **THEN** no se persiste `metrica.publicada` ni ninguna otra fila de métricas

### Requirement: Metadata de eventos de waitlist y métricas

La metadata SHALL limitarse a `entradaId`, `turnoId`, `seniaId`, `metricaId`, `sillonId`, `periodo`, `ocupacion`, `ausentismo`, `metodologia` como texto, `motivo` cuando corresponda y `correlationId`. MUST NOT incluir DNI, teléfono, email, diagnóstico, historia clínica, credenciales ni secretos. Cada evento SHALL llevar un `correlationId` distinto por operación.

#### Scenario: Metadata permitida

- **GIVEN** una adjudicación con turno, seña y métrica asociados
- **WHEN** se inspecciona la metadata persistida
- **THEN** contiene solo las claves permitidas, el período `YYYY-MM` y un `correlationId` propio de la operación

#### Scenario: Sin datos personales ni secretos

- **GIVEN** una operación que conoce datos del paciente y credenciales del proveedor
- **WHEN** se serializa el evento
- **THEN** no aparecen DNI, teléfono, email, diagnóstico, historia clínica, credenciales ni secretos

#### Scenario: Correlación por operación

- **GIVEN** dos operaciones independientes de waitlist
- **WHEN** se emiten y persisten sus eventos
- **THEN** cada uno conserva un `correlationId` distinto
