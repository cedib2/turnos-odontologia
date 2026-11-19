# Actores y Roles

## Actores del sistema

| Actor | Descripción | Cómo interactúa | Fuente |
|---|---|---|---|
| Paciente | Titular de los datos, reserva y gestiona su turno | Link/QR, portal público y mensajes de WhatsApp | discovery §2, §3, §5 |
| Recepcionista | Gestiona la agenda del consultorio | Panel de agenda por sillón/profesional | discovery §2, §4 |
| Odontólogo | Profesional que atiende y registra la evolución | Agenda propia y ficha clínica | discovery §2, §5 |
| Administrador / responsable de datos | Configura roles, plantillas, exporta y respalda | Panel de administración | Inferido de discovery §5 y §9 — nombre del rol `(TBD)` |
| Auditor `(TBD)` | Consulta trazabilidad sin modificar datos | Log de auditoría de solo lectura | Inferido del requisito de auditoría — rol no confirmado |
| Servicios externos | WhatsApp Business API y Mercado Pago | Webhooks y llamadas API | discovery §8 |
| Dirección `(TBD)` | Consulta métricas de ocupación y ausentismo | Panel de reportes | informe §C.3 — rol no confirmado |

> Datos de salud: la ficha clínica y la evolución están alcanzadas por Ley 25.326 y Ley 26.529; el acceso del paciente y la copia en 48 h se tratan en RN-CL-04.

## RBAC — Matriz de permisos

Leyenda: **C** crear, **L** leer, **A** actualizar, **E** eliminar, **—** sin acceso. Ámbito: **propio**, **todos**, **config**. Los permisos efectivos y la herencia entre roles son `(TBD)`.

| Recurso | Paciente | Recepcionista | Odontólogo | Administrador `(TBD)` | Auditor `(TBD)` |
|---|---|---|---|---|---|
| Agenda (todos los profesionales/sillones) | L (solo huecos públicos) | C L A E | L (propia) | C L A E | L |
| Turno propio | C L A (dentro de ventana) | C L A E | L | C L A E | L |
| Bloqueos y sobreturnos | — | C L A E | L `(TBD)` | C L A E | L |
| Ficha clínica / anamnesis / odontograma | L (acceso conforme Ley 26.529 art. 14; alcance `(TBD)`) | L `(TBD)` | C L A | C L A E `(TBD)` | L |
| Evolución firmada y consentimientos | L `(TBD)` | L `(TBD)` | C L A | L A `(TBD)` | L |
| Presupuesto, caja y señas | L (propias) `(TBD)` | C L A | L `(TBD)` | C L A E | L |
| Plantillas y configuración de WhatsApp | — | — | — | C L A E | L |
| Horarios, feriados y duraciones | — | L | L `(TBD)` | C L A E | L |
| Lista de espera y métricas | — | C L A `(TBD)` | L `(TBD)` | C L A | L |
| Auditoría | — | — | — | L | L |
| Exportación completa y respaldos | — | — | — | C L | L |
| Dossier de cumplimiento / DPA / RNBD | L `(TBD)` | — | — | C L A | L |

**Reglas de acceso derivadas**: nadie puede reservar dos turnos solapados para el mismo profesional o sillón (RN-AG-01); el odontólogo «sin acceso a agenda ajena» es un patrón observado en la competencia pero **no** una decisión del proyecto `(TBD)`.

## Rutas públicas

Accesibles sin autenticación de staff, con token firmado/expirable `(TBD)`:

1. `link/QR` de reserva: consulta de disponibilidad y creación de turno.
2. Confirmación de asistencia desde enlace enviado por WhatsApp.
3. Cancelación y reprogramación dentro de la ventana de 48 hs hábiles.
4. Consulta/descarga de la propia historia clínica conforme a Ley 26.529 art. 14 — alcance y plazo operativo `(TBD)`.
5. Webhook de WhatsApp Business API y de Mercado Pago: no son «rutas de usuario», pero son endpoints públicos con verificación de firma `(TBD)`.

## Fuentes

- `docs/discovery/discovery.md` §§2–3, §5, §7, §9.
- `docs/discovery/informe-discovery.md` §A.2 (filas 2, 5, 7), §C.3, §D.3.
