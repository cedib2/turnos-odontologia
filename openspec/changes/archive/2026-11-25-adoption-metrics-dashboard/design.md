# Design — C-22

## Context
C-01 E exige que cada métrica tenga definición, fórmula, fuente y período, con metas `(TBD)` hasta el piloto. Al planificarse, varias fuentes todavía no existían: C-09 (reserva pública y cancelación), C-10 (confirmaciones) y C-19 (ocupación/ausentismo). Las tres quedaron integradas en `demo-integracion` el 2026-11-25 y el tablero las usa (decisiones 6 y 7).

## Decisiones

### 1. `Metrica` no se construye sin metodología
El record valida definición, fórmula, fuente, zona horaria y `desde < hasta`. Una métrica sin datos queda `SIN_DATOS` con `valor` nulo. Los estados `NO_DISPONIBLE` y `DIFERIDA` quedan para fuentes que no existan; hoy ninguna métrica del tablero los usa.

### 2. Adopción online por token público
Numerador: turnos creados en el período con `turnos.token_publico IS NOT NULL`, que es la fuente que nombra C-01 («canal de creación: token público link/QR»). Denominador: todos los turnos creados en el período (`created_at`). El desglose por `origen` se publica porque `origen` es texto libre sin enum decidido, así que no se usa como criterio.

### 3. Ventana de 48 hs hábiles medida contra el inicio original
`CalculadoraHorasHabiles` suma solo horas de lunes a viernes no feriados (calendario C-07) en `America/Argentina/Buenos_Aires` (C-01 C2). La anticipación se mide entre la fecha del evento en `auditoria_log` y `metadata.inicioAnterior`. Por eso `ReprogramacionService` (C-08) agrega ese campo. Los eventos sin `inicioAnterior` se excluyen del denominador y se informan en `excluidosSinInicioAnterior`.

**Contrato con C-09** (cumplido): `turno.cancelado` está en `AuditoriaCatalogo.TIPOS`, y la cancelación y la reprogramación por link incluyen `inicioAnterior` (ISO-8601) en su metadata. **Inconsistencia abierta**: la spec `public-booking` solo acepta cambios por link cuando `now >= vencimiento` (dentro de los últimos 2 días hábiles), mientras esta métrica cuenta como cumplimiento los cambios con 48 hs hábiles o más de anticipación. Con ese comportamiento, los cambios por link nunca cumplen. Hay que confirmar con el PO qué sentido tiene la ventana (RN-AG-05, DD-07).

### 4. Lectura directa sobre tablas y log
Las consultas son SQL de solo lectura sobre `turnos` y `auditoria_log`. No se materializa una tabla `Métrica`: la entidad de ocupación y ausentismo es de C-19 (`metricas_ocupacion`).

### 5. Auditoría de cada consulta
Un evento `metricas.consultadas` por consulta, con los códigos y el período, en su propia transacción. Las consultas rechazadas por período inválido no se auditan porque no devuelven datos.

### 6. Confirmaciones desde el outbox de C-10
Denominador: turnos con una notificación de propósito `CONFIRMACION` aceptada por el proveedor (`proveedor_message_id` no nulo) y programada en el período. Numerador: los de esas notificaciones en estado `CONFIRMADA`, que C-10 asigna cuando el paciente responde con el botón. Se cuentan turnos distintos para que un reintento no duplique.

### 7. Ocupación y ausentismo tal como los publica C-19
C-19 publica un valor por sillón y mes con su metodología. C-22 los muestra en el detalle para los meses (zona AR) que se superponen con el período y deja el valor del tablero nulo: agregarlos entre sillones exigiría una fórmula (ponderar por minutos disponibles o no) que nadie decidió.

## Límites
- Si C-09 marca `token_publico` en turnos que no fueron creados por el paciente, la adopción se sobreestima: el contrato es «token presente ⇔ creado vía link/QR».
- Escala pequeña (C-01 B4): las consultas no usan agregados precalculados.
