# Design — C-22

## Context
C-01 E exige que cada métrica tenga definición, fórmula, fuente y período, con metas `(TBD)` hasta el piloto. Varias fuentes todavía no existen: C-09 (reserva pública y cancelación), C-10 (confirmaciones) y C-19 (ocupación/ausentismo, diferido a v0.2).

## Decisiones

### 1. `Metrica` no se construye sin metodología
El record valida definición, fórmula, fuente, zona horaria y `desde < hasta`. Una métrica sin datos queda `SIN_DATOS` con `valor` nulo. Las que dependen de fuentes inexistentes quedan `NO_DISPONIBLE` o `DIFERIDA`, con el motivo en `detalle`.

### 2. Adopción online por token público
Numerador: turnos creados en el período con `turnos.token_publico IS NOT NULL`, que es la fuente que nombra C-01 («canal de creación: token público link/QR»). Denominador: todos los turnos creados en el período (`created_at`). El desglose por `origen` se publica porque `origen` es texto libre sin enum decidido, así que no se usa como criterio.

### 3. Ventana de 48 hs hábiles medida contra el inicio original
`CalculadoraHorasHabiles` suma solo horas de lunes a viernes no feriados (calendario C-07) en `America/Argentina/Buenos_Aires` (C-01 C2). La anticipación se mide entre la fecha del evento en `auditoria_log` y `metadata.inicioAnterior`. Por eso `ReprogramacionService` (C-08) agrega ese campo. Los eventos sin `inicioAnterior` se excluyen del denominador y se informan en `excluidosSinInicioAnterior`.

**Contrato con C-09**: la cancelación debe (a) dar de alta `turno.cancelado` en `AuditoriaCatalogo.TIPOS` y (b) incluir `inicioAnterior` (ISO-8601) en su metadata. Hasta entonces la métrica cuenta solo reprogramaciones.

### 4. Lectura directa sobre tablas y log
Las consultas son SQL de solo lectura sobre `turnos` y `auditoria_log`. No se materializa una tabla `Métrica` porque C-19, dueña de esa entidad, está diferido.

### 5. Auditoría de cada consulta
Un evento `metricas.consultadas` por consulta, con los códigos y el período, en su propia transacción. Las consultas rechazadas por período inválido no se auditan porque no devuelven datos.

## Límites
- Si C-09 marca `token_publico` en turnos que no fueron creados por el paciente, la adopción se sobreestima: el contrato es «token presente ⇔ creado vía link/QR».
- Escala pequeña (C-01 B4): las consultas no usan agregados precalculados.
