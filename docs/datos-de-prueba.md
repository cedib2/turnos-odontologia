# Datos de prueba

Juego de datos ficticio para probar cada funcionalidad del MVP de punta a punta en el entorno **dev**.

- Script: [`src/main/resources/db/datos-prueba/V900__datos_de_prueba.sql`](../src/main/resources/db/datos-prueba/V900__datos_de_prueba.sql)
- Se carga **solo con el perfil `dev`**: `application-dev.yml` agrega `classpath:db/datos-prueba` a las ubicaciones de Flyway. Los tests (`application-test.yml`) y producción no lo cargan.
- Carga manual sobre una base que ya tiene V001..V033:

```bash
psql "$DATABASE_URL" -v ON_ERROR_STOP=1 -1 -f src/main/resources/db/datos-prueba/V900__datos_de_prueba.sql
```

- Para regenerar desde cero: recrear la base y volver a aplicar las migraciones con el script. Como la auditoría y la historia clínica son append-only, el script no se puede correr dos veces sobre la misma base.
- **Las fechas de la agenda son relativas al día de carga.** Los turnos van de 20 días hábiles atrás a 15 hacia adelante, sin sábados, domingos ni feriados. Si pasan muchos días desde la carga, hay que regenerar la base para recuperar los escenarios a futuro.
- Todos los datos son inventados: documentos `9000xxxx`, teléfonos `+54911500000xx`, emails `@ejemplo.test` y `@turnos-odontologia.test`, matrículas `*-PRUEBA-*`, `payment_id` `PRUEBA-MP-*`, `wamid.PRUEBA-*` y CAE de prueba.

## Usuarios de staff

Contraseña de todos: **`Prueba1234!`** (`POST /auth/login`)

| Email | Rol | Notas |
|---|---|---|
| `admin@turnos-odontologia.test` | administrador | Todos los permisos |
| `recepcion@turnos-odontologia.test` | recepcionista | |
| `recepcion2@turnos-odontologia.test` | recepcionista | |
| `lgimenez@turnos-odontologia.test` | odontologo | Vinculado a la Dra. Laura Giménez (profesional 1): ve solo su agenda y fichas |
| `msosa@turnos-odontologia.test` | odontologo | Dr. Martín Sosa (profesional 2) |
| `cruiz@turnos-odontologia.test` | odontologo | Dra. Carolina Ruiz (profesional 3) |
| `dfernandez@turnos-odontologia.test` | odontologo | Dr. Diego Fernández (profesional 4) |
| `auditor@turnos-odontologia.test` | auditor | Solo auditoría y configuración |
| `paciente@turnos-odontologia.test` | paciente | Representa a Sofía Martínez (paciente 1) |
| `baja@turnos-odontologia.test` | recepcionista | **Inactivo**: el login debe rechazarse |

## Catálogos

| Entidad | Contenido |
|---|---|
| Sillones | 1, 2 y 3 activos. El **4 está inactivo** y tiene un bloqueo de todo el período |
| Profesionales | 1 Giménez (general), 2 Sosa (ortodoncia), 3 Ruiz (odontopediatría), 4 Fernández (endodoncia), **5 Paz inactiva** |
| Tratamientos | 12 activos de 20 a 120 min (duración variable). El 13 (`PROTESIS-OLD`) está inactivo |
| Horarios | Giménez lun–vie 9–13 y lun–jue 14–18. Sosa mar/jue 10–19 y sáb 9–13. Ruiz lun/mié/vie 8–14. Fernández lun–vie 14–20. El horario 22 está dado de baja |
| Feriados | Nacionales de oct-2026 a jul-2027, más cierres propios (24/12 y 31/12). Uno está dado de baja (25/03/2027) |

Asignación de sillones en la agenda generada: Giménez → 1, Sosa → 2, Ruiz y Fernández → 3 (mañana y tarde).

## Escenarios por funcionalidad

### Agenda y anti-solapamiento (C-07, C-08)
- Unos 517 turnos en estado `reservado`, `confirmado`, `cancelado` e `inasistente`, con orígenes `agenda`, `link`, `recepcion`, `telefono` y `waitlist`.
- Hay huecos libres: alrededor del 25 % de los slots pasados y del 45 % de los futuros.
- **Solapamiento**: crear un turno en el mismo horario de cualquier turno activo del mismo profesional o sillón tiene que fallar por la restricción de exclusión. Un turno cancelado (`activo = false`) libera el hueco.
- **Bloqueos**:
  1. Vacaciones de Giménez (días hábiles +8 y +9).
  2. Mantenimiento del sillón 2 (día +3, 8–13).
  3. Capacitación de Ruiz (día −5).
  4. Trámite de Fernández (día +1, 14–16).
  5. Congreso de Sosa, anulado: no bloquea.
  6. Sillón 4 fuera de servicio.
- **Sobreturnos** (`es_sobreturno = true`, con motivo): Giménez el día +2 a las 18:00 y el día +5 a las 13:00 (entre turnos), y Fernández el día −3 a las 20:00.
- Vistas `/agenda/dia` y `/agenda/semana`: cualquier día hábil dentro del rango tiene datos.

### Reserva pública, cancelación y reprogramación (C-06, C-09)
- **Paciente 1 (Sofía Martínez)**:
  - Turno **reservado mañana a las 11:00** con Giménez. Está **dentro** de las 48 hs hábiles, así que cancelarlo o reprogramarlo tiene que rechazarse.
  - Turno **reservado a 6 días hábiles a las 09:00**. Está **fuera** de la ventana, así que se puede cancelar o reprogramar.
- **Paciente 42 (Valentín Navarro)**: no tiene turnos ni ficha. Sirve para la primera reserva por link.
- **Paciente 39 (Hernán Villalba)**: no tiene consentimiento de datos registrado.
- **Pacientes 40 y 41**: mismo DNI `90000040`. El 40 está dado de baja y el 41 activo, lo que prueba el índice único parcial.
- **Disponibilidad**: `GET /publico/v1/turnos/disponibilidad` para el profesional 1 y el sillón 1 en cualquier día hábil futuro, salvo +8 y +9 (vacaciones).
- Los tokens públicos **no se siembran**: los firma la aplicación con el secreto del entorno.

### WhatsApp: confirmación y recordatorio (C-10)
- Plantillas:
  - 2 `turno_confirmacion_v1` y 3 `turno_recordatorio_v1`, ambas APROBADAS.
  - 1 `turno_recordatorio_v0`, RECHAZADA.
  - 4 `turno_confirmacion_v2`, PENDIENTE.
  - 5, en inglés e inactiva.
- Para probar el bloqueo por plantilla no aprobada: desactivar la plantilla 3 y programar un recordatorio.
- Notificaciones en los 7 estados (`PENDIENTE`, `ENVIADA`, `ENTREGADA`, `LEIDA`, `CONFIRMADA`, `FALLIDA` con 3 intentos fallidos y `BLOQUEADA`).
- Outbox: confirmaciones pendientes y recordatorios a 24 h, ya procesados en el pasado y pendientes a futuro. Intentos y eventos de webhook (`delivered`, `read` y `button_reply`), con `evento_id` único para probar la idempotencia.

### Historia clínica (C-11, C-12, C-13)
- **24 fichas** (pacientes 1–15, 17–19, 22–24, 30, 31 y 33).
- **Anamnesis**:
  - Versión 1 en todas las fichas.
  - Versión 2 en los pacientes 2, 8 y 15 (nuevo diagnóstico de diabetes).
  - El paciente 18 está anticoagulado.
- **Odontograma FDI**:
  - Adultos con piezas permanentes (caries, obturado, endodoncia, corona, ausente, extracción indicada).
  - **Menores** (4, 5, 9, 13, 19 y 23) con **piezas temporales** (51, 54, 55, 64, 74, 75 y 84) y selladores en el 16.
  - Versiones 2 sobre la misma pieza y cara (16 oclusal y 54 oclusal pasan de caries a obturado).
- **Evoluciones**:
  - Una por cada turno confirmado y pasado de un paciente con ficha.
  - 3 tienen **versión 2 que reemplaza** a la 1.
  - Las 5 más recientes quedan **sin firmar**, para probar la firma con matrícula. El resto tiene firma con un hash que coincide con `FirmaClinicaService`.
- **Consentimientos**:
  - `tratamiento_general` en todas las fichas, firmado con el proveedor simulado. Los pacientes 1 y 3 tienen versión 2.
  - `endodoncia` (pacientes 2, 6, 8, 12 y 18) **sin firmar**.
- **Copias de historia** entregadas a los pacientes 2 y 8.

### Presupuesto y señas (C-14, C-15)
- 12 presupuestos con 22 ítems a precio snapshot. Los totales se calculan a partir de los ítems. El **5 está anulado**. Los presupuestos 1, 2, 4, 6, 8 y 10 están asociados al próximo turno del paciente.
- Señas:

| id | Estado | Detalle |
|---|---|---|
| 1, 2, 5, 8, 10 | aprobado | Conciliadas |
| 3 | rechazado | |
| 4, 9 | pendiente | Sirven para simular el webhook de Mercado Pago |
| 6 | cancelado | |
| 7 | reembolsado | |
| 11 | aprobado | **Sin conciliar**: referencia el turno inexistente 999999 |

### Facturación ARCA (C-16)

| Seña | Comprobantes |
|---|---|
| 1 y 2 | Factura emitida con CAE |
| 5 | Factura **rechazada** con error, para probar el reintento manual |
| 7 | Factura emitida y **nota de crédito** por el reembolso |
| 8 | Factura **pendiente** |
| 10 | **Sin comprobante**, para emitir uno |

### Lista de espera y métricas (C-19, C-22)
- Entradas de lista de espera:
  - 5 **activas**, con y sin depósito requerido, para distintos profesionales y días.
  - 1 **ofrecida** (paciente 27, con la seña pendiente 4).
  - 1 **adjudicada** (paciente 29, con turno `origen = waitlist` y la seña 8).
  - 1 **expirada**.
- Para probar el relleno: cancelar un turno activo dentro del rango de una entrada activa.
- Métricas de ocupación y ausentismo de los últimos 3 meses cerrados, para los sillones 1–3, con la metodología completa. La métrica del mes en curso se calcula a partir de turnos, bloqueos, horarios y feriados, que cubren el período.

### Cumplimiento (C-17, C-18)
- 1 **exportación por paciente SOLICITADA** (paciente 15), con un plazo que vence en unas 46 h.
- 1 **respaldo FALLIDO**, para probar la alerta de fallo.
- 3 **subprocesadores sin cláusulas AAIP 198/2023**: el ítem `CLAUSULAS_SUBPROCESADORES` del dossier tiene que quedar bloqueado.
- **No se inventa evidencia**:
  - No hay exportaciones, respaldos ni restores COMPLETADOS: se generan corriendo las operaciones reales.
  - Los ítems manuales del dossier siguen PENDIENTES.
  - `auditoria_log` arranca vacío y lo llena la aplicación con su cadena de hash (con datos sembrados a mano, la verificación de integridad fallaría).
