# C-01 — Decisiones habilitantes

- **Change**: `C-01 decisiones-habilitantes`
- **Governance**: `CRITICO`
- **Alcance**: solo análisis (sin código, sin instalaciones, sin servicios externos nuevos, sin cambios bajo `openspec/`)
- **Estado**: aprobado por el PO
- **Fecha**: 2026-11-20
- **Fuentes consultadas**: `knowledge-base/05_reglas_de_negocio.md`, `knowledge-base/09_decisiones_y_supuestos.md`, `knowledge-base/10_preguntas_abiertas.md`, `knowledge-base/01_vision_y_objetivos.md`, `knowledge-base/02_descripcion_general.md`, `knowledge-base/08_arquitectura_propuesta.md`, `CHANGES.md`, `docs/discovery/`. Las decisiones A/B/C se recuperaron de Engram (observación 13, `topic_key=opsx/c-01/decisions`) y se transcriben sin re-decidirlas.

> La aprobación humana explícita del PO cubre las 18 secciones siguientes. Los ítems marcados `(TBD)` no se resuelven por convención (RN-GL-03).

## Decisiones

### A1 — ARCA completo en MVP
- Decisión: opción b — facturación ARCA y conciliación completas en el MVP; `C-16` se implementa.
- Decisor: Product Owner
- Fecha: 2026-11-20
- Alternativas descartadas: (a) solo presupuesto y señas en MVP — descartada porque el informe (`§D.3`) la considera imprescindible y la decisión cierra IN-01/DD-09 en favor de esa fuente.
- Changes impactados: `C-14`, `C-15`, `C-16`, `C-21`
- Ítems aún (TBD): ninguno en esta decisión; los detalles de integración se definen en `C-16`.
- Aprobación humana explícita: sí — PO, 2026-11-20

### A2 — Consultorio único
- Decisión: opción a — consultorio único (DD-10, SU-01).
- Decisor: Product Owner
- Fecha: 2026-11-20
- Alternativas descartadas: (b) multisede con aislamiento por sede — descartada por su impacto en modelo de datos, RBAC y reportes, sin necesidad declarada en el MVP.
- Changes impactados: `C-03`, `C-04`, `C-07`
- Ítems aún (TBD): ninguno
- Aprobación humana explícita: sí — PO, 2026-11-20

### A3 — Solo plantillas de WhatsApp, sin IA
- Decisión: opción a — solo plantillas WhatsApp aprobadas, sin asistente conversacional; `IA_HANDOFF_ENABLED=false`, `C-20` diferido.
- Decisor: Product Owner
- Fecha: 2026-11-20
- Alternativas descartadas: (b) asistente con handoff clínico registrado — descartada por la evidencia de precisión y las obligaciones de RN-CL-06 que implicaría en el MVP.
- Changes impactados: `C-10`, `C-20`
- Ítems aún (TBD): ninguno
- Aprobación humana explícita: sí — PO, 2026-11-20

### A4 — Reembolso total y liberación inmediata
- Decisión: opción a — reembolso total y liberación inmediata del hueco; riesgo de ausentismo aceptado (IN-02, RN-FI-03).
- Decisor: Product Owner
- Fecha: 2026-11-20
- Alternativas descartadas: retención parcial de la seña y/u ocultamiento del hueco — descartadas por fricción con el paciente e incompatibilidad con el *write-back* real (DD-03).
- Changes impactados: `C-09`, `C-15`, `C-22`
- Ítems aún (TBD): ninguno
- Aprobación humana explícita: sí — PO, 2026-11-20

### A5 — Precio plano todo incluido, sin desglose de API
- Decisión: opción 2 — precio plano todo incluido, sin desglose de costos de API para el consultorio; enmienda aprobada a RN-SE-06 y a `C-21`.
- Decisor: Product Owner
- Fecha: 2026-11-20
- Alternativas descartadas: desglosar los costos de API/Meta en la factura del consultorio, y un modelo de precio por conversación — descartadas por romper la previsibilidad del precio único (RN-NT-04, US-016).
- Changes impactados: `C-15`, `C-16`, `C-21`
- Ítems aún (TBD): ninguno
- Aprobación humana explícita: sí — PO, 2026-11-20

### B1 — Stack: Java + Spring Boot + JPA, API REST
- Decisión: Java + Spring Boot + JPA (Lombok/DTO), API REST (resuelve DD-01 para este proyecto).
- Decisor: equipo técnico
- Fecha: 2026-11-20
- Alternativas descartadas: mantener el stack como `(TBD)` y elegir otra combinación popular — descartadas porque el PO habilitó fijar el stack en C-01 y la combinación elegida cubre la API REST del núcleo.
- Changes impactados: `C-02`, `C-03` y sucesivos
- Ítems aún (TBD): versión de Java y Maven vs Gradle — bloquea `C-02`/`C-03`.
- Aprobación humana explícita: sí — PO, 2026-11-20

### B2 — Base de datos relacional con restricción transaccional
- Decisión: opción a — BD relacional con restricción/serialización transaccional para anti-solapamiento (RN-AG-01: profesional **y** sillón; dos escrituras concurrentes se verifican en `C-08`).
- Decisor: equipo técnico
- Fecha: 2026-11-20
- Alternativas descartadas: control solo en la interfaz (DD-04) — descartado porque no sobrevive a escrituras concurrentes; persistencia sin restricción de integridad — descartada por el mismo motivo.
- Changes impactados: `C-03`, `C-08`, `C-09`
- Ítems aún (TBD): motor de BD concreto — bloquea `C-02`/`C-03`.
- Aprobación humana explícita: sí — PO, 2026-11-20

### B3 — Web app de consultorio único
- Decisión: opción a — web app de consultorio único (P0-sys/P0-scale).
- Decisor: Product Owner
- Fecha: 2026-11-20
- Alternativas descartadas: SaaS multi-tenant y aplicación nativa — descartadas por A2 y por la escala prevista (B4).
- Changes impactados: `C-02`, `C-04`, `C-06`, `C-09`
- Ítems aún (TBD): ninguno
- Aprobación humana explícita: sí — PO, 2026-11-20

### B4 — Escala pequeña con techo explícito
- Decisión: opción a — escala pequeña con techo explícito (SU-02).
- Decisor: Product Owner
- Fecha: 2026-11-20
- Alternativas descartadas: diseño multi-tenant de gran escala — descartado por presupuesto acotado y ausencia de necesidad declarada.
- Changes impactados: `C-03`, `C-04`, `C-22`
- Ítems aún (TBD): ninguno
- Aprobación humana explícita: sí — PO, 2026-11-20

### B5 — Infraestructura mínima
- Decisión: opción a — infraestructura mínima/local en huella operativa; se despliega en la nube según B6.
- Decisor: equipo técnico
- Fecha: 2026-11-20
- Alternativas descartadas: despliegue administrado costoso o plataforma con servicios administrados adicionales — descartados por la restricción presupuestaria (SU-07).
- Changes impactados: `C-02`, `C-17`, `C-18`
- Ítems aún (TBD): ninguno
- Aprobación humana explícita: sí — PO, 2026-11-20

### B6 — Cloud en región AR con DPA y cláusulas AAIP 198/2023
- Decisión: opción b — cloud en región AR, con DPA y cláusulas AAIP 198/2023 para subprocessores (RN-SE-04, RN-SE-05).
- Decisor: equipo técnico
- Fecha: 2026-11-20
- Alternativas descartadas: on-prem o local sin DPA ni cláusulas de subprocessores — descartadas porque no satisfacen la residencia de datos ni el cumplimiento demostrable requerido.
- Changes impactados: `C-02`, `C-17`, `C-18`
- Ítems aún (TBD): ninguno en esta decisión; el owner legal de la documentación queda registrado en D.
- Aprobación humana explícita: sí — PO, 2026-11-20

### C1 — Sobreturno como excepción auditada
- Decisión: opción b — sobreturno como excepción auditada con override, motivo y actor registrados; RN-GL-01 prevalece salvo override explícito (IN-04, SU-06, RN-AG-03).
- Decisor: Product Owner y odontólogo
- Fecha: 2026-11-20
- Alternativas descartadas: sobreturnos sin restricción (rompe RN-AG-01), y tratarlos siempre como turno en otro recurso (no cubre la práctica declarada).
- Changes impactados: `C-05`, `C-07`, `C-08`
- Ítems aún (TBD): ninguno
- Aprobación humana explícita: sí — PO, 2026-11-20

### C2 — Zona horaria, feriados y cómputo de 48 hs hábiles
- Decisión: opción a — `America/Argentina/Buenos_Aires` + feriados nacionales, días hábiles completos y calendario configurable (RN-AG-05, DD-07, SU-05).
- Decisor: odontólogo (consultorio)
- Fecha: 2026-11-20
- Alternativas descartadas: 48 hs corridas, 24 h, sin ventana y feriados provinciales — descartadas por no respetar la ventana acordada ni el alcance nacional confirmado.
- Changes impactados: `C-07`, `C-08`, `C-09`, `C-10`
- Ítems aún (TBD): ninguno
- Aprobación humana explícita: sí — PO, 2026-11-20

### C3 — Catálogo amplio con duración configurable
- Decisión: opción b — catálogo amplio con duración configurable por profesional (RN-AG-03, DD-04, US-002/US-008).
- Decisor: odontólogo
- Fecha: 2026-11-20
- Alternativas descartadas: duración única por turno y catálogo mínimo — descartadas porque la agenda requiere duración variable por tratamiento y profesional.
- Changes impactados: `C-07`, `C-08`
- Ítems aún (TBD): ninguno
- Aprobación humana explícita: sí — PO, 2026-11-20

### C4 — Anamnesis mínima versionada
- Decisión: opción a — anamnesis de campos mínimos + texto libre, versionada sin sobrescritura (RN-CL-01/02/03).
- Decisor: odontólogo
- Fecha: 2026-11-20
- Alternativas descartadas: formulario clínico rígido extenso y texto libre sin versionar — descartados por carga operativa y por no conservar evoluciones firmadas.
- Changes impactados: `C-11`, `C-12`, `C-13`
- Ítems aún (TBD): ninguno
- Aprobación humana explícita: sí — PO, 2026-11-20

### D — Marco normativo ampliado con dossier mínimo y pendientes explícitos
- Decisión: opción b — marco ampliado con dossier mínimo y listado explícito de pendientes (cumplimiento progresivo); riesgo de dossier incompleto admitido. Aprobado por el PO; owner legal a designar (requerido por `C-18`).
- Decisor: responsable legal (owner legal a designar; pendiente registrado más abajo)
- Fecha: 2026-11-20
- Alternativas descartadas: dossier completo desde el inicio (IN-05/IN-06) — descartado por presupuesto acotado; declaración de Ley 25.326 sin evidencia — descartada por no ser cumplimiento demostrable (RN-SE-04, DD-06).
- Changes impactados: `C-05`, `C-12`, `C-17`, `C-18`
- Ítems aún (TBD): owner legal que firma el dossier, aprobado por el PO pero no identificado — requerido por `C-18`.
- Aprobación humana explícita: sí — PO, 2026-11-20

### E — Umbrales explícitos con metodología y período
- Decisión: opción a — umbrales explícitos con metodología y período para las cinco métricas. Requisito literal del PO: «Cada métrica (adopción online, ocupación por sillón, ausentismo, cumplimiento de ventana y dossier) debe tener definición, fórmula, fuente del dato y período. Los números son metas iniciales a ajustar tras el piloto; si no hay base para un valor, dejalo (TBD) con la forma de obtenerlo.»
- Decisor: Product Owner
- Fecha: 2026-11-20
- Alternativas descartadas: publicar cifras sin metodología ni período (SU-04) — descartado por no auditables; copiar umbrales de la competencia — descartado por no verificables de forma independiente (DD-06).
- Changes impactados: `C-09`, `C-18`, `C-19`, `C-22`
- Ítems aún (TBD): período y meta inicial de las cinco métricas, y la ambigüedad metodológica de ocupación (¿los bloqueos cuentan como agenda ocupada?). No se inventan cifras: ver tabla de Métricas.
- Aprobación humana explícita: sí — PO, 2026-11-20

### Gap C-19 — Waitlist con relleno automático y depósito
- Decisión: opción b — waitlist con relleno automático y depósito **diferida a v0.2, no descartada**. `C-19` se cierra sin código en v0.1 y se mantiene documentada la dependencia con `C-09`/`C-10`/`C-15` para cuando se retome. Coherente con `knowledge-base/01_vision_y_objetivos.md` línea 39 («diferenciador posterior al MVP»).
- Decisor: Product Owner
- Fecha: 2026-11-20
- Alternativas descartadas: (a) incluirla en v0.1 — descartada por presupuesto y por ser un diferenciador posterior; eliminarla del roadmap — descartada porque el PO la mantiene como objetivo de v0.2.
- Changes impactados: `C-09`, `C-10`, `C-15`, `C-19`, `C-22`
- Ítems aún (TBD): ninguno para v0.1; la planificación de v0.2 queda fuera de este cierre.
- Aprobación humana explícita: sí — PO, 2026-11-20

## Conflictos de fuentes resueltos

| Conflicto | Resolución aprobada |
|---|---|
| ARCA (IN-01 / DD-09): `discovery.md` §§6/8 la ubica después del MVP, §9 la lista como restricción, informe §D.3 la incluye como imprescindible | A1=b: facturación y conciliación ARCA completas en el MVP; `C-16` se implementa. |
| IN-02 (Mercado Pago): §11 la pregunta vs. §8 y el informe la dan por incluida | Incluida por paridad competitiva (DD-05); A4 fija además la política de reembolso total y liberación inmediata. |
| Sobreturno (IN-04 / SU-06 / RN-AG-03): §5 lo permite vs. §7 prohíbe solapamiento | C1=b: excepción auditada con override, motivo y actor; RN-GL-01 prevalece salvo override. |
| Dossier (IN-05): §5 pide roles/auditoría/exportación/respaldo vs. informe §C.4 exige DPA, AAIP 198/2023, RNBD, 48 h y 10 años | D=b: dossier mínimo + listado explícito de pendientes, cumplimiento progresivo. |
| Marco normativo (IN-06): §9 solo Ley 25.326 vs. informe suma Ley 26.529, Decreto 1089/2012 y Ley 25.506 | D=b: marco ampliado como requerimiento; owner legal a designar para `C-18`. |
| IA (DD-11): plantillas vs. asistente con handoff registrado | A3=a: solo plantillas; `IA_HANDOFF_ENABLED=false`; `C-20` diferido. |
| Precio con IA (RN-NT-04 / US-016): costos de API detallados por separado vs. precio único | A5=2: suscripción plana todo incluido sin desglose de API; RN-SE-06 enmendada y `C-21` ajustado. |
| IN-03 (17 vs. 22 sistemas comparados) | Solo trazabilidad documental: 22 sistemas como cifra vigente, PDF original de 17 conservado como fuente; sin impacto en el producto. |

## `(TBD)` de implementación diferidos

No se inventan valores; se resuelven en los changes indicados (governance BAJO):

- Motor de base de datos concreto → `C-02` / `C-03`.
- Versión de Java → `C-02` / `C-03`.
- Maven vs Gradle → `C-02` / `C-03`.
- Owner legal que firma el dossier (`aprobado por el PO; owner legal a designar, requerido por C-18`) → `C-18`.

## Métricas

Definiciones tomadas de `knowledge-base/01_vision_y_objetivos.md` §«Métricas de éxito» (líneas 51-61). Las fórmulas y fuentes son la traducción formal de esas definiciones a entidades del sistema; no constituyen decisiones nuevas. Cualquier cambio de metodología requiere aprobación del PO.

| Métrica | Definición | Fórmula | Fuente del dato | Período | Meta inicial |
|---|---|---|---|---|---|
| Adopción de reserva online | % de turnos creados por el paciente vía link/QR sobre el total | `100 × turnos con canal=link/QR / total de turnos creados` | Entidad `Turno` y su canal de creación (token público link/QR) | `(TBD)` — medir en piloto y fijar rango | `(TBD)` — baseline del piloto y umbral revisable |
| Ocupación verificable | % de agenda ocupada por sillón/profesional, con metodología publicable | `100 × minutos ocupados / minutos disponibles`, por sillón y profesional; `(TBD)` si los bloqueos cuentan como ocupación (resolver: definir con el consultorio y documentar la metodología antes de publicar) | Agenda, turnos y bloqueos por sillón/profesional | `(TBD)` — medir en piloto y fijar rango | `(TBD)` — baseline del piloto y umbral revisable |
| Ausentismo verificable | % de no-shows con definición y período explícitos | `100 × turnos con evento de inasistencia / total de turnos confirmados`; `(TBD)` si los cancelados fuera de ventana entran en el numerador (resolver: fijar definición auditada con el consultorio) | Eventos de asistencia del turno y auditoría append-only | `(TBD)` — medir en piloto y fijar rango | `(TBD)` — baseline del piloto y umbral revisable |
| Cumplimiento de ventana | % de cancelaciones/reprogramaciones dentro de las 48 hs hábiles | `100 × cancelaciones/reprogramaciones dentro de 48 hs hábiles / total de cancelaciones/reprogramaciones` | Turnos y sus eventos de cancelación/reprogramación con sello de tiempo; calendario configurable de feriados | `(TBD)` — medir en piloto y fijar rango | `(TBD)` — baseline del piloto y umbral revisable |
| Evidencia de cumplimiento | Dossier descargable completo (DPA, AAIP, RNBD, logs, exportación) | `100 × ítems del dossier presentes y verificables / ítems exigidos`; `(TBD)` la lista cerrada de ítems exigidos (resolver: completar el listado de pendientes en `C-18`) | Dossier de cumplimiento, logs append-only, exportación y respaldo | `(TBD)` — medir en piloto y fijar rango | `(TBD)` — baseline del piloto y umbral revisable |

> **Governance**: las metas son iniciales y se ajustan tras el piloto. Donde no hay base para un valor se mantiene `(TBD)` con su forma de obtención; no se publican cifras sin metodología y período explícitos (DD-06, SU-04).

## Aclaraciones finales

- A5=2 modifica RN-SE-06; la frase «precio predecible y todo incluido» de `AGENTS.md` **no cambia**.
- «API REST en el núcleo; UI de reserva pública en changes posteriores». La limitación «sin interfaz gráfica» aplica solo a `C-02`; el link/QR de reserva pública **permanece en el MVP** según discovery/KB.
