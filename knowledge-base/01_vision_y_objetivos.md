# Visión y Objetivos

> Generado con `kb-creator` (variante `interview`, 2026-11-19). El registro de entrevista son los outputs de discovery ya corregidos; lo que el registro no responde se marca `(TBD)` y se consolida en [10_preguntas_abiertas.md](10_preguntas_abiertas.md).

## Propósito del sistema

Permitir que un consultorio odontológico gestione sus turnos en un solo lugar y que el paciente pueda reservar, confirmar, cancelar y reprogramar sin llamar por teléfono, con trazabilidad clínica y cumplimiento verificable de la normativa argentina de datos personales e historia clínica.

**Contexto**: el problema es dual (`docs/discovery/discovery.md` §1). Los pacientes dependen de llamadas o mensajes para gestionar su turno, y los consultorios sufren reservas dobles o pisadas entre profesionales y sillones. La oportunidad **no** es «nadie lo hace»: la seña con Mercado Pago y la declaración de Ley 25.326 ya están documentadas en la competencia. El diferencial está en previsible, verificable y demostrable (`docs/discovery/informe-discovery.md` «Nota preliminar» y §C.3).

## Objetivos por actor

| Actor | Objetivo principal | Objetivos secundarios | Fuente |
|---|---|---|---|
| Paciente | Gestionar su turno sin llamar, desde un link o QR | Ver disponibilidad, recibir recordatorio por WhatsApp, conocer y ejercer sus derechos sobre sus datos | discovery §2, §3, §5 |
| Recepcionista | Centralizar y defender la agenda del consultorio | Ver agenda por sillón, detectar solapamientos, administrar bloqueos y sobreturnos, reubicar cancelaciones | discovery §2, §4, §5 |
| Odontólogo | Ver su agenda y la ficha clínica del paciente | Registrar anamnesis, odontograma FDI (incluidos dientes temporales) y evolución firmada con matrícula | discovery §2, §5 |
| Administrador / responsable de datos | Cumplir y poder demostrar el cumplimiento | Roles, auditoría, exportación completa, respaldo, copia de historia, dossier de evidencia | discovery §5, §9; informe §D.3 |
| Dirección del consultorio `(TBD)` | Medir la operación | Ocupación por sillón y métricas verificables de ausentismo; precio predecible | informe §C.3, §C.4, §D.3 — rol no confirmado en la entrevista |

## Alcance v0.1 (MVP acordado)

**SÍ incluye**, según el registro de entrevista (`docs/discovery/discovery.md` §5, §8; `docs/discovery/informe-discovery.md` §D.3):

- Agenda multiprofesional y multisillón, con duración variable por tipo de tratamiento, bloqueos, sobreturnos y prevención de solapamientos con bloqueo en base de datos.
- Reserva online por enlace y QR con confirmación, cancelación y reprogramación, con *write-back* real a la agenda.
- WhatsApp Business API obligatoria: plantillas aprobadas para confirmación y recordatorio.
- Ficha clínica, anamnesis y odontograma FDI con dientes temporales, presupuestos y evolución firmada con matrícula.
- Caja, presupuesto y señas; Mercado Pago para señas **por paridad competitiva, no como diferenciador**.
- Roles y permisos, auditoría, exportación completa de datos y respaldo.
- Ventana de cancelación/reprogramación configurable de **48 hs hábiles**.
- Cumplimiento de Ley 25.326 / Ley 26.529 / Decreto 1089/2012 con evidencia (DPA, registro AAIP del responsable, cláusulas AAIP 198/2023 para subprocessors, retención y copia de historia). Parámetros operativos exactos `(TBD)`.

**Condicionado a decisión pendiente** (no se inventa un valor):

- Facturación ARCA y conciliación dentro del MVP: `(TBD)` — el registro es contradictorio (ver IN-01).
- IA conversacional con derivación humana obligatoria ante urgencias: `(TBD)` — si se incluye, el handoff debe quedar registrado en la historia clínica.
- Soporte multisede desde el día 1: `(TBD)` — el registro sugiere empezar con consultorio único y diferir multisede avanzada.
- Relleno automático de lista de espera con depósito y métricas de ocupación/ausentismo: se trata como **diferenciador** posterior al MVP; su inclusión en v0.1 es `(TBD)`.

## Fuera de alcance

- Validación y liquidación de obras sociales y prepagas (foso de los incumbentes; etapa posterior).
- Facturación avanzada, receta electrónica con validez legal (ReNaPDiS) y, además, la facturación ARCA no confirmada para el MVP.
- Periodontograma con curvas, radiología/PACS e imágenes 3D.
- API pública, interoperabilidad FHIR completa y Google Calendar bidireccional (etapa posterior).
- IA conversacional amplia sin reglas clínicas auditables.
- Sustituir el criterio clínico del odontólogo ni emitir diagnósticos automatizados.
- Migración de datos desde sistemas de terceros `(TBD)` — no mencionada en la entrevista.

## Métricas de éxito

| Métrica | Definición propuesta | Valor objetivo |
|---|---|---|
| Adopción de reserva online | % de turnos creados por el paciente vía link/QR sobre el total | `(TBD)` — es el riesgo principal del proyecto |
| Ocupación verificable | % de agenda ocupada por sillón/profesional, con metodología publicable | `(TBD)` |
| Ausentismo verificable | % de no-shows con definición y período explícitos | `(TBD)` |
| Cumplimiento de ventana | % de cancelaciones/reprogramaciones dentro de las 48 hs hábiles | `(TBD)` |
| Evidencia de cumplimiento | Dossier descargable completo (DPA, AAIP, RNBD, logs, exportación) | `(TBD)` |

> **Suposición**: el informe advierte que los competidores publican reducciones de ausencias sin metodología; por eso las métricas de este sistema deben ser auditables, no solo decorativas. Confirmar con el Product Owner qué umbral considera éxito.

## Fuentes

- `docs/discovery/discovery.md` §§1–11.
- `docs/discovery/informe-discovery.md` «Nota preliminar: correcciones al informe previo», §§C.1–C.4, §D.3.
