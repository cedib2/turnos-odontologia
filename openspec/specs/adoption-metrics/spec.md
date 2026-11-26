# adoption-metrics Specification

## Purpose

Define el tablero de adopción y cumplimiento (C-22): métricas auditables con definición, fórmula, fuente, zona horaria y período explícitos, sin cifras decorativas (DD-06, C-01 E). Cubre la adopción de la reserva online, el cumplimiento de la ventana de 48 hs hábiles, las confirmaciones por WhatsApp (C-10), la ocupación y el ausentismo publicados por C-19 y la completitud del dossier (C-18). Cada consulta queda en el log append-only.

## Requirements

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
El sistema SHALL medir la anticipación de cada reprogramación o cancelación contra el inicio original del turno en horas hábiles (lunes a viernes, sin feriados, zona `America/Argentina/Buenos_Aires`) y SHALL informar los eventos excluidos por falta de inicio original. Las reprogramaciones del staff (C-08) y las cancelaciones y reprogramaciones por link (C-09) MUST informar `inicioAnterior` en la metadata del evento.

#### Scenario: Feriado dentro de la ventana
- **GIVEN** un lunes feriado y dos reprogramaciones hechas un viernes para turnos del martes y del miércoles
- **WHEN** se consulta la ventana
- **THEN** solo la del miércoles cuenta como dentro de las 48 hs hábiles

#### Scenario: Cancelación por link
- **GIVEN** un paciente que cancela o reprograma su turno con el token de gestión
- **WHEN** se emite `turno.cancelado` o `turno.reprogramado`
- **THEN** la metadata incluye `inicioAnterior` con el inicio del turno previo al cambio

### Requirement: Confirmaciones por WhatsApp
El sistema SHALL calcular `100 × turnos con confirmación respondida / turnos con solicitud de confirmación enviada` a partir del outbox de C-10: cuenta las notificaciones de propósito `CONFIRMACION` aceptadas por el proveedor (`proveedor_message_id` no nulo) y programadas en el período, y como confirmadas las de estado `CONFIRMADA`, con desglose por estado.

#### Scenario: Solicitud no enviada
- **GIVEN** tres turnos con solicitud de confirmación: una confirmada, una entregada sin respuesta y una pendiente sin enviar
- **WHEN** se consulta el tablero
- **THEN** confirmaciones vale 50,00 % con numerador 1 y denominador 2

### Requirement: Ocupación y ausentismo publicados por C-19
El sistema SHALL mostrar la ocupación y el ausentismo tal como C-19 los publica por sillón y mes (`metricas_ocupacion` activas), con la metodología de cada valor, incluyendo los meses de la zona AR que se superponen con el período. MUST NOT agregarlos entre sillones ni recalcularlos: el valor del tablero queda nulo y las cifras van en el detalle.

#### Scenario: Meses fuera del período
- **GIVEN** métricas publicadas por C-19 para el mes en curso y para seis meses atrás
- **WHEN** se consulta el tablero del mes en curso
- **THEN** solo se muestran las del mes en curso, cada una con su sillón, valor y metodología

### Requirement: Tablero sin cifras decorativas
El tablero SHALL devolver adopción, ventana, confirmaciones, ocupación, ausentismo y evidencia de cumplimiento; una métrica sin datos en el período SHALL quedar `SIN_DATOS` y sin valor. Cada consulta SHALL quedar auditada.

#### Scenario: Tablero completo
- **WHEN** se consulta el tablero sin datos de C-10 ni C-19 en el período
- **THEN** confirmaciones, ocupación y ausentismo quedan `SIN_DATOS` y sin valor, y se registra un evento `metricas.consultadas`
