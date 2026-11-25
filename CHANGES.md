# CHANGES — Secuencia de Implementación

> Índice canónico de todos los changes del proyecto **turnos-odontologia**.
> Cada change es atómico: un agente puede implementarlo en una sesión (~4-6 horas).
> **Leer este archivo antes de empezar un item.**

---

## Cómo usar este documento

1. Identificar el item a implementar y verificar que sus dependencias figuran completas en este roadmap.
2. Leer los archivos de la `knowledge-base/` indicados en "Leer antes", con foco en los ítems `(TBD)` que lo afectan.
3. Acordar el alcance y los criterios de aceptación del item; si el governance lo exige, obtener la aprobación humana explícita antes de escribir código o datos.
4. Implementar, verificar los criterios acordados y registrar la evidencia (tests, decisiones, aprobaciones).
5. Marcar el checkbox `[x]` en este archivo al completar la verificación técnica.

Los IDs se conservan al editar o reordenar. No se reciclan IDs retirados. El proyecto **no usa ClickUp** (`task-management` omitido en la fundación): este índice local es la única fuente operativa de alcance y dependencias.

### Contexto no negociable (corrección de mercado)

- La seña con **Mercado Pago** y la declaración de **Ley 25.326** **no** son vacíos de mercado: ya están documentadas en la competencia. Se incluyen por paridad y cumplimiento, no como innovación (DD-05, DD-06).
- Los diferenciadores reales son: **precio predecible y todo incluido**, **métricas verificables de ocupación y ausentismo**, **relleno automático de la lista de espera con depósito** y **cumplimiento demostrable con evidencia**.
- Presupuesto acotado, sin stack impuesto y sin servicios externos nuevos más allá de los previstos. Todo lo no decidido permanece `(TBD)` y se resuelve en `C-01`; no se inventa valor por convención (RN-GL-03).
- Restricciones de esta fundación: sin instalaciones, sin commits, sin cambios bajo `openspec/`. La elección visual queda diferida (`visual-design=deferred`).

### Niveles de governance

| Nivel | Regla operativa en este roadmap |
|---|---|
| **CRITICO** | Auth, facturación/cobros, seguridad, logs de auditoría y modelos core que todo referencia. **Solo análisis**; requiere aprobación humana explícita antes de codear. |
| **ALTO** | Configuración que afecta datos de usuario, inyección de archivos en dotfiles del usuario, exportación/respaldo/restore y notificaciones. **Proponer y esperar revisión** antes de escribir. |
| **MEDIO** | Lógica de negocio, adaptadores de dominio y pipelines. **Implementar con checkpoints**. |
| **BAJO** | CRUD simples, catálogos, definiciones de tipos y utilidades de solo lectura. **Autonomía completa si pasan los tests**. |

### Reservas de numeración de migraciones Flyway

> Aprobado por el PO el 2026-11-21. Esta tabla **prevalece** sobre cualquier número de migración mencionado en el scope de un change. Ninguna migración ya aplicada se edita.

| Parte | Responsable | Rango reservado | Asignación |
|---|---|---|---|
| Parte 1 — Tronco Base | PO de esta rama | V001–V009 | V001 = C-03; V002 = C-04; V003 = C-05; V004 = C-06; V005 y V006 = C-08 (ya aplicadas). C-07 **reutiliza V005**: no crea migración si es solo CRUD sobre tablas existentes; si necesita esquema o seeds, usa un número nuevo dentro del rango (V007 en adelante). V008–V009 quedan como holgura de la Parte 1. |
| Parte 2 | compañero | desde V010 | V010 = C-10; V011 = C-11; V012 = C-12; V013 = C-13; siguientes a asignar por su dueño |
| Parte 3 | compañero | desde V020 | a asignar por su dueño |
| Parte 2 | compañero | desde V010 | a asignar por su dueño |
| Parte 3 | compañero | desde V020 | V020 = C-14; V021 = C-15; V022 en adelante queda como holgura |
| Parte 4 | compañero | desde V030 | a asignar por su dueño |
| Parte 4 | Joaquín Adrián Velázquez Carrera | desde V030 | V030 = C-17 (`cumplimiento_operaciones`); V031 = C-18 (`dossier_items`, `subprocesadores`). |

Los números históricos citados en los scopes (`C-10 = 007`, `C-11 = 008`, `C-14 = 009`, `C-15 = 010`, `C-16 = 011`, `C-17 = 012`, `C-19 = 013`) quedan **supersedidos** por esta tabla: cada dueño elige dentro de su rango y lo coordina antes de crear la migración.

**Nota `outOfOrder`**: V002–V004 se incorporan por debajo de V005/V006, que ya están aplicadas en entornos locales y de test. En una base donde V006 ya quedó registrada, Flyway clasifica V002–V004 como `IGNORED` salvo `outOfOrder=true` (`flyway.outOfOrder=true` o `spring.flyway.out-of-order=true` en Spring Boot). En una base nueva se aplican en orden, sin esa opción. Habilitar `spring.flyway.out-of-order=true` en dev/test antes de C-04/C-05/C-06; no existe base de producción con V006 porque C-08 no es desplegable.

### Orden operativo de la Parte 1 (decisión del PO, 2026-11-21)

> **Decisor**: Lautaro Ellerbach (PO). **Motivo**: C-06 desbloquea C-09 y C-13 de los compañeros, y C-05 puede reemplazarse después sobre el puerto `AuditEventPublisher`. Registro completo: [`docs/decisiones/C-05-secuencia-y-condiciones-PO.md`](docs/decisiones/C-05-secuencia-y-condiciones-PO.md). Esta secuencia **supersede** el orden previo `C-05 → C-04 → C-06`.

| Orden | Change | Nota |
|---|---|---|
| 1 | **C-04 `auth-rbac`** | Aporta el actor real; necesario para C-06 y después para C-05. |
| 2 | **C-06 `public-access-tokens`** | **Dependencia verificada**: según el scope de C-06 (línea «Dependencias»), depende **solo de C-04**; no figura C-05. |
| 3 | **C-05 `audit-append-only-log`** | Se integra después sobre `AuditEventPublisher` (hoy simulado en C-08). C-08 se marcó `[x]` el 2026-11-22, tras verificar C-04 y C-05 y resolver la deuda del adaptador de actor. |

---

## Árbol de dependencias

```
C-01 decisiones-habilitantes                 ← gate de aprobación humana (ANALYSIS ONLY)
  └── C-02 foundation-scaffolding
        └── C-03 core-models-and-migrations
              │
              ├── C-04 auth-rbac                          ← desbloquea superficie protegida
              │     └── C-06 public-access-tokens
              │
              ├── C-05 audit-append-only-log
              │
              └── C-07 scheduling-catalogs
                    │
                    └── C-08 agenda-core-and-overlap-prevention   ← JOIN (C-04 + C-05 + C-07)
                          │
                          ├── C-09 public-booking-and-reschedule  ← + C-06
                          │     ├── C-15 mercadopago-deposits-reconciliation  ← + C-14
                          │     │     ├── C-16 arca-invoicing-reconciliation (TBD IN-01)
                          │     │     ├── C-17 export-backup-restore          ← + C-13
                          │     │     │     └── C-18 compliance-evidence-dossier
                          │     │     ├── C-19 waitlist-auto-refill-metrics (TBD v0.1)
                          │     │     └── C-21 precio-predecible-costos
                          │     └── C-22 adoption-metrics-dashboard          ← + C-18
                          ├── C-10 whatsapp-notifications-outbox
                          │     └── C-20 ia-conversacional-handoff (TBD DD-11)
                          ├── C-11 clinical-record-odontogram
                          │     └── C-12 clinical-signature-consent
                          │           ├── C-13 patient-clinical-record-access  ← + C-06
                          │           └── C-20 ia-conversacional-handoff (TBD DD-11)
                          └── C-14 budgets-and-cash
```

### Paralelismo por fase

> Cada gate es un punto de sincronización. Los changes dentro de un gate pueden ejecutarse en paralelo.

```
GATE 0: ninguna                              ← ANALYSIS ONLY + aprobación humana
  → C-01 decisiones-habilitantes             [Agente A — análisis]

GATE 1: C-01 ✓
  → C-02 foundation-scaffolding              [Agente A]

GATE 2: C-02 ✓
  → C-03 core-models-and-migrations          [Agente A]

GATE 3: C-03 ✓                               ← PRIMER FORK (3 paralelos)
  → C-04 auth-rbac                           [Agente A]
  → C-05 audit-append-only-log               [Agente B]
  → C-07 scheduling-catalogs                 [Agente C]

GATE 4: C-04 ✓
  → C-06 public-access-tokens                [Agente A]

GATE 5: C-04 + C-05 + C-07 ✓                 ← JOIN
  → C-08 agenda-core-and-overlap-prevention  [Agente A]

GATE 6: C-08 ✓                               ← FORK (3 paralelos)
  → C-09 public-booking-and-reschedule       [Agente A — si C-06 ✓]
  → C-10 whatsapp-notifications-outbox       [Agente B — si C-05 ✓]
  → C-11 clinical-record-odontogram          [Agente C — si C-05 ✓]

GATE 7: C-11 ✓
  → C-12 clinical-signature-consent          [Agente C]

GATE 8: C-05 + C-07 + C-08 ✓
  → C-14 budgets-and-cash                    [Agente B]

GATE 9: C-06 + C-09 + C-12 + C-14 ✓          ← FORK (3 paralelos)
  → C-13 patient-clinical-record-access      [Agente A]
  → C-15 mercadopago-deposits-reconciliation [Agente B]
  → C-20 ia-conversacional-handoff           [Agente C — si C-01 habilita DD-11]

GATE 10: C-01 + C-15 ✓
  → C-16 arca-invoicing-reconciliation       [Agente B — solo si IN-01 = MVP]

GATE 11: C-13 + C-15 ✓
  → C-17 export-backup-restore               [Agente A]

GATE 12: C-01 + C-17 ✓
  → C-18 compliance-evidence-dossier         [Agente B]

GATE 13: C-01 + C-09 + C-10 + C-15 ✓         ← FORK condicional
  → C-19 waitlist-auto-refill-metrics        [Agente A — si C-01 habilita v0.1]
  → C-21 precio-predecible-costos            [Agente C — si C-01 fija RN-SE-06]

GATE 14: C-09 + C-18 ✓
  → C-22 adoption-metrics-dashboard          [Agente C]
```

### Camino crítico (10 changes — mínimo irreducible)

```
C-01 → C-02 → C-03 → C-04 → C-08 → C-11 → C-12 → C-13 → C-17 → C-18*
```

`*` La rama financiera `C-08 → C-14 → C-15` converge en `C-17` con longitud igual; el dossier de cumplimiento (`C-18`) cierra el alcance indispensable porque la declaración ya no diferencia: hace falta evidencia (RN-SE-04). Los changes `C-16`, `C-19`, `C-20`, `C-21` y `C-22` no son irreducibles: dependen de decisiones `(TBD)` o son reporting diferido.

### Plan óptimo con 3 agentes

```
Paso │ Agente A (Backend Core)        │ Agente B (Backend Aux)          │ Agente C (Frontend/Adaptadores)
─────┼────────────────────────────────┼─────────────────────────────────┼────────────────────────────────────
  1  │ C-01 decisiones-habilitantes   │         —                       │         —
  2  │ C-02 foundation-scaffolding    │         —                       │         —
  3  │ C-03 core-models-and-migrations│         —                       │         —
  4  │ C-04 auth-rbac                 │ C-05 audit-append-only-log      │ C-07 scheduling-catalogs
  5  │ C-06 public-access-tokens      │         —                       │         —
  6  │ C-08 agenda-core-and-overlap   │         —                       │         —
  7  │ C-09 public-booking            │ C-10 whatsapp-notifications     │ C-11 clinical-record-odontogram
  8  │         —                      │ C-14 budgets-and-cash           │ C-12 clinical-signature-consent
  9  │ C-13 patient-clinical-access   │ C-15 mercadopago-deposits       │ C-20 ia-conversacional-handoff *
 10  │         —                      │ C-16 arca-invoicing-reconciliation * │      —
 11  │ C-17 export-backup-restore     │         —                       │         —
 12  │         —                      │ C-18 compliance-evidence-dossier │         —
 13  │ C-19 waitlist-auto-refill *    │         —                       │ C-21 precio-predecible-costos *
 14  │         —                      │         —                       │ C-22 adoption-metrics-dashboard
```

`*` Condicionado a la decisión registrada en `C-01`; si no se habilita, el agente queda libre y la ficha se cierra como diferida sin código.

---

## FASE 0 — Decisiones habilitantes

> No hay código en esta fase. Es el único punto donde se pueden resolver los `(TBD)` que bloquean el alcance.

### [C-01] `decisiones-habilitantes`
- **Estado**: `[x]` completado
- **Scope**:
  - Registrar, sin código, la resolución de: **IN-01 / DD-09** (facturación y conciliación ARCA en MVP o posterior), **DD-01** (lenguaje/framework, persistencia, `system_type`, escala, `needs_infra`, cloud/on-prem/local), **DD-10** (consultorio único vs multisede), **DD-11** (IA conversacional en MVP), **IN-04 / SU-06** (semántica operativa de sobreturno), **IN-05 / IN-06** (responsable de datos, marco Ley 25.326 + Ley 26.529 + Decreto 1089/2012 + Ley 25.506, plazos 48 h y 10 años), **IN-02** (reembolso y retención del hueco), zona horaria/feriados y cómputo de 48 hs hábiles, catálogo de tratamientos y duraciones, estructura de anamnesis, objetivos numéricos de métricas y modelo de precio único (RN-NT-04, RN-SE-06).
  - Registro de cada decisión con decisor (Product Owner / responsable legal / equipo técnico / odontólogo) y fecha; los ítems no respondidos siguen marcados `(TBD)` y bloquean la ficha que los consume.
  - Criterio de salida: aprobación humana explícita por decisor. Sin instalaciones, sin servicios nuevos, sin cambios bajo `openspec/`.
- **Dependencias**: ninguna
- **Governance**: CRITICO — solo análisis; aprobación humana explícita antes de cualquier código
- **Leer antes**:
  - `knowledge-base/10_preguntas_abiertas.md` §Inconsistencias detectadas (IN-01..IN-06), §Preguntas abiertas (priorizadas)
  - `knowledge-base/09_decisiones_y_supuestos.md` §Decisiones documentadas (DD-01, DD-09, DD-10, DD-11), §Supuestos inferidos (SU-01, SU-02, SU-05, SU-06, SU-07)
  - `knowledge-base/01_vision_y_objetivos.md` §Condicionado a decisión pendiente, §Métricas de éxito
  - `knowledge-base/02_descripcion_general.md` §Stack tecnológico, §Integraciones externas
  - `knowledge-base/08_arquitectura_propuesta.md` §Seguridad, §Restricciones operativas de esta fase

---

## FASE 1 — Cimientos, identidad y trazabilidad

> C-04, C-05 y C-07 son paralelos; C-04 y C-05 se reúnen en C-08 junto con C-07.

### [C-02] `foundation-scaffolding`
- **Estado**: `[x]` completado
- **Scope**:
  - Estructura conceptual `src/domain`, `src/application`, `src/infrastructure`, `src/interfaces`, `tests/`; `knowledge-base/` como fuente y `docs/discovery/` en solo lectura; sin `openspec/`.
  - Plantilla de entorno con los nombres propuestos en la KB (`APP_BASE_URL`, `DATABASE_URL`, `TIMEZONE`, `CANCELLATION_WINDOW_HOURS`, `WHATSAPP_*`, `MERCADOPAGO_*`, `EXPORT_RETENTION_YEARS`, `BACKUP_CRON`, `IA_HANDOFF_ENABLED`); valores sensibles como marcadores `(TBD)`, sin secretos reales; gestor de secretos `(TBD)`.
  - Health check indicativo (path/verbo `(TBD)`) y esqueleto de CI con jobs de lint y test (herramientas `(TBD)`); sin instalar dependencias en esta fase.
  - Tipos base por dominio (agenda, clínico, financiero, identidad, auditoría, notificaciones); lenguaje y convenciones `(TBD)`.
  - Tests: humo de estructura, health check, plantilla de entorno sin secretos.
- **Dependencias**: C-01
- **Governance**: BAJO
- **Leer antes**:
  - `knowledge-base/08_arquitectura_propuesta.md` §Estructura de directorios, §Variables de entorno
  - `knowledge-base/02_descripcion_general.md` §Arquitectura general, §Stack tecnológico
  - `knowledge-base/01_vision_y_objetivos.md` §Alcance v0.1 (MVP acordado)
  - `knowledge-base/10_preguntas_abiertas.md` §Resumen explícito de ítems `(TBD)` (arquitectura y técnica)

---

### [C-03] `core-models-and-migrations`
- **Estado**: `[x]` completado
- **Scope**:
  - Migración 001: entidades base `Paciente` (documento y campos opcionales `(TBD)`), `Profesional` (matrícula), `Sillón`, `Consultorio` (único vs multisede `(TBD)`), `Usuario`, `Rol`, `Permiso`; nombres físicos `(TBD)`.
  - Mixins de auditoría (creación/actualización, activo/soft delete `(TBD)`), repositorio genérico y unidad de trabajo; patrón de implementación `(TBD)`.
  - Índices declarados: `documento_identidad`, `telefono`, `profesional.activo`, `sillon.activo`.
  - Seed: roles base `paciente`, `recepcionista`, `odontólogo`, `administrador` `(TBD)`, `auditor` `(TBD)`, `dirección` `(TBD)`; permisos efectivos y herencia `(TBD)`; datos personales tratados conforme Ley 25.326 (mecanismo `(TBD)`).
  - Tests: creación de entidades, constraints, aislamiento por sede si aplica `(TBD)`, seed de roles.
- **Dependencias**: C-02
- **Governance**: CRITICO — modelos core que todo lo demás referencia; aprobación humana antes de fijar el esquema
- **Leer antes**:
  - `knowledge-base/04_modelo_de_datos.md` §Dominios, §Entidades (Paciente, Profesional y Sillón, Usuario/Rol/Permiso), §Seed data inicial
  - `knowledge-base/03_actores_y_roles.md` §Actores del sistema, §RBAC — Matriz de permisos
  - `knowledge-base/08_arquitectura_propuesta.md` §Patrones (Repository, UoW, soft delete/versionado)
  - `knowledge-base/09_decisiones_y_supuestos.md` §Supuestos inferidos (SU-01, SU-02)

---

### [C-04] `auth-rbac`
- **Estado**: [x] completado — implementado, verificado y archivado el 2026-11-21; 147 tests verdes (93 previos + 54 nuevos), `openspec validate auth-rbac --strict` válido; specs sincronizadas
- **Scope**:
  - Endpoints indicativos `POST /auth/login`, `POST /auth/refresh`, `POST /auth/logout`, `GET /auth/me` (paths, verbos y versionado `(TBD)`); mecanismo de sesión/token y cookie segura `(TBD)`; expiración y rotación `(TBD)`; rate limiting (umbrales `(TBD)`).
  - Middleware de autorización `require_role` / `require_admin` implementando la matriz de roles, con ámbitos propio/todos/config; herencia y permisos efectivos `(TBD)`.
  - Campos de sesión (`sub`, rol, `jti`, `iat`, `exp` — esquema `(TBD)`); secretos por entorno, nunca en repositorio; acceso del odontólogo a agenda ajena `(TBD)`.
  - Migración 002: tablas de sesiones/tokens de staff.
  - Tests: login, token expirado, rotación, deny por rol, rate limit, sin secretos en logs.
- **Decisión PO (2026-11-21)**: ver [`docs/decisiones/C-04-decisiones-auth-rbac.md`](docs/decisiones/C-04-decisiones-auth-rbac.md). Access 15 min / refresh 7 días con rotación; logout revoca sesión y familia; `spring-boot-starter-security` aprobado con guardias acotadas al paquete `auth` (agenda/catálogos siguen sin autenticación en dev/test y sus 93 tests deben permanecer verdes); secretos por entorno con `.env` ignorado y `.env.example` sin valores; rate limit en memoria por IP y usuario (5 intentos fallidos / 15 min, 429 + `Retry-After`). Valores = metas iniciales, no normativas. Apply y archive aprobados por el PO el 2026-11-21; ver registros en `docs/decisiones/`.
- **Dependencias**: C-03
- **Governance**: CRITICO — auth; solo análisis y aprobación humana antes de codear
- **Leer antes**:
  - `knowledge-base/03_actores_y_roles.md` §RBAC — Matriz de permisos, §Rutas públicas
  - `knowledge-base/05_reglas_de_negocio.md` §Dominio: Seguridad y cumplimiento (RN-SE-01)
  - `knowledge-base/08_arquitectura_propuesta.md` §Seguridad (Autenticación, Autorización, Gestión de secretos)
  - `knowledge-base/07_flujos_principales.md` §Flujo 6: Acceso, exportación, respaldo y evidencia
  - `knowledge-base/10_preguntas_abiertas.md` §Resumen explícito de ítems `(TBD)` (Roles y permisos)

---

### [C-05] `audit-append-only-log`
- **Estado**: `[x]` completado — verificado y archivado el 2026-11-22; specs sincronizadas
- **Scope**:
  - Migración 003: registro append-only con actor, fecha, entidad, entidad_id, IP, dispositivo `(TBD)` y hash encadenado `(TBD)`; inmutabilidad garantizada en capa de datos.
  - Eventos obligatorios: creación/cancelación/reprogramación de turno, firma clínica, cobro/conciliación, exportación/respaldo/restore, cambios de roles y configuración; catálogo de nombres de eventos `(TBD)`.
  - Adaptador de auditoría invocado por los casos de uso; API de consulta solo lectura para administrador/auditor `(TBD)`; retención del log `(TBD, propuesta 10 años)`; prueba de integridad `(TBD)`.
  - Tests: intento de UPDATE/DELETE rechazado, cobertura de eventos, verificación de cadena de hashes, control de permisos de lectura.
- **Orden PO (2026-11-21)**: ejecutar **después de C-04 y C-06** (ver § «Orden operativo de la Parte 1»). Se integra sobre el puerto `AuditEventPublisher` de C-08.
- **Pedidos del PO para cuando se retome (decisión 2026-11-21)**:
  1. Lista **exacta** del catálogo de eventos (nombres y granularidad); no inventarla por convención.
  2. Campos **prohibidos en `metadata`** con test explícito de rechazo (datos clínicos/PII).
  3. Documentar el **límite del hash sin clave** (SHA-256 vs HMAC) en la evidencia/dossier.
  4. Marcar la **consulta REST de auditoría como diferible**: no bloquea C-05 y queda `(TBD)` para un change posterior.
- **Dependencias**: C-03
- **Governance**: CRITICO — audit trail; aprobación humana antes de codear
- **Leer antes**:
  - `knowledge-base/05_reglas_de_negocio.md` §Dominio: Seguridad y cumplimiento (RN-SE-02, RN-SE-04)
  - `knowledge-base/04_modelo_de_datos.md` §AuditoríaLog y Evidencia
  - `knowledge-base/08_arquitectura_propuesta.md` §Patrones (Log append-only), §Seguridad (Auditoría)
  - `knowledge-base/06_funcionalidades.md` §US-014 — Auditar, exportar y respaldar

---

### [C-06] `public-access-tokens`
- **Estado**: `[x]` completado
- **Scope**:
  - Migración 004: token público por turno/enlace con expiración `(TBD)` para reserva, confirmación, gestión y acceso a historia.
  - Servicio de emisión/validación con firma y vencimiento (algoritmo y plazo `(TBD)`); verificación de identidad del paciente para historia `(TBD)`; protección contra reenvíos de operaciones críticas (RN-GL-02, mecanismo `(TBD)`).
  - Rutas públicas sin autenticación de staff listadas en la KB; webhooks de WhatsApp y Mercado Pago quedan en C-10 y C-15 con verificación de firma propia.
  - Tests: token vencido, token forjado, reutilización, identidad no verificada, reenvío duplicado.
- **Orden PO (2026-11-21)**: ejecutar **después de C-04 y antes de C-05**; desbloquea C-09 y C-13 de los compañeros (ver § «Orden operativo de la Parte 1»).
- **Dependencias**: C-04
- **Governance**: CRITICO — seguridad de la superficie pública; aprobación humana antes de codear
- **Leer antes**:
  - `knowledge-base/03_actores_y_roles.md` §Rutas públicas
  - `knowledge-base/08_arquitectura_propuesta.md` §Patrones (Tokens públicos con expiración), §Seguridad (Autenticación, Webhooks)
  - `knowledge-base/05_reglas_de_negocio.md` §Dominio: Agenda (RN-AG-06), §Excepciones globales (RN-GL-02)
  - `knowledge-base/10_preguntas_abiertas.md` §Preguntas abiertas (identidad del paciente y vencimiento del token)

---

## FASE 2 — Agenda operativa

> C-07 crea los catálogos referenciados por C-08 (duración, feriados y zona horaria); C-08 se apoya en C-03 y C-07. C-04 se difiere mediante `CurrentActorProvider` (actor fijo solo dev/test, sin autenticación ni RBAC) y C-05 mediante `AuditEventPublisher` (emisión en memoria/no-op, sin log append-only). C-08 NO se despliega a producción ni se marca como cumplimiento hasta que C-04 y C-05 estén implementados y aprobados.

### [C-07] `scheduling-catalogs`
- **Estado**: `[x]` completado — CRUD administrativo sobre V005; sin migración nueva ni seeds; verificado 2026-11-21
- **Scope**:
  - Migración 005: `Tratamiento` con duración variable (valores `(TBD)`), horarios laborales, feriados, zona horaria `(TBD)`, calendario `(TBD)`, anticipación mínima/máxima de reserva `(TBD)`.
  - CRUD administrativo indicativo `/tratamientos`, `/horarios`, `/feriados` (paths `(TBD)`) y servicio de consulta de configuración para disponibilidad; cambios reflejados en link/QR sin recargar (mecanismo `(TBD)`).
  - Seed: catálogo de tratamientos y duraciones `(TBD)`, horarios y feriados `(TBD)`; catálogo FDI se siembra en C-11.
  - Tests: CRUD, validación de duración positiva, solapamiento de franjas horarias `(TBD)`, permisos de configuración.
- **Dependencias**: C-03
- **Governance**: BAJO — catálogos y CRUD; autonomía si pasan los tests
- **Leer antes**:
  - `knowledge-base/04_modelo_de_datos.md` §Entidades (Bloqueo y Feriado), §Seed data inicial
  - `knowledge-base/06_funcionalidades.md` §US-002 — Administrar disponibilidad
  - `knowledge-base/05_reglas_de_negocio.md` §Dominio: Agenda (RN-AG-02, RN-AG-04)
  - `knowledge-base/09_decisiones_y_supuestos.md` §Supuestos inferidos (SU-05)

---

### [C-08] `agenda-core-and-overlap-prevention`
- **Estado**: `[x]` completado — puertos reemplazados: auditoría por C-05 y actor por `AuthenticatedCurrentActorProvider` (adaptador de `AuthenticatedActor`) con protección RBAC de la agenda fuera de dev/test; verificado 2026-11-22 (226/226). Ver [`docs/decisiones/C-08-cierre-adaptador-actor-rbac.md`](docs/decisiones/C-08-cierre-adaptador-actor-rbac.md).
- **Scope**:
  - Migración 006: `Turno` (`paciente_id`, `profesional_id`, `sillon_id`, `inicio`, `fin`, `duracion_minutos`, `tipo_tratamiento_id`, `estado` `(TBD)`, `es_sobreturno`, `origen` `(TBD)`, `token_publico`), `Bloqueo`, relación con feriados; índices `(profesional_id, inicio, fin)`, `(sillon_id, inicio, fin)`, `estado`, `token_publico`.
  - Restricción en base de datos que impide solapamiento para el mismo profesional **y** el mismo sillón ante escrituras concurrentes (RN-AG-01); mecanismo concreto `(TBD)`; bloqueos ocupan agenda y se respetan en disponibilidad.
  - Sobreturno como excepción auditada o turno en otro recurso `(TBD, IN-04)`; ante conflicto prevalece RN-AG-01 salvo override con motivo y actor (RN-GL-01); `write-back` real interno (RN-AG-06).
  - Endpoints indicativos `/agenda`, `/turnos`, `/bloqueos`, `/sobreturnos`; vista día/semana por profesional y sillón (US-001); capa de presentación `(TBD — visual-design diferido)`.
  - Tests: dos escrituras concurrentes sobre el mismo hueco, bloqueo respetado, sobreturno según regla resuelta, reprogramación atómica, emisión de eventos de auditoría.
- **Dependencias**: C-03, C-07 (C-04 y C-05 diferidos; ver nota de fase y puertos `CurrentActorProvider` / `AuditEventPublisher`)
- **Governance**: MEDIO — lógica de negocio central; implementar con checkpoints
- **Leer antes**:
  - `knowledge-base/04_modelo_de_datos.md` §Entidades (Turno, Bloqueo y Feriado)
  - `knowledge-base/05_reglas_de_negocio.md` §Dominio: Agenda (RN-AG-01, RN-AG-03, RN-AG-06), §Excepciones globales (RN-GL-01)
  - `knowledge-base/06_funcionalidades.md` §US-001 — Agenda multiprofesional y multisillón
  - `knowledge-base/08_arquitectura_propuesta.md` §Patrones (Restricción de integridad en base de datos, Write-back)
  - `knowledge-base/10_preguntas_abiertas.md` §Inconsistencias detectadas (IN-04)

---

## FASE 3 — Reserva pública y notificaciones

> C-09 depende del token público (C-06) y de la agenda (C-08). C-10 y C-11 arrancan en paralelo.

### [C-09] `public-booking-and-reschedule`
- **Estado**: [x] completado — implementado y verificado el 2026-11-22; 219 tests verdes, `openspec validate public-booking-and-reschedule --strict` válido; sin migraciones nuevas y sin commits
- **Scope**:
  - Endpoints públicos indicativos sobre `/turnos`: consultar disponibilidad, crear, confirmar, cancelar y reprogramar con token (paths, verbos y versionado `(TBD)`).
  - *Write-back* real y liberación atómica del hueco; si el hueco se tomó entre consulta y confirmación, rechazo y alternativas `(TBD)`; token vencido → solicitar nuevo enlace `(TBD)`.
  - Cálculo de la ventana de **48 hs hábiles** usando feriados y zona horaria de C-07 (cómputo exacto `(TBD)`); fuera de ventana deriva a recepción y no libera automáticamente `(TBD)`.
  - Idempotencia ante reenvíos (RN-GL-02) y emisión de eventos para WhatsApp y auditoría; estado de confirmación visible en agenda.
  - Tests: reserva/confirmación/cancelación/reprogramación E2E (Flujo 1 y 2), carrera por hueco, límite de ventana, reenvío duplicado.
- **Dependencias**: C-06, C-08
- **Governance**: MEDIO — flujo con estado y write-back público; checkpoints
- **Leer antes**:
  - `knowledge-base/07_flujos_principales.md` §Flujo 1: Reserva online por link/QR, §Flujo 2: Cancelación / reprogramación con ventana de 48 hs hábiles
  - `knowledge-base/06_funcionalidades.md` §US-003, §US-004, §US-005 (Épica 2)
  - `knowledge-base/05_reglas_de_negocio.md` §Dominio: Agenda (RN-AG-05, RN-AG-06, RN-AG-07)
  - `knowledge-base/09_decisiones_y_supuestos.md` §Decisiones documentadas (DD-03, DD-07)
  - `knowledge-base/03_actores_y_roles.md` §Rutas públicas

---

### [C-10] `whatsapp-notifications-outbox`
- **Estado**: `[x]` completado — V010 + outbox, intentos, webhook firmado e idempotente; adaptador real y ruta HTTP del webhook `(TBD)`; verificado 2026-11-22. Ver [`docs/decisiones/C-10-whatsapp-notifications-outbox.md`](docs/decisiones/C-10-whatsapp-notifications-outbox.md).
- **Scope**:
  - Migración 007: plantillas de WhatsApp, mensajes y tabla *outbox*; implementación del outbox `(TBD)`.
  - Adaptador de **WhatsApp Business API obligatoria** con plantillas aprobadas (códigos `(TBD)`) para confirmación y recordatorio (RN-NT-01); programación del recordatorio (anticipación `(TBD)`).
  - Webhook con verificación de firma `(TBD)` e idempotencia; reintentos con registro de estado, intentos y `proveedor_message_id` (RN-NT-03); si Meta rechaza la plantilla, bloquear envío y avisar a administración.
  - Costos y límites predecibles, sin packs sorpresa (RN-NT-04, modelo comercial `(TBD)`); handoff de IA `(TBD, DD-11)` se implementa en C-20.
  - Tests: envío con plantilla aprobada, plantilla rechazada, webhook sin firma, reintento duplicado, registro de intentos.
- **Dependencias**: C-05, C-08
- **Governance**: ALTO — sistema de notificaciones; proponer y esperar revisión antes de escribir
- **Leer antes**:
  - `knowledge-base/05_reglas_de_negocio.md` §Dominio: Notificaciones (RN-NT-01, RN-NT-03, RN-NT-04)
  - `knowledge-base/07_flujos_principales.md` §Flujo 3: Confirmación y recordatorio por WhatsApp
  - `knowledge-base/06_funcionalidades.md` §US-006 — Recordatorios y confirmaciones
  - `knowledge-base/08_arquitectura_propuesta.md` §Patrones (Outbox + webhooks idempotentes), §Seguridad (Webhooks)
  - `knowledge-base/09_decisiones_y_supuestos.md` §Decisiones documentadas (DD-02)

---

## FASE 4 — Historia clínica

> C-11 precede a C-12 (firma) y a C-13 (acceso del paciente). La historia no se sobrescribe: se versiona.

### [C-11] `clinical-record-odontogram`
- **Estado**: `[x]` completado — V011 + seed FDI (32 permanentes + 20 temporales), historia versionada append-only, permisos por rol y auditoría; verificado 2026-11-22. Ver [`docs/decisiones/C-11-clinical-record-odontogram.md`](docs/decisiones/C-11-clinical-record-odontogram.md).
- **Scope**:
  - Migración 008: `FichaClínica`, `Anamnesis` (estructura `(TBD)`), `Odontograma` (`pieza_fdi`, `temporal`, `cara` `(TBD)`, `estado`, `autor_id`, `version`), `Evolución`, `Consentimiento`.
  - Seed del catálogo FDI de piezas permanentes y **temporales**; nomenclatura FDI obligatoria (RN-CL-01); historia versionada sin sobrescritura (RN-CL-02); borrado físico `(TBD)`.
  - Endpoints indicativos `/pacientes`, `/fichas`, `/odontogramas` (paths `(TBD)`); apertura de ficha desde el turno; acceso de recepción a historia `(TBD)`; uno de los roles no confirmados `(TBD)`.
  - Tests: codificación FDI temporal/permanente, nueva versión ante intento de sobrescribir, permisos por rol, trazabilidad en auditoría.
- **Dependencias**: C-05, C-08
- **Governance**: MEDIO — lógica de dominio clínico; checkpoints
- **Leer antes**:
  - `knowledge-base/04_modelo_de_datos.md` §Entidades (FichaClínica, Anamnesis y Odontograma), §Seed data inicial
  - `knowledge-base/05_reglas_de_negocio.md` §Dominio: Clínico (RN-CL-01, RN-CL-02)
  - `knowledge-base/06_funcionalidades.md` §US-008 — Registrar ficha, anamnesis y odontograma
  - `knowledge-base/07_flujos_principales.md` §Flujo 4: Registro clínico y firma
  - `knowledge-base/03_actores_y_roles.md` §RBAC — Matriz de permisos (fila Ficha clínica)

---

### [C-12] `clinical-signature-consent`
- **Estado**: `[x]` completado — V012, firma de evolución con matrícula y hash, consentimiento por puerto de firma electrónica (proveedor y contingencia `(TBD)`), auditoría por firma; verificado 2026-11-22. Ver [`docs/decisiones/C-12-clinical-signature-consent.md`](docs/decisiones/C-12-clinical-signature-consent.md).
- **Scope**:
  - Firma de evolución con **matrícula** obligatoria del profesional; impedir firma si falta matrícula (RN-CL-03).
  - Consentimientos con firma electrónica conforme Ley 25.506; proveedor e implementación `(TBD)`; política de contingencia si el proveedor no está disponible `(TBD)`.
  - Ante intento de modificar una evolución firmada, crear nueva versión (RN-CL-02); escribir un evento de auditoría por cada firma (RN-SE-02).
  - Tests: firma sin matrícula, firma correcta, nueva versión, trazabilidad, contingencia `(TBD)`.
- **Dependencias**: C-05, C-11
- **Governance**: ALTO — integridad y validez legal de datos de usuario; proponer y esperar revisión
- **Leer antes**:
  - `knowledge-base/05_reglas_de_negocio.md` §Dominio: Clínico (RN-CL-03), §Dominio: Seguridad y cumplimiento (RN-SE-02)
  - `knowledge-base/06_funcionalidades.md` §US-009 — Firmar evolución y consentimientos
  - `knowledge-base/07_flujos_principales.md` §Flujo 4: Registro clínico y firma (casos de error)
  - `knowledge-base/09_decisiones_y_supuestos.md` §Supuestos inferidos (IN-06: Ley 25.506)
  - `knowledge-base/10_preguntas_abiertas.md` §Preguntas abiertas (responsable legal, firma electrónica)

---

### [C-13] `patient-clinical-record-access`
- **Estado**: `[x]` completado — V013, token `HISTORIA` + verificación de identidad (mecanismo `(TBD)`, cerrado fuera de dev/test), copia con plazo y hash, auditoría de accesos y denegaciones; verificado 2026-11-22. Ver [`docs/decisiones/C-13-patient-clinical-record-access.md`](docs/decisiones/C-13-patient-clinical-record-access.md).
- **Scope**:
  - Verificación de identidad del titular `(TBD)`; acceso y copia conforme Ley 26.529 art. 14 y plazo de 48 h `(TBD, parámetros operativos)`; alcance del acceso del paciente `(TBD)` (RN-CL-04).
  - Endpoint indicativo de consulta/descarga de la propia historia protegido por el token de C-06; registro en log append-only de cada acceso/exportación y de cada intento denegado.
  - Sin diagnósticos automatizados ni sustitución del criterio clínico.
  - Tests: identidad no verificada → denegación registrada, copia dentro del plazo `(TBD)`, auditoría de acceso.
- **Dependencias**: C-06, C-12
- **Governance**: ALTO — acceso a datos sensibles de usuario; proponer y esperar revisión antes de escribir
- **Leer antes**:
  - `knowledge-base/05_reglas_de_negocio.md` §Dominio: Clínico (RN-CL-04), §Dominio: Seguridad y cumplimiento (RN-SE-01, RN-SE-02)
  - `knowledge-base/06_funcionalidades.md` §US-010 — Acceso y copia de la historia por el paciente
  - `knowledge-base/07_flujos_principales.md` §Flujo 6: Acceso, exportación, respaldo y evidencia
  - `knowledge-base/03_actores_y_roles.md` §Rutas públicas, §RBAC — Matriz de permisos
  - `knowledge-base/10_preguntas_abiertas.md` §Preguntas abiertas (identidad y plazos)

---

## FASE 5 — Presupuesto, señas y facturación

> El alcance financiero de C-16 depende de IN-01. Mercado Pago es paridad, no diferenciador (DD-05).

### [C-14] `budgets-and-cash`
- **Estado**: `[x] completado` (2026-11-23) — apply verificado: `mvn test` 241/241 en verde y `openspec validate budgets-and-cash --strict` sin errores. Change archivado el 2026-11-23 como `2026-11-23-budgets-and-cash`; specs principales sincronizados (9 requirements agregados).
- **Scope**:
  - Migración `V020` (Parte 3): `Presupuesto`, `Ítem` (tratamiento, cantidad, precio snapshot, descripción); total `NUMERIC(12,2)` en `ARS`; asociación opcional al turno (RN-FI-01).
  - CRUD indicativo `/presupuestos` en `dev`/`test` para recepción y administración; reutilización del catálogo de C-07; impresión/PDF y lectura por paciente diferidos (D13/D14).
  - Sin asiento de caja ni cobro (D10/D15): eventos `presupuesto.creado` y `presupuesto.modificado`; arqueo y conciliación en C-15.
  - Tests: cálculo de total con `HALF_UP`, ítems, superficie sin credenciales de staff, asociación al turno y auditoría.
- **Dependencias**: C-05, C-07, C-08
- **Governance**: BAJO — CRUD de catálogo/documento; autonomía si pasan los tests
- **Leer antes**:
  - `knowledge-base/04_modelo_de_datos.md` §Entidades (Presupuesto, Ítem, Seña y PagoMP)
  - `knowledge-base/05_reglas_de_negocio.md` §Dominio: Financiero (RN-FI-01)
  - `knowledge-base/06_funcionalidades.md` §US-011 — Emitir presupuesto
  - `knowledge-base/07_flujos_principales.md` §Flujo 5: Presupuesto y seña con Mercado Pago
  - `knowledge-base/02_descripcion_general.md` §API REST (si aplica)

---

### [C-15] `mercadopago-deposits-reconciliation`
- **Estado**: `[x] completado y archivado` (2026-11-23) — apply verificado: `mvn test` 287/287 en verde (241 de la línea base de C-14 + 46 nuevos) y `openspec validate mercadopago-deposits-reconciliation --strict` sin errores. Change archivado el 2026-11-23 como `2026-11-23-mercadopago-deposits-reconciliation`; specs principales sincronizados (9 requirements agregados y 2 modificados). Decisiones MP-1..MP-12 aprobadas por el PO en `docs/decisiones/C-15-decisiones.md`.
- **Scope**:
  - Migración `V021`: tabla `senias` con `turno_id` y `presupuesto_id` opcionales sin FK física, `monto NUMERIC(12,2)`, `moneda` fija `ARS`, `estado`, `proveedor`, `payment_id`, `external_reference`, `idempotency_key`, `fecha_aprobacion` y `conciliado`, con checks y unicidad. Sin tablas de caja, asiento ni arqueo; V001–V020 intactas.
  - Integración REST sobre `RestTemplate` sin SDK ni dependencias nuevas: `POST /v1/preferences`, `X-Idempotency-Key`, `external_reference = "senia-" + UUID` y reembolso total. Superficie `POST /senias` y `POST /senias/{id}/reembolsar` en `dev`/`test`, sin credenciales de staff ni RBAC nuevo.
  - Webhook público `POST /publico/v1/webhooks/mercadopago` autenticado por HMAC-SHA256 local (`ts`/`v1` y `x-request-id`), sin token de C-06; `400` sin estado, auditoría ni persistencia ante firma inválida. Deduplicación en `public_idempotencia` (`pago.webhook`) dentro de la misma transacción del cambio de estado.
  - Estados cerrados con transiciones solo desde `pendiente` y descarte de eventos tardíos; conciliación cuando `turno_id` o `presupuesto_id` resuelve a una entidad existente, con evento `pago.conciliado`, sin crear entidades ni asientos.
  - Reembolso total e inmediato según C-01 A4, con liberación del hueco por el flujo de cancelación existente, dentro y fuera de la ventana; aviso al paciente detrás de un puerto NO-OP/simulado, sin envío real ni eventos de entrega.
  - Auditoría de dinero: cinco tipos `pago.solicitado`, `pago.aprobado`, `pago.rechazado`, `pago.reembolsado` y `pago.conciliado`, con metadata acotada, `correlationId` propio y actores `SYSTEM`/`SIMULADO`. Sin secretos en respuestas, logs ni metadata (MP-10). Costos de API no se desglosan (C-01 A5); Mercado Pago es paridad competitiva (DD-05) y ARCA queda en C-16.
  - Tests: firma válida/ausente/inválida, secreto no expuesto, webhook duplicado, reversión atómica, pago rechazado, contracargo, conciliación con referencia válida e inexistente, reembolso total y rechazado, y una fila de auditoría por operación confirmada.
- **Dependencias**: C-05, C-09, C-14
- **Governance**: CRITICO — cobros y conciliación; solo análisis y aprobación humana antes de codear
- **Leer antes**:
  - `knowledge-base/05_reglas_de_negocio.md` §Dominio: Financiero (RN-FI-02, RN-FI-03), §Excepciones globales (RN-GL-02)
  - `knowledge-base/07_flujos_principales.md` §Flujo 5: Presupuesto y seña con Mercado Pago (casos de error)
  - `knowledge-base/06_funcionalidades.md` §US-012 — Cobrar seña con Mercado Pago
  - `knowledge-base/09_decisiones_y_supuestos.md` §Decisiones documentadas (DD-05)
  - `knowledge-base/10_preguntas_abiertas.md` §Inconsistencias detectadas (IN-02), §Preguntas abiertas (política de reembolso)

---

### [C-16] `arca-invoicing-reconciliation`
- **Estado**: `[x] completado y archivado` (2026-11-23) — apply verificado: `mvn test` **320/320 en verde** (287 de la línea base de C-15 + 33 nuevos de C-16) y `openspec validate arca-invoicing-reconciliation --strict` sin errores; `openspec status --change arca-invoicing-reconciliation` 4/4 artefactos completos. Change archivado el 2026-11-23 como `2026-11-23-arca-invoicing-reconciliation`; specs principales sincronizados (11 requirements agregados y 1 modificado: `arca-invoicing` 7 y `arca-reconciliation` 2, ambos en verde con `openspec validate --specs --strict`). Aprobación humana de governance CRITICO registrada en `docs/decisiones/C-16-decisiones.md` (AR-1..AR-14). Sin commits ni push.
- **Scope**:
  - Migración `V022`: tabla `comprobantes` con `senia_id`, `presupuesto_id`/`turno_id` opcionales sin FK física, `tipo`, `estado`, `moneda` fija `ARS`, `monto NUMERIC(12,2)`, `concepto`, `cae`, `fecha_emision`, `intentos`, `ultimo_error`, `conciliado` y auditoría. Unicidad por `(senia_id, tipo)` para permitir factura y nota de crédito sobre la misma seña reembolsada (AR-1, AR-11). Sin caja, asiento ni arqueo; V001–V021 intactas.
  - Emisión de una sola factura por seña `aprobado` sobre el importe cobrado (nunca el saldo del presupuesto), letra `C` y punto de venta configurables, consumidor final e IVA no discriminado. Datos del receptor reutilizados de `pacientes` sin campos fiscales nuevos; dato faltante queda `pendiente` con aviso NO-OP a recepción, sin exponer DNI ni dirección (AR-4, AR-5, AR-6).
  - Integración REST con `RestTemplate` sin SDK ni dependencia nueva: `ARCA_BASE_URL`, `ARCA_ACCESS_TOKEN`, payload y cabeceras salientes acotadas; adaptador NO-OP/simulado en `dev`/`test` con CAE determinista de prueba (`CAE-TEST-`/`NC-TEST-`) y defaults no productivos para CI (AR-7, AR-8, AR-14).
  - Estados cerrados `pendiente|emitido|rechazado|anulado` con transiciones acotadas, reintento manual y error saneado, sin reintento automático ni outbox. Idempotencia `factura.emitir`/`nota_credito.emitir` en `public_idempotencia` con hash SHA-256, en la misma transacción del cambio de estado y la auditoría, con reversión atómica verificada (AR-9).
  - Conciliación solo con CAE, montos iguales y seña `aprobado`+`conciliado`: actualiza únicamente el flag del comprobante e informa seña no conciliada, montos divergentes o comprobante no emitido (AR-10).
  - Nota de crédito manual de recepción sobre seña `reembolsado`: revierte el total, anula la factura y usa su propia clave de idempotencia; sin disparo automático al finalizar el reembolso (AR-11).
  - Superficie `POST /comprobantes`, `POST /comprobantes/{id}/reintentar`, `POST /comprobantes/{id}/nota-credito` y `GET /comprobantes/{id}` bajo `@Profile({"dev","test"})`, sin credenciales de staff ni RBAC nuevo. Liquidación contable y cierre impositivo quedan diferidos (AR-3).
  - Auditoría con los cinco tipos aprobados `factura.emitida`, `factura.rechazada`, `factura.conciliada`, `factura.anulada` y `nota_credito.emitida`, metadata cerrada, `correlationId` propio y actores `SIMULADO`/`SYSTEM`; sin CAE, DNI ni secretos en logs o metadata (AR-12, AR-14).
  - Tests: migración y constraints V022, persistencia y unicidad, emisión con/sin presupuesto, seña no aprobada, receptor faltante, rechazo/reintento/error saneado, idempotencia y rollback, cuatro escenarios de conciliación, nota de crédito y reenvío, auditoría y metadata, MVC y wiring `dev`/`test`.
- **Dependencias**: C-01, C-15
- **Governance**: CRITICO — facturación; análisis y aprobación humana explícita antes de cualquier código
- **Leer antes**:
  - `knowledge-base/10_preguntas_abiertas.md` §Inconsistencias detectadas (IN-01), §Preguntas abiertas (prioridad Alta)
  - `knowledge-base/09_decisiones_y_supuestos.md` §Decisiones documentadas (DD-09)
  - `knowledge-base/05_reglas_de_negocio.md` §Dominio: Financiero (RN-FI-04)
  - `knowledge-base/02_descripcion_general.md` §Integraciones externas (ARCA)
  - `knowledge-base/01_vision_y_objetivos.md` §Condicionado a decisión pendiente

---

## FASE 6 — Cumplimiento demostrable

> La declaración de Ley 25.326 ya no diferencia: hace falta evidencia descargable (RN-SE-04).

### [C-17] `export-backup-restore`
<<<<<<< HEAD
- **Estado**: `[x]` completado
=======
- **Estado**: `[ ]` pendiente — implementado en `feat/Parte4-J-Velazquez` (V030, 12 tests); governance ALTO: **espera revisión humana** antes de archivar. Ver [`docs/decisiones/Parte4-C17-C18-C21-C22-checkpoints.md`](docs/decisiones/Parte4-C17-C18-C21-C22-checkpoints.md)
>>>>>>> origin/feat/Parte4-J-Velazquez
- **Scope**:
  - Migración 012: registro de exportaciones, respaldos y restauraciones (formato, plazos y destino `(TBD)`).
  - Exportación completa de datos y copia de historia en 48 h `(TBD, IN-05/IN-06)`; formatos `(TBD)`; cifrado en reposo y ubicación de datos/subprocesadores `(TBD, RN-SE-05)`.
  - Respaldo programado con frecuencia `(TBD)` y retención de 10 años `(TBD)`; `BACKUP_CRON` `(TBD)`; procedimiento de **restore** probado en entorno aislado antes de escribir sobre datos de usuario; alerta ante fallo `(TBD)`.
  - Registro append-only de cada exportación/respaldo/restore vía C-05.
  - Tests: exportación completa, restore en entorno aislado, fallo de respaldo, trazabilidad en auditoría.
- **Dependencias**: C-05, C-13, C-15
- **Governance**: ALTO — backup/restore y configuración que afecta datos de usuario; proponer y esperar revisión antes de escribir
- **Leer antes**:
  - `knowledge-base/05_reglas_de_negocio.md` §Dominio: Seguridad y cumplimiento (RN-SE-03, RN-SE-04, RN-SE-05)
  - `knowledge-base/06_funcionalidades.md` §US-014 — Auditar, exportar y respaldar
  - `knowledge-base/07_flujos_principales.md` §Flujo 6: Acceso, exportación, respaldo y evidencia
  - `knowledge-base/04_modelo_de_datos.md` §Entidades (AuditoríaLog y Evidencia)
  - `knowledge-base/10_preguntas_abiertas.md` §Inconsistencias detectadas (IN-05, IN-06), §Preguntas abiertas (despliegue y infra)

---

### [C-18] `compliance-evidence-dossier`
- **Estado**: `[ ]` pendiente — implementado en `feat/Parte4-J-Velazquez` (V031, 5 tests); governance ALTO: **espera revisión humana** y designación del owner legal (C-01 D)
- **Scope**:
  - Dossier descargable con DPA responsable/encargado, registro **AAIP/RNBD** del responsable, cláusulas **AAIP 198/2023** para subprocessores, roles, logs, exportación y retención; alcance exacto `(TBD, IN-05)`.
  - Responsable de datos y owner legal `(TBD)`; aplicabilidad confirmada de Ley 25.326, Ley 26.529, Decreto 1089/2012 y Ley 25.506 `(TBD)`; procedimiento INTIMA/CONSTA `(TBD)`.
  - Bloquear inclusión en el dossier de subprocessores sin cláusulas publicadas `(TBD)`; cada descarga queda registrada en auditoría.
  - Tests: dossier completo, evidencia faltante bloquea publicación, trazabilidad de roles y descargas.
- **Dependencias**: C-01, C-12, C-17
- **Governance**: ALTO — evidencia legal y configuración de datos de usuario; proponer y esperar revisión
- **Leer antes**:
  - `knowledge-base/05_reglas_de_negocio.md` §Dominio: Seguridad y cumplimiento (RN-SE-04, RN-SE-06)
  - `knowledge-base/08_arquitectura_propuesta.md` §Seguridad (Cumplimiento demostrable)
  - `knowledge-base/10_preguntas_abiertas.md` §Inconsistencias detectadas (IN-05, IN-06), §Preguntas abiertas (responsable de datos)
  - `knowledge-base/01_vision_y_objetivos.md` §Métricas de éxito (Evidencia de cumplimiento)
  - `knowledge-base/09_decisiones_y_supuestos.md` §Decisiones documentadas (DD-06)

---

## FASE 7 — Diferenciadores condicionados y medición

> Todos los items de esta fase dependen de decisiones `(TBD)` de C-01. Si no se habilitan, se cierran como diferidos sin código.

### [C-19] `waitlist-auto-refill-metrics`
- **Estado**: [x] completado — implementado y verificado el 2026-11-23; 354 tests verdes (320 base + 34 nuevos de C-19), `openspec validate waitlist-auto-refill-metrics --strict` válido, 4/4 artefactos; migración `V023__waitlist_and_metrics.sql` aplicada sin tocar V001–V022; checkpoints W-11 documentados en `docs/decisiones/C-19-decisiones.md`; sin commits
- **Scope**:
  - Migración 013 (si se habilita): `ListaDeEspera` (preferencias, `deposito_requerido`, estado) y `Métrica` (`sillon_id`, `periodo`, `ocupacion`, `ausentismo`, `metodologia` obligatoria).
  - Relleno automático tras cancelación/liberación, con ofrecimiento por WhatsApp (C-10), depósito configurable y adjudicación atómica; pasar al siguiente candidato si no paga en plazo `(TBD)`; doble adjudicación impedida (RN-AG-07, US-015).
  - Métricas **verificables** de ocupación y ausentismo por sillón, con metodología y período explícitos; objetivo numérico `(TBD)`; conciliación del depósito contra C-15.
  - Inclusión en v0.1 `(TBD)`: es diferenciador real, **no** confundir con la seña de Mercado Pago (paridad).
  - Tests: adjudicación concurrente, depósito, expiración de oferta, metodología obligatoria.
- **Dependencias**: C-01, C-09, C-10, C-15
- **Governance**: MEDIO — pipeline y lógica de negocio; checkpoints
- **Leer antes**:
  - `knowledge-base/06_funcionalidades.md` §US-015 — Motor de ocupación `(TBD)`, §Épica 7
  - `knowledge-base/07_flujos_principales.md` §Flujo 7: Relleno automático de waitlist con depósito `(TBD)`
  - `knowledge-base/05_reglas_de_negocio.md` §Dominio: Agenda (RN-AG-07), §Dominio: Financiero (RN-FI-03)
  - `knowledge-base/04_modelo_de_datos.md` §Entidades (ListaDeEspera y Métricas `(TBD)`)
  - `knowledge-base/01_vision_y_objetivos.md` §Métricas de éxito, §Condicionado a decisión pendiente

---

### [C-20] `ia-conversacional-handoff`
- **Estado**: `[x]` cerrado como **diferido sin código** por C-01 A3 (`IA_HANDOFF_ENABLED=false`), según la regla del propio scope; guardia `IaHandoffDiferidoTest`; verificado 2026-11-22. Ver [`docs/decisiones/C-20-ia-conversacional-handoff-diferido.md`](docs/decisiones/C-20-ia-conversacional-handoff-diferido.md).
- **Scope**:
  - Si DD-11 habilita IA: adaptador conversacional sobre WhatsApp (proveedor/API `(TBD)`) con reglas explícitas de derivación humana ante dolor, sangrado o postoperatorio (RN-CL-06).
  - Cada handoff queda registrado en la historia clínica (C-11/C-12) con override humano auditable (C-05); sin diagnósticos automatizados ni sustitución del criterio clínico.
  - Evidencia de precisión y métricas de handoff `(TBD)`; bandera `IA_HANDOFF_ENABLED` gobernada por C-01.
  - Inclusión en MVP `(TBD)`; si no se habilita, cerrar ficha como diferida sin código.
  - Tests: detección de urgencia, handoff registrado, override humano, sin respuesta diagnóstica automática.
- **Dependencias**: C-01, C-10, C-12
- **Governance**: ALTO — configuración que afecta datos clínicos de usuario; proponer y esperar revisión
- **Leer antes**:
  - `knowledge-base/05_reglas_de_negocio.md` §Dominio: Clínico (RN-CL-06)
  - `knowledge-base/06_funcionalidades.md` §US-007 — Derivación humana `(TBD)`
  - `knowledge-base/07_flujos_principales.md` §Flujo 3: Confirmación y recordatorio por WhatsApp (paso 4)
  - `knowledge-base/09_decisiones_y_supuestos.md` §Decisiones documentadas (DD-11)
  - `knowledge-base/10_preguntas_abiertas.md` §Preguntas abiertas (IA conversacional en MVP)

---

### [C-21] `precio-predecible-costos`
- **Estado**: `[ ]` pendiente — implementado y verificado (4 tests) con C-01 A5=2; monto/moneda/periodicidad `(TBD)` hasta aprobación del PO
- **Scope**:
  - Superficie de consulta del precio único, público y predecible, con los costos de API detallados por separado (Meta, Mercado Pago y ARCA si aplica); soporte de la superficie `(TBD — visual-design diferido)`.
  - Modelo comercial exacto `(TBD, US-016/RN-SE-06)`; sin packs sorpresa dentro del plan (RN-NT-04, confirmar modelo `(TBD)`); reutiliza la configuración de C-10 y C-15 sin exponer secretos.
  - Tests: consistencia entre precio publicado y detalle de costos, ausencia de credenciales en la superficie.
- **Dependencias**: C-01, C-10, C-15
- **Governance**: BAJO — consulta de solo lectura y catálogo; autonomía si pasan los tests
- **Leer antes**:
  - `knowledge-base/05_reglas_de_negocio.md` §Dominio: Notificaciones (RN-NT-04), §Dominio: Seguridad y cumplimiento (RN-SE-06)
  - `knowledge-base/06_funcionalidades.md` §US-016 — Precio único y predecible `(TBD)`
  - `knowledge-base/09_decisiones_y_supuestos.md` §Decisiones documentadas (DD-06)
  - `knowledge-base/01_vision_y_objetivos.md` §Métricas de éxito
  - `knowledge-base/10_preguntas_abiertas.md` §Preguntas abiertas (modelo comercial)

---

### [C-22] `adoption-metrics-dashboard`
- **Estado**: `[ ]` pendiente — implementado y verificado (9 tests) por contrato con C-09 (`token_publico`, `inicioAnterior`); se marca `[x]` cuando C-09 emita esos datos
- **Scope**:
  - Instrumentación y consulta de la métrica principal de riesgo: % de turnos creados por paciente vía link/QR sobre el total (adopción online), con metodología y período explícitos; objetivo `(TBD, SU-03)`.
  - Consulta del cumplimiento de la ventana de 48 hs hábiles y de confirmaciones; reutiliza C-19 si se habilita `(TBD)`; presentación `(TBD — visual-design diferido)`.
  - Cada consulta sensible queda registrada en auditoría; sin cifras decorativas sin metodología (DD-06).
  - Tests: conteo por `origen`, periodo, metodología obligatoria, auditoría de consulta.
- **Dependencias**: C-05, C-09, C-18
- **Governance**: BAJO — utilidad de solo lectura; autonomía si pasan los tests
- **Leer antes**:
  - `knowledge-base/01_vision_y_objetivos.md` §Métricas de éxito
  - `knowledge-base/09_decisiones_y_supuestos.md` §Supuestos inferidos (SU-03), §Decisiones documentadas (DD-06)
  - `knowledge-base/06_funcionalidades.md` §US-004 — Confirmar asistencia, §US-015 — Motor de ocupación
  - `knowledge-base/04_modelo_de_datos.md` §Dominios (Ocupación `(TBD)`), §Entidades (ListaDeEspera y Métricas `(TBD)`)
  - `knowledge-base/10_preguntas_abiertas.md` §Preguntas abiertas (objetivos numéricos de adopción)

---

### Tabla resumen y primer item

| ID | Change | Fase | Governance | Dependencias | `(TBD)` que lo afectan |
|---|---|---|---|---|---|
| C-01 | decisiones-habilitantes | 0 | CRITICO | — | IN-01, DD-01, DD-10, DD-11, IN-04/05/06, reembolso, zona horaria, anamnesis, precio |
| C-02 | foundation-scaffolding | 1 | BAJO | C-01 | stack, secretos, CI |
| C-03 | core-models-and-migrations | 1 | CRITICO | C-02 | consultorio único/multisede, tipo de documento, roles |
| C-04 | auth-rbac | 1 | CRITICO | C-03 | sesión/token, rate limit, herencia de permisos |
| C-05 | audit-append-only-log | 1 | CRITICO | C-03 | hash, retención, catálogo de eventos |
| C-06 | public-access-tokens | 1 | CRITICO | C-04 | expiración, verificación de identidad |
| C-07 | scheduling-catalogs | 2 | BAJO | C-03 | duraciones, feriados, zona horaria, anticipación |
| C-08 | agenda-core-and-overlap-prevention | 2 | MEDIO | C-04, C-05, C-07 | mecanismo anti-solapamiento, estados, sobreturno (IN-04) |
| C-09 | public-booking-and-reschedule | 3 | MEDIO | C-06, C-08 | alternativas, token vencido, cómputo 48 hs |
| C-10 | whatsapp-notifications-outbox | 3 | ALTO | C-05, C-08 | plantillas, outbox, firma de webhook, costos |
| C-11 | clinical-record-odontogram | 4 | MEDIO | C-05, C-08 | anamnesis, cara FDI, borrado |
| C-12 | clinical-signature-consent | 4 | ALTO | C-05, C-11 | proveedor de firma, contingencia |
| C-13 | patient-clinical-record-access | 4 | ALTO | C-06, C-12 | identidad, plazo 48 h, alcance |
| C-14 | budgets-and-cash | 5 | BAJO | C-05, C-07, C-08 | turno asociado, PDF, moneda |
| C-15 | mercadopago-deposits-reconciliation | 5 | CRITICO | C-05, C-09, C-14 | firma webhook, conciliación, reembolso (IN-02) |
| C-16 | arca-invoicing-reconciliation | 5 | CRITICO | C-01, C-15 | IN-01 (MVP vs posterior) |
| C-17 | export-backup-restore | 6 | ALTO | C-05, C-13, C-15 | formato, plazos, frecuencia, retención, destino |
| C-18 | compliance-evidence-dossier | 6 | ALTO | C-01, C-12, C-17 | owner legal, alcance del dossier, INTIMA/CONSTA |
| C-19 | waitlist-auto-refill-metrics | 7 | MEDIO | C-01, C-09, C-10, C-15 | inclusión en v0.1, plazos, objetivos |
| C-20 | ia-conversacional-handoff | 7 | ALTO | C-01, C-10, C-12 | DD-11, proveedor, evidencia |
| C-21 | precio-predecible-costos | 7 | BAJO | C-01, C-10, C-15 | modelo comercial (RN-SE-06) |
| C-22 | adoption-metrics-dashboard | 7 | BAJO | C-05, C-09, C-18 | objetivo de adopción |

**Primer item elegible**: `C-01` (`decisiones-habilitantes`). Antes de arrancar, leer `knowledge-base/10_preguntas_abiertas.md` §Inconsistencias detectadas y `knowledge-base/09_decisiones_y_supuestos.md` §Decisiones documentadas; acordar el alcance de la aprobación humana y dejar constancia de cada `(TBD)` resuelto o todavía pendiente.
