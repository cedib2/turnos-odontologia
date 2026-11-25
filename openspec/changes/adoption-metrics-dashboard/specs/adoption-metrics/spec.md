# Spec Delta

## Purpose

Medir la adopción online y las métricas de cumplimiento con metodología y período explícitos (C-01 §E, DD-06, SU-03), sin cifras decorativas y con auditoría de cada consulta.

## ADDED Requirements

### Requirement: Metodología y período obligatorios
Toda métrica SHALL publicarse con definición, fórmula, fuente, meta y período `[desde, hasta)`. Un resultado sin metodología o sin período MUST NOT construirse, y un período inválido MUST responderse con error sin auditar.

#### Scenario: Metodología incompleta
- **GIVEN** una metodología sin fórmula o sin fuente
- **WHEN** se intenta construir el resultado
- **THEN** el sistema lo rechaza

#### Scenario: Período inválido
- **GIVEN** una consulta sin `desde`/`hasta` o con `desde >= hasta`
- **WHEN** se consulta `GET /metricas/adopcion`
- **THEN** responde 400 y no registra evento de auditoría

### Requirement: Adopción online por canal de creación
El sistema SHALL calcular `100 × turnos con token público de creación / total de turnos creados` contando turnos por `created_at` dentro del período, incluidos los luego desactivados.

#### Scenario: Conteo por canal y período
- **GIVEN** dos turnos con token público y dos sin token creados dentro del período, y otros fuera de él
- **WHEN** se consulta el período
- **THEN** adopción online es `CALCULADA` con numerador 2, denominador 4 y 50.00 %

#### Scenario: Período sin turnos
- **GIVEN** un período sin turnos creados
- **WHEN** se consulta
- **THEN** adopción online es `SIN_DATOS` y no publica porcentaje

### Requirement: Fuentes pendientes sin cifra
Una métrica cuya fuente todavía no existe SHALL informarse `NO_DISPONIBLE`, sin porcentaje y nombrando el change del que depende.

#### Scenario: Ventana, confirmaciones y dossier pendientes
- **GIVEN** que C-09, C-10 y C-18 no registraron su `FuenteMetrica`
- **WHEN** se consulta el tablero
- **THEN** esas tres métricas son `NO_DISPONIBLE` con dependencia C-09, C-10 y C-18 respectivamente

### Requirement: Auditoría de cada consulta
Cada consulta válida SHALL publicar `metricas.consultadas` con actor, período y estado de cada métrica.

#### Scenario: Consulta auditada
- **GIVEN** un actor de staff
- **WHEN** consulta el tablero
- **THEN** existe un evento `metricas.consultadas` con su actor, `desde`, `hasta` y el estado por métrica
