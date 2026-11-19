# Descripción General

> Generado con `kb-creator` (variante `interview`). **No se impone stack tecnológico**: la entrevista lo prohíbe explícitamente (`docs/discovery/discovery.md` §9). Todo lo que no fue decidido se marca `(TBD)`.

## Stack tecnológico

| Capa | Tecnologías | Versión mínima | Estado |
|---|---|---|---|
| Cliente / reserva pública | `(TBD)` | `(TBD)` | La entrevista exige link/QR y autogestión, pero no define tecnología |
| Backend / API | `(TBD)` | `(TBD)` | Sin imposición; restricción presupuestaria |
| Persistencia | `(TBD)` | `(TBD)` | Debe soportar bloqueo transaccional anti-solapamiento |
| Autenticación de staff | `(TBD)` | `(TBD)` | RBAC obligatorio; esquema no definido |
| Colas / trabajos programados | `(TBD)` | `(TBD)` | Recordatorios, relleno de waitlist y respaldos |
| Infraestructura / despliegue | `(TBD)` | `(TBD)` | ¿Cloud, on-prem o local? `(TBD)` |
| Lenguaje/framework preferido del equipo | `(TBD)` | `(TBD)` | Pregunta P4 sin responder |

**Restricciones duras registradas**: presupuesto acotado; cumplimiento de Ley 25.326 (y Ley 26.529 / Decreto 1089/2012 según el informe); sin stack impuesto; sin servicios externos nuevos más allá de las integraciones ya previstas. `needs_infra`: `(TBD)` — dependerá de la decisión de despliegue y de si la base de datos es local o administrada.

## Arquitectura general

Diagrama **lógico** (no una decisión de implementación):

```
[ Paciente ] --link/QR--> [ Capa pública de reserva ] --+
[ Recepcionista / Odontólogo ] --> [ Capa de aplicación ]  |
                                        |                  |
                                        +--> [ Dominio: Agenda, Clínico, Financiero,
                                               Notificaciones, Identidad, Auditoría ]
                                                 |
                                        [ Persistencia / transacciones ]
                                                 |
                    +----------------------------+-----------------------------+
                    |                            |                             |
             [ WhatsApp API ]            [ Mercado Pago ]             [ ARCA / OS / Google / IA ]
               MVP obligatoria            MVP por paridad               posterior o (TBD)
```

**Decisiones de alto nivel ya justificadas por la entrevista**:

1. El anti-solapamiento no puede resolverse solo en la interfaz: debe existir una restricción en la capa de datos (RN-AG-01). El mecanismo concreto es `(TBD)`.
2. La agenda es la fuente de verdad: reserva, cancelación y reprogramación externas deben hacer *write-back* real (RN-AG-03), no enviar un aviso.
3. Auditoría y exportación son capacidades de dominio, no un reporte opcional: son la base del cumplimiento demostrable.
4. Las notificaciones de WhatsApp y los eventos de pago son asíncronos: se propone patrón *outbox* + webhooks con idempotencia; la implementación es `(TBD)`.

**No definido**: topología de despliegue, multi-tenancy, separación por sede, estrategia de caché, lenguaje, framework, base de datos y cola. Ver [09_decisiones_y_supuestos.md](09_decisiones_y_supuestos.md) DD-01 y [10_preguntas_abiertas.md](10_preguntas_abiertas.md).

## Integraciones externas

| Servicio | Propósito | Tipo | Alcance | Estado |
|---|---|---|---|---|
| WhatsApp Business API | Confirmación y recordatorio; plantillas aprobadas; handoff humano `(TBD)` | REST + webhook (detalles `(TBD)`) | MVP, obligatoria | Confirmado (`discovery` §8) |
| Mercado Pago | Señas, caja y conciliación | REST + webhook | MVP, por paridad competitiva | Confirmado (`discovery` §8; corrección del informe) |
| ARCA | Facturación y conciliación | REST/SDK `(TBD)` | `(TBD)` — contradictorio entre fuentes | IN-01 |
| Google Calendar | Sincronización | API `(TBD)` | Etapa posterior | Confirmado como posterior |
| Validación/liquidación de obras sociales | SOAP/API por obra social `(TBD)` | Integración local `(TBD)` | Etapa posterior, alto costo | Confirmado como posterior |
| IA conversacional con derivación clínica | Atención por WhatsApp y handoff | API `(TBD)` | `(TBD)` | discovery §11; informe §C.2 |
| FHIR / PACS / radiología | Interoperabilidad e imágenes | `(TBD)` | Etapa posterior | informe §D.3 |

> **Corrección vigente**: la seña con Mercado Pago **no** es un vacío de mercado (DentalSoft, AgendaPro, FLAP, DentApp y Mi Agenda Profesional la documentan). Se incluye por paridad, no como innovación (`informe-discovery.md` «Nota preliminar», punto 1).

## API REST (si aplica)

Superficie **indicativa**, no contrato cerrado; los paths, verbos, formatos y versionado son `(TBD)`:

| Recurso | Operaciones previstas | Actor |
|---|---|---|
| `/agenda` y `/turnos` | consultar disponibilidad, crear, confirmar, cancelar, reprogramar | Paciente, Recepcionista, Odontólogo |
| `/bloqueos`, `/sobreturnos` | crear, consultar, eliminar | Recepcionista |
| `/pacientes`, `/fichas`, `/odontogramas` | consultar, registrar, versionar | Odontólogo, Recepcionista |
| `/presupuestos`, `/señas`, `/caja` | crear, consultar, conciliar | Recepcionista, Administrador `(TBD)` |
| `/notificaciones`, `/webhooks` | enviar plantillas, recibir eventos | Sistema |
| `/auditoria`, `/exportaciones`, `/respaldos` | consultar, generar, descargar | Administrador `(TBD)` |

## Fuentes

- `docs/discovery/discovery.md` §§5, 8, 9, 11.
- `docs/discovery/informe-discovery.md` «Nota preliminar», §§A.1, A.2, C.2, C.4, D.3.
