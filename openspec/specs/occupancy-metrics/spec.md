# occupancy-metrics Specification

## Purpose

Define el cálculo auditable de la ocupación y el ausentismo por sillón y período mensual, con metodología explícita en `America/Argentina/Buenos_Aires` y publicación condicionada a metodología y período.

## Requirements

### Requirement: Cálculo de la ocupación por sillón y período

El sistema SHALL calcular la ocupación como `100 × minutos ocupados / minutos disponibles` para cada sillón y período mensual `YYYY-MM`, usando `turnos` activos y `bloqueos` activos como minutos ocupados y el catálogo de horarios y feriados como minutos disponibles. Los bloqueos SHALL contar como agenda ocupada. El denominador cero SHALL producir sin valor de ocupación y no una cifra inventada.

#### Scenario: Ocupación con bloqueos

- **GIVEN** un sillón con 600 minutos disponibles, 300 minutos de turnos y 120 de bloqueos en el período
- **WHEN** se calcula la ocupación
- **THEN** el valor es 70 con su metodología y período asociados

#### Scenario: Sin minutos disponibles

- **GIVEN** un sillón sin horarios operativos en el período
- **WHEN** se calcula la ocupación
- **THEN** no se publica cifra alguna y el evento queda sin `metrica.publicada`

### Requirement: Cálculo del ausentismo por sillón y período

El sistema SHALL calcular el ausentismo como `100 × turnos con inasistencia / turnos confirmados` para cada sillón y período mensual. La inasistencia SHALL registrarse explícitamente con `turno.inasistencia`; los turnos cancelados, dentro o fuera de la ventana de 48 hs hábiles, MUST NOT entrar en el numerador.

#### Scenario: Inasistencia registrada

- **GIVEN** 20 turnos confirmados y 3 inasistencias registradas en el período
- **WHEN** se calcula el ausentismo
- **THEN** el valor es 15 con su metodología y período asociados

#### Scenario: Cancelados fuera del numerador

- **GIVEN** 20 turnos confirmados, 2 cancelados y 1 inasistencia registrada
- **WHEN** se calcula el ausentismo
- **THEN** el valor es 5 y los cancelados no cuentan como inasistencia

### Requirement: Metodología obligatoria y publicación condicionada

El sistema SHALL persistir cada métrica con `sillon_id`, `periodo`, `ocupacion`, `ausentismo`, `metodologia` no vacía con fórmula y fuente y `calculada_en`, y SHALL publicar la cifra solo con metodología y período explícitos. La meta inicial permanece `(TBD) -> forma de obtención` (baseline del piloto y aprobación del PO Parte 3); el sistema MUST NOT publicar cifras sin metodología, período o con una meta inventada.

#### Scenario: Publicación auditable

- **GIVEN** una métrica calculada con fórmula, fuente, sillón y período
- **WHEN** se consulta la superficie de métricas
- **THEN** se devuelve la cifra junto con su metodología, período y evento `metrica.publicada`

#### Scenario: Metodología ausente

- **GIVEN** un cálculo sin metodología o sin período
- **WHEN** se intenta publicar
- **THEN** se rechaza la publicación, no se persiste la fila y no se emite evento

#### Scenario: Meta no definida

- **GIVEN** el período de piloto sin meta numérica aprobada
- **WHEN** se consulta la superficie de métricas
- **THEN** se devuelven cifra, metodología y período sin comparación contra una meta
