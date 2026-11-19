# Decisiones y Supuestos

## Decisiones documentadas

### DD-01 — No imponer stack tecnológico
**Decisión**: no elegir lenguaje, framework ni base de datos en la KB; marcar `(TBD)`.  
**Contexto**: la entrevista declara «sin stack tecnológico impuesto» y presupuesto acotado (`discovery` §9).  
**Alternativas consideradas**: (a) elegir un stack popular para acelerar; (b) dejar la decisión al equipo; (c) diferir hasta validar presupuesto y destino de despliegue.  
**Justificación**: (b)+(c) respeta la restricción y evita deuda especulativa.  
**Trade-offs aceptados**: la arquitectura queda lógica y los diseños físicos se postergan.

### DD-02 — WhatsApp Business API obligatoria en MVP
**Decisión**: incluir WhatsApp Business API con plantillas para confirmación y recordatorio.  
**Contexto**: discovery §5 y §8 la declaran obligatoria; es estándar de mercado con API oficial en varios competidores.  
**Alternativas consideradas**: SMS/email; enlace manual de WhatsApp (patron observado en Dentidesk y BlueDental).  
**Justificación**: es el canal esperado por pacientes y no depende de un servicio nuevo no previsto.  
**Trade-offs aceptados**: costos por conversación y dependencia de Meta; modelo de precio `(TBD)`.

### DD-03 — Reserva por link/QR con write-back real
**Decisión**: la reserva pública escribe directamente en la agenda y libera el hueco al cancelar.  
**Contexto**: problema central del discovery §1 y recomendación §D.3 del informe.  
**Alternativas consideradas**: solicitudes con aprobación manual (patrón Weave); marketplace externo.  
**Justificación**: reduce fricción y evita estados divergentes.  
**Trade-offs aceptados**: mayor exigencia de transaccionalidad y seguridad en superficie pública.

### DD-04 — Agenda multiprofesional/multisillón con anti-solapamiento en datos
**Decisión**: duración variable, bloqueos, sobreturnos y prevención de solapamientos desde el MVP.  
**Contexto**: discovery §5, §7; es un requisito de entrada, no diferenciador.  
**Alternativas consideradas**: Control solo en la UI; sobreturnos sin restricción.  
**Justificación**: las pisadas entre profesionales y sillones son el problema operativo declarado.  
**Trade-offs aceptados**: la semántica exacta de sobreturno sigue `(TBD)` (RN-AG-03).

### DD-05 — Seña con Mercado Pago por paridad, no como innovación
**Decisión**: incluir señas con Mercado Pago en el MVP como paridad competitiva.  
**Contexto**: la afirmación «ningún sistema cobra seña con Mercado Pago» es **falsa**; DentalSoft, AgendaPro, FLAP, DentApp y Mi Agenda Profesional la documentan (`informe-discovery.md` «Nota preliminar»).  
**Alternativas consideradas**: presentarla como diferencial; diferirla.  
**Justificación**: es estándar observable y el propio discovery §8 la incluye por paridad.  
**Trade-offs aceptados**: no genera ventaja competitiva por sí sola; hay que competir en ejecución (conciliación, idempotencia, transparencia).

### DD-06 — Replanteo de la tesis de oportunidad
**Decisión**: los vacíos reales son precio predecible/todo incluido, métricas verificables de ocupación y ausentismo, relleno automático de waitlist con depósito y cumplimiento **demostrable**.  
**Contexto**: correcciones del informe: también es falso que ningún proveedor declare Ley 25.326 (Bilog, Odonthia, AgendaPro, DentalCore y otros lo hacen).  
**Alternativas consideradas**: tesis «nadie lo hace».  
**Justificación**: esa tesis quedó refutada por fuentes primarias.  
**Trade-offs aceptados**: el mensaje de producto debe hablar de evidencia, no de novedad.

### DD-07 — Ventana de cancelación de 48 hs hábiles
**Decisión**: cancelación/reprogramación configurable con ventana de 48 hs hábiles.  
**Contexto**: discovery §7 y §5; informe §D.3.  
**Alternativas consideradas**: 24 h, 48 h corridas, sin ventana.  
**Justificación**: es el valor acordado en la entrevista.  
**Trade-offs aceptados**: hay que definir feriados y cómputo `(TBD)`.

### DD-08 — Separación MVP / diferenciadores / etapas posteriores
**Decisión**: fuera del MVP quedan obras sociales, periodontograma avanzado, radiología, API pública, Google Calendar e IA amplia; waitlist con depósito y métricas son diferenciadores.  
**Contexto**: discovery §6, informe §D.3.  
**Alternativas consideradas**: incluir todo desde el inicio.  
**Justificación**: presupuesto acotado y riesgo de scope creep.  
**Trade-offs aceptados**: se resigna el foso local de obras sociales en la primera versión.

### DD-09 — ARCA dentro del MVP: `(TBD)`
**Decisión**: `(TBD)` — no se decide.  
**Contexto**: discovery §9 lo lista como restricción, §8 lo ubica en etapa posterior, e `informe-discovery.md` §D.3 lo incluye entre los imprescindibles. Ver IN-01.  
**Alternativas consideradas**: (a) solo presupuesto y señas en MVP; (b) facturación ARCA y conciliación completa desde MVP.  
**Justificación de la indecisión**: contradicción no resuelta en las fuentes; no corresponde al agente inventarla.  
**Trade-offs aceptados**: bloquea el cierre del alcance financiero.

### DD-10 — Multisede desde el día 1: `(TBD)`
**Decisión**: `(TBD)` — discovery §11 lo pregunta; el informe habla de multisede avanzada como etapa posterior.  
**Alternativas consideradas**: (a) consultorio único; (b) multisede con aislamiento por sede.  
**Trade-offs aceptados**: afecta el modelo de datos y RBAC.

### DD-11 — IA conversacional en MVP: `(TBD)`
**Decisión**: `(TBD)` — discovery §11 lo pregunta. Si se incluye, la derivación humana ante urgencias es obligatoria y auditada (RN-CL-06).  
**Alternativas consideradas**: (a) solo plantillas; (b) asistente con handoff registrado.  
**Trade-offs aceptados**: define si se necesita evidencia de precisión y métricas de handoff.

## Supuestos inferidos

### SU-01 — Consultorio único como punto de partida
**Suposición**: la primera versión atiende un solo consultorio.  
**Origen**: discovery §11 plantea la multisede como pregunta abierta; informe §D.3 la difiere como «multisede avanzada».  
**Riesgo si es falso**: hay que rehacer modelo de datos, RBAC y reportes por sede.  
**Cómo validar**: confirmar DD-10 con el Product Owner antes de diseñar la base de datos.

### SU-02 — Escala pequeña/mediana
**Suposición**: equipo y consultorio pequeños, sin multi-tenancy (escala y `system_type` `(TBD)`).  
**Origen**: presupuesto acotado y actores enumerados (`discovery` §2, §9).  
**Riesgo si es falso**: cambian-auth, aislamiento y decisiones de infraestructura.  
**Cómo validar**: responder P0-sys y P0-scale en [10_preguntas_abiertas.md](10_preguntas_abiertas.md).

### SU-03 — Adopción incierta de la reserva online
**Suposición**: el riesgo principal es que los pacientes sigan llamando por costumbre (`discovery` §10).  
**Origen**: discurso de riesgo del discovery; sin medición previa.  
**Riesgo si es falso**: si la adopción es alta, hay que dimensionar antes; si es baja, el MVP no resuelve el problema.  
**Cómo validar**: definir métrica de adopción y objetivo `(TBD)` antes del lanzamiento.

### SU-04 — Evidencia de competidores no verificable de forma independiente
**Suposición**: las cifras de adopción y reducciones de ausencias de la competencia son afirmaciones comerciales, no datos auditados.  
**Origen**: convenciones `[A]` del informe y «Nota preliminar».  
**Riesgo si es falso**: se podría subestimar a un competidor.  
**Cómo validar**: demos prioritarias listadas en informe §D.1.

### SU-05 — Zona horaria y calendario local
**Suposición**: opera en hora de Argentina con feriados nacionales; zona exacta y feriados provinciales `(TBD)`.  
**Origen**: alcance argentino del informe; regla de 48 **hs hábiles** en discovery §7.  
**Riesgo si es falso**: la ventana de cancelación y los recordatorios se calculan mal.  
**Cómo validar**: confirmar jurisdicción y calendario con el consultorio.

### SU-06 — Sobreturno como excepción controlada
**Suposición**: un sobreturno se permite solo con un sillón/profesional libre o con override auditado.  
**Origen**: tensión entre discovery §5 (sobreturnos) y §7 (anti-solapamiento).  
**Riesgo si es falso**: se rompe la garantía central de RN-AG-01.  
**Cómo validar**: definir RN-AG-03 con el Product Owner y el odontólogo.

### SU-07 — Coste de infraestructura acotado
**Suposición**: se prefiere una solución local/simple antes que un despliegue administrado costoso (`needs_infra` `(TBD)`).  
**Origen**: restricción presupuestaria (`discovery` §9).  
**Riesgo si es falso**: elección de stack y cumplimiento (backups, cifrado) incompatible.  
**Cómo validar**: responder P4 sobre cloud/on-prem y responsable de datos.

## Fuentes

- `docs/discovery/discovery.md` §§1, 2, 5–11.
- `docs/discovery/informe-discovery.md` «Nota preliminar», §§B, C, D.
