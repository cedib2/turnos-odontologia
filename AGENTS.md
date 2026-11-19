<!-- active-stack:agent-instruction -->
# turnos-odontologia — Instrucciones para Agentes

> Este archivo (y su copia `CLAUDE.md`) es lo PRIMERO que todo agente lee al entrar al repo.
> Generado con la variante `generate` de `agent-instruction` a partir de `knowledge-base/`, `CHANGES.md` y `.atl/skill-registry.md`. No editar a mano sin resincronizar ambos archivos.

---

## Stack Tecnológico

El stack está deliberadamente **`(TBD)`** hasta `C-01`: no hay lenguaje, framework ni base de datos elegidos. La arquitectura de la KB es **lógica**, no una decisión de implementación.

| Capa | Tecnologías | Versión mínima | Estado |
|---|---|---|---|
| Cliente / reserva pública | `(TBD)` | `(TBD)` | La entrevista exige link/QR y autogestión, pero no define tecnología |
| Backend / API | `(TBD)` | `(TBD)` | Sin imposición; restricción presupuestaria |
| Persistencia | `(TBD)` | `(TBD)` | Debe soportar bloqueo transaccional anti-solapamiento |
| Autenticación de staff | `(TBD)` | `(TBD)` | RBAC obligatorio; esquema no definido |
| Colas / trabajos programados | `(TBD)` | `(TBD)` | Recordatorios, relleno de waitlist y respaldos |
| Infraestructura / despliegue | `(TBD)` | `(TBD)` | ¿Cloud, on-prem o local? `(TBD)` |
| Lenguaje/framework preferido del equipo | `(TBD)` | `(TBD)` | Pregunta P4 sin responder |

Integraciones confirmadas en el MVP: **WhatsApp Business API** (obligatoria) y **Mercado Pago** (señas, por paridad). ARCA, Google Calendar, obras sociales, FHIR/PACS e IA conversacional quedan condicionados o diferidos. Detalle completo: [knowledge-base/02_descripcion_general.md](knowledge-base/02_descripcion_general.md).

---

## Base de Conocimiento

La fuente de verdad del dominio vive en `knowledge-base/`. Leer el archivo relevante ANTES de implementar.

| Archivo | Cuándo leerlo |
|---|---|
| [README.md](knowledge-base/README.md) | Índice, resumen ejecutivo y orden de lectura |
| [01_vision_y_objetivos.md](knowledge-base/01_vision_y_objetivos.md) | Propósito, alcance del MVP, fuera de alcance y métricas |
| [02_descripcion_general.md](knowledge-base/02_descripcion_general.md) | Stack `(TBD)`, arquitectura lógica e integraciones |
| [03_actores_y_roles.md](knowledge-base/03_actores_y_roles.md) | Actores, matriz RBAC, rutas públicas y datos sensibles |
| [04_modelo_de_datos.md](knowledge-base/04_modelo_de_datos.md) | Dominios, entidades, constraints y seed |
| [05_reglas_de_negocio.md](knowledge-base/05_reglas_de_negocio.md) | Reglas codificadas `RN-XX-NN` y excepciones globales |
| [06_funcionalidades.md](knowledge-base/06_funcionalidades.md) | Épicas e historias `US-NNN` con criterios de aceptación |
| [07_flujos_principales.md](knowledge-base/07_flujos_principales.md) | Siete flujos extremo a extremo y sus errores |
| [08_arquitectura_propuesta.md](knowledge-base/08_arquitectura_propuesta.md) | Patrones, estructura conceptual, seguridad y variables de entorno |
| [09_decisiones_y_supuestos.md](knowledge-base/09_decisiones_y_supuestos.md) | Decisiones `DD-01..11` y supuestos `SU-01..07` |
| [10_preguntas_abiertas.md](knowledge-base/10_preguntas_abiertas.md) | Inconsistencias `IN-01..06` y prioridades: resolver ANTES de codear |

Las preguntas de prioridad **Alta** y las inconsistencias `IN-01..IN-06` se resuelven en `C-01`, no por convención del agente. Las fuentes `docs/discovery/` son solo lectura.

---

## Skills Disponibles

Fuente de verdad: [.atl/skill-registry.md](.atl/skill-registry.md). Solo se enumeran las skills marcadas `INSTALADA`; no se re-escanea el inventario.

| Agente / rol | Skills instaladas que carga | Estado |
|---|---|---|
| Orquestador (todas las fases) | `active-orchestrator`, `agent-instruction` | Activo |
| Fundación (discovery → registry) | `discovery-research`, `web-scraper`, `kb-creator`, `roadmap-generator`, `find-skills` | Completada; no re-ejecutar salvo pedido |
| Agente A — Backend Core | Sin skills de stack asignadas; depende de `C-01` | Post-`C-01` |
| Agente B — Backend Aux / integraciones | Sin skills de stack asignadas; depende de `C-01` | Post-`C-01` |
| Agente C — Capa visual / presentación | `design-system`, `design-taste-frontend-v1`, `emil-design-eng` | Diferidas con `visual-design` |
| Mantenimiento de skills | `skill-creator` | Bajo pedido |

Las cinco skills recomendadas (`test-driven-development`, `domain-modeling`, `codebase-design`, `web-security-review` y `mercadopago-claude-marketplace`) están **NO INSTALADAS**: requieren aprobación e instalación explícitas y no se asume su disponibilidad.

> Los compact rules de cada skill los resuelve el orquestador desde [.atl/skill-registry.md](.atl/skill-registry.md) (generado por `skill-registry`; no versionado — no está en el repo). Esta tabla solo mapea skill→rol.

---

## Roadmap de Changes

El plan operativo completo está en [CHANGES.md](CHANGES.md). Resumen:

- **Total**: 22 changes en 8 fases, 15 gates y 3 agentes (A/B/C).
- **Camino crítico** (10 changes, mínimo irreducible): `C-01 → C-02 → C-03 → C-04 → C-08 → C-11 → C-12 → C-13 → C-17 → C-18`.
- **Primer change**: `C-01` (`decisiones-habilitantes`), governance CRITICO, **solo análisis** y aprobación humana; bloquea `C-02` y casi toda la implementación.
- **Fuente operativa única**: `CHANGES.md` define alcance, dependencias, checkboxes y niveles de governance locales (ver su sección «Niveles de governance», no se repiten acá). El proyecto **no usa ClickUp**: `task-management` fue omitido en la fundación.

Antes de iniciar un item: leer [CHANGES.md](CHANGES.md), verificar dependencias y los archivos de «Leer antes», y confirmar que no queda un `(TBD)` que lo bloquee.

---

## Reglas Duras (específicas del proyecto)

> Reglas globales ya definidas en el archivo global de instrucciones del cliente activo (orquestador, governance, TDD, engram): el proyecto las hereda. Acá viven solo las reglas **específicas de este proyecto** y las que el global no cubre. Son contrato: romperlas es un defecto.

- **NUNCA** elegir lenguaje, framework, persistencia, topología de despliegue ni modelo multi-tenant antes de `C-01` → mantener `(TBD)`, registrar la decisión con decisor y fecha y esperar aprobación humana; no inventar valores por convención (RN-GL-03, DD-01).
- **NUNCA** usar [CHANGES.md](CHANGES.md) como documentación informativa ni introducir ClickUp u otra herramienta externa de seguimiento → es la única fuente operativa de alcance, dependencias y verificación (`task-management=skipped`).
- **NUNCA** crear `design-system/`, branding ni mockups en esta fundación → `visual-design=deferred`; al primer cambio visual, preguntar **una vez** la política (`system-and-mockups`/`system-only`/`off`) y aplicar la política global de `design-mockups` que el proyecto hereda.
- **NUNCA** presentar la seña con Mercado Pago ni la declaración de Ley 25.326 como innovación o vacío de mercado → incluirlas por **paridad competitiva y cumplimiento** (DD-05, DD-06). Los diferenciadores reales son: precio predecible y todo incluido, métricas verificables de ocupación y ausentismo, relleno automático de waitlist con depósito y cumplimiento **demostrable** con evidencia.
- **NUNCA** publicar cifras de adopción, ocupación o ausentismo sin metodología y período explícitos → toda métrica debe ser auditable; las cifras de la competencia no son verificables de forma independiente (DD-06, SU-03, SU-04).
- **NUNCA** resolver el anti-solapamiento solo en la interfaz → existe una restricción en la capa de datos para el mismo profesional **y** el mismo sillón ante escrituras concurrentes (RN-AG-01); el mecanismo concreto se decide en `C-01`.
- **NUNCA** fijar duración única por turno ni tratar la agenda como un simple registro → duración variable por tratamiento, bloqueos y *write-back* real (no un aviso) desde la reserva pública y hacia la agenda (RN-AG-03, RN-AG-06, DD-03, DD-04).
- **NUNCA** aplicar un plazo de cancelación/reprogramación distinto de **48 hs hábiles** → ventana configurable con feriados y zona horaria a confirmar en `C-01` (DD-07, SU-05).
- **NUNCA** implementar notificaciones sin WhatsApp Business API → es obligatoria en el MVP, con plantillas aprobadas y patrón *outbox* + webhooks idempotentes; la implementación concreta es `(TBD)` (RN-NT-01/03, DD-02).
- **NUNCA** usar nomenclatura dental propia ni sobrescribir la historia clínica → odontograma **FDI** incluidas piezas temporales, versionado sin sobrescritura y firma de evolución con matrícula (RN-CL-01/02/03).
- **NUNCA** tratar roles, auditoría, exportación, respaldo y dossier como reportes opcionales → son capacidades de dominio y la base del cumplimiento demostrable: log append-only, exportación/copia en 48 h y restore probado en entorno aislado antes de escribir sobre datos de usuario (RN-SE-02/03/04/05).
- **NUNCA** invocar skills marcadas `NO INSTALADA` ni instalar dependencias o servicios externos nuevos durante esta fundación → requieren aprobación e instalación explícitas; presupuesto acotado, sin commits y sin cambios bajo `openspec/`.

---

## Flujo de Trabajo

```
1. Leer la KB relevante (knowledge-base/)              → dominio, reglas RN-*, (TBD) que afectan el item
2. Leer CHANGES.md y verificar C-01 + dependencias     → si un (TBD) bloquea, detenerse y pedir decisión humana
3. Cargar la skill del rol desde .atl/skill-registry.md → solo skills INSTALADA
4. Acordar alcance y criterios de aceptación           → respetar el governance local de CHANGES.md
5. Implementar y verificar la definición de done       → registrar evidencia (tests, decisiones, aprobaciones)
6. Marcar [x] en CHANGES.md                            → solo después de la verificación técnica
```

Ante conflicto entre la KB y este archivo, prevalecen las reglas duras. Ante conflicto entre fuentes de discovery, prevalece [knowledge-base/10_preguntas_abiertas.md](knowledge-base/10_preguntas_abiertas.md) y se eleva a `C-01`.
<!-- /active-stack:agent-instruction -->
