# Spec Delta (propuesta)

> Aplica sobre la capability `adoption-metrics` una vez archivado C-22.

## MODIFIED Requirements

### Requirement: Fuentes pendientes sin cifra
Una métrica cuya fuente todavía no existe SHALL informarse `NO_DISPONIBLE`, sin porcentaje y nombrando el change del que depende. La métrica `evidencia_cumplimiento` SHALL calcularse desde el dossier de C-18 como `100 × ítems presentes y verificables / ítems exigidos`.

#### Scenario: Ventana y confirmaciones pendientes
- **GIVEN** que C-09 y C-10 no registraron su `FuenteMetrica`
- **WHEN** se consulta el tablero
- **THEN** esas dos métricas son `NO_DISPONIBLE` con dependencia C-09 y C-10 respectivamente

#### Scenario: Evidencia de cumplimiento calculada
- **GIVEN** un dossier con 7 de 9 ítems exigidos presentes y verificables
- **WHEN** se consulta el tablero
- **THEN** `evidencia_cumplimiento` es `CALCULADA` con numerador 7, denominador 9 y 77.78 %
