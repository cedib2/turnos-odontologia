# Preguntas Abiertas

> Registro de entrevista: `docs/discovery/discovery.md` y `docs/discovery/informe-discovery.md`. Según la instrucción de esta fase, **no se preguntó al usuario**: todo lo que el registro no responde queda como `(TBD)` y se lista aquí.

## Inconsistencias detectadas

### IN-01 — Facturación ARCA: restricción, etapa posterior o MVP
**Documento A dice**: `discovery.md` §8 y §6 ubican ARCA en etapas posteriores; §9 la lista como restricción de cumplimiento.  
**Documento B dice**: `informe-discovery.md` §D.3 la incluye entre los elementos imprescindibles del MVP («facturación ARCA y conciliación»).  
**Impacto**: define si el alcance financiero del MVP es solo presupuesto+seña o incluye facturación electrónica; afecta entidades, integraciones y coste.  
**Resolución propuesta**: separar «cumplimiento normativo general» de «facturación electrónica operativa»; decidir DD-09 con el Product Owner. `(TBD)`

### IN-02 — Seña con Mercado Pago: pregunta abierta vs. decisión implícita
**Documento A dice**: `discovery.md` §11 pregunta si incluirla desde el MVP por paridad.  
**Documento B dice**: `discovery.md` §8 la da por incluida en el MVP por paridad; el informe confirma que ya es estándar.  
**Impacto**: bajo; la fuente más específica (§8) prevalece.  
**Resolución propuesta**: tratarla como DD-05 (incluida por paridad) y confirmar solo la política de reembolso `(TBD)`.

### IN-03 — Cantidad de sistemas comparados
**Documento A dice**: `discovery.md` encabezado menciona 17 sistemas (PDF original).  
**Documento B dice**: `informe-discovery.md` amplía a 22 sistemas con matriz ponderada.  
**Impacto**: ninguno en el producto; afecta la trazabilidad documental.  
**Resolución propuesta**: usar 22 como cifra vigente y conservar el PDF como fuente original.

### IN-04 — Sobreturnos vs. anti-solapamiento
**Documento A dice**: `discovery.md` §5 permite sobreturnos.  
**Documento B dice**: `discovery.md` §7 prohíbe turnos superpuestos en profesional o sillón.  
**Impacto**: RN-AG-01 y RN-AG-03 pueden contradecirse en implementación.  
**Resolución propuesta**: definir el sobreturno como excepción auditada o como turno en otro recurso; SU-06. `(TBD)`

### IN-05 — Alcance del dossier de cumplimiento
**Documento A dice**: `discovery.md` §5 pide roles, auditoría, exportación y respaldo; §9 pide Ley 25.326 y facturación ARCA.  
**Documento B dice**: `informe-discovery.md` §C.4 exige DPA, cláusulas AAIP 198/2023, RNBD, exportación en 48 h, retención de 10 años y procedimiento INTIMA/CONSTA.  
**Impacto**: define el alcance real de RN-SE-03/RN-SE-04 y su coste.  
**Resolución propuesta**: adoptar el dossier como objetivo, priorizando lo implementable con presupuesto acotado. `(TBD)`

### IN-06 — Datos de salud: Ley 25.326 vs. Ley 26.529
**Documento A dice**: `discovery.md` §9 solo nombra Ley 25.326.  
**Documento B dice**: el informe suma Ley 26.529 art. 14, Decreto 1089/2012 (10 años) y Ley 25.506 (firma).  
**Impacto**: cambia plazos, conservación y derechos de acceso del paciente.  
**Resolución propuesta**: tratar el marco ampliado como requerimiento; confirmar aplicabilidad con asesoría legal. `(TBD)`

## Preguntas abiertas (priorizadas)

| Prioridad | Pregunta | Bloquea | Decisor | Estado |
|---|---|---|---|---|
| Alta | ¿La facturación y conciliación con ARCA entran en el MVP o quedan para etapa posterior? (IN-01, DD-09) | Alcance financiero y diseño de datos | Product Owner | `(TBD)` |
| Alta | ¿Stack tecnológico y criterio de selección, dado que no hay imposición y el presupuesto es acotado? (P4, DD-01) | Arquitectura física y estimación | Equipo técnico | `(TBD)` |
| Alta | ¿Tipo de sistema y escala: web app, SaaS multi-tenant o consultorio único? (P0-sys/P0-scale) | Autenticación, aislamiento e infra | Product Owner | `(TBD)` |
| Alta | ¿Consultorio único o multisede desde el día 1? (discovery §11, DD-10) | Modelo de datos y RBAC | Product Owner | `(TBD)` |
| Alta | ¿Seña/depósito con Mercado Pago: política de reembolso y retención del hueco ante pago rechazado? (IN-02, RN-FI-03) | Flujo 5 | Product Owner | `(TBD)` |
| Alta | ¿Qué significa operativamente «sobreturno» sin romper anti-solapamiento? (IN-04, RN-AG-03) | RN-AG-01 | Product Owner + Odontólogo | `(TBD)` |
| Alta | ¿Responsable de datos, inscripción RNBD/AAIP, DPA y cláusulas AAIP 198/2023 para subprocessores? (RN-SE-04) | Cumplimiento demostrable | Responsable legal | `(TBD)` |
| Media | ¿IA conversacional en MVP, con handoff clínico registrado? (discovery §11, DD-11) | Notificaciones e historia clínica | Product Owner | `(TBD)` |
| Media | ¿Copia de historia en 48 h y retención de 10 años confirmadas como parámetros del sistema? (IN-05, IN-06) | Exportación y borrado | Responsable legal | `(TBD)` |
| Media | ¿Zona horaria, calendario de feriados y cómputo exacto de las 48 hs hábiles? (RN-AG-05, SU-05) | Agenda y recordatorios | Consultorio | `(TBD)` |
| Media | ¿Relleno automático de lista de espera con depósito y métricas en v0.1 o como diferenciador posterior? (US-015) | Épica 7 | Product Owner | `(TBD)` |
| Media | ¿Estructura de anamnesis, catálogo de tratamientos y duraciones por tratamiento? (US-002, US-008) | Seed data | Odontólogo | `(TBD)` |
| Media | ¿Objetivos numéricos de adopción online, ocupación y ausentismo? (métricas de éxito) | Aceptación del MVP | Product Owner | `(TBD)` |
| Media | ¿Modelo comercial: precio único todo incluido, manejo de costos de Meta y API? (RN-NT-04, US-016) | Producto y precio público | Product Owner | `(TBD)` |
| Media | ¿Despliegue cloud/on-prem/local y necesidades reales de infra (`needs_infra`)? (P4) | Seguridad y backups | Equipo técnico | `(TBD)` |
| Baja | ¿Migración de datos desde sistemas anteriores y exportación al salir? | Onboarding | Product Owner | `(TBD)` |
| Baja | ¿Identidad del paciente para acceso a su historia y vencimiento del token de enlace/QR? (Flujos 1 y 6) | Seguridad de superficie pública | Equipo técnico | `(TBD)` |

## Resumen explícito de ítems `(TBD)`

**Alcance y producto**: ARCA en MVP; IA en MVP; multisede; waitlist/métricas en v0.1; precio único; migración de datos; objetivos de métricas; horas hábiles y feriados; duraciones y catálogo de tratamientos; estructura de anamnesis.

**Arquitectura y técnica**: stack completo (frontend, backend, persistencia, auth, colas, despliegue); `system_type`; escala; `needs_infra`; residencia de datos; mecanismo de anti-solapamiento en base de datos; estrategia de token/expiración; verificación de webhooks; librerías y formatos; nombres y valores de variables de entorno.

**Roles y permisos**: nombre del rol administrador/responsable; rol auditor; rol dirección; herencia y permisos efectivos; acceso del odontólogo a agenda ajena; acceso de recepcionista a historia; ámbito de acceso del paciente.

**Datos y reglas**: tipos de documento, campos opcionales del paciente, estados del turno, semántica de sobreturno, asociación presupuesto–turno, conciliación, política de reembolso, formato/frecuencia de exportación y respaldo, proveedor de firma electrónica, borrado/soft delete, plazos de copia y retención, RNBD/AAIP y subprocessores.

> **Nota de proceso**: en esta ejecución no se modificó `.active-orchestrator-state.json`, aunque el contrato de la skill prevé un hook `state.kb` en ejecuciones orquestadas; la instrucción de la fase prohíbe expresamente tocarlo. La fase siguiente (roadmap) debe registrar la metadata de KB desde estos archivos si corresponde.

## Fuentes

- `docs/discovery/discovery.md` §§5–11.
- `docs/discovery/informe-discovery.md` «Nota preliminar», §§C.3, C.4, D.1–D.3.
