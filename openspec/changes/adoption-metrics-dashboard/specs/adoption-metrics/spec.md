# Spec Delta — adoption-metrics

## ADDED Requirements

### Requirement: Metodología obligatoria
Toda métrica MUST incluir definición, fórmula, fuente, zona horaria y período explícito (`desde` < `hasta`); la meta SHALL ser `(TBD)` hasta el piloto (C-01 E, DD-06). Sin datos en el período MUST NOT publicarse un valor.

#### Scenario: Período ausente
- **GIVEN** una consulta sin período válido
- **WHEN** se pide una métrica
- **THEN** la consulta se rechaza y no se audita ninguna consulta

### Requirement: Adopción de reserva online
El sistema SHALL calcular `100 × turnos creados con token público / turnos creados` en el período, con desglose por `origen`.

#### Scenario: Conteo por canal
- **GIVEN** cuatro turnos creados en el período, dos con token público
- **WHEN** se consulta la adopción
- **THEN** el valor es 50,00 % con numerador 2 y denominador 4

### Requirement: Cumplimiento de la ventana de 48 hs hábiles
El sistema SHALL medir la anticipación de cada reprogramación o cancelación contra el inicio original del turno en horas hábiles (lunes a viernes, sin feriados, zona `America/Argentina/Buenos_Aires`) y SHALL informar los eventos excluidos por falta de inicio original.

#### Scenario: Feriado dentro de la ventana
- **GIVEN** un lunes feriado y dos reprogramaciones hechas un viernes para turnos del martes y del miércoles
- **WHEN** se consulta la ventana
- **THEN** solo la del miércoles cuenta como dentro de las 48 hs hábiles

### Requirement: Tablero sin cifras decorativas
El tablero SHALL informar como `NO_DISPONIBLE` o `DIFERIDA` las métricas cuya fuente aún no existe (confirmaciones, ocupación, ausentismo) y SHALL auditar cada consulta.

#### Scenario: Tablero completo
- **WHEN** se consulta el tablero
- **THEN** devuelve adopción, ventana, confirmaciones (no disponible), ocupación y ausentismo (diferidas) y evidencia de cumplimiento, y registra un evento `metricas.consultadas`
