# Design

## Context

Ver `proposal.md` para la motivación. El estado actual que condiciona el diseño es:

- `src/` no contiene ninguna implementación financiera: solo `FinancieroReference.java`, un `record` sin consumidores. No hay entidades `Presupuesto`, `Ítem`, `Caja` ni `Seña`, ni tipos `BigDecimal`/moneda.
- V001–V006 son las únicas migraciones. `tratamientos` (V005) tiene `codigo`, `nombre` y `duracion_minutos`, sin precio y sin seeds; `turnos` (V006) no tiene `presupuesto_id`. El máximo aplicado es V006.
- `AuditoriaCatalogo` tiene una lista blanca cerrada de 7 tipos y rechaza cualquier tipo nuevo; C-05 exige proponer por escrito cada evento adicional antes de implementarlo.
- Las decisiones D1–D15 están en `docs/decisiones/C-14-decisiones.md`. RN-GL-03 exige que los valores semánticos vengan de una decisión humana, no de convención del agente.
- El governance local es BAJO y no hay aplicación ejecutable en `src/main`; la verificación se hará con contexto Spring de test y PostgreSQL/Testcontainers.

## Goals / Non-Goals

**Goals:**

- Persistir presupuestos con ítems a precio snapshot y total determinista en ARS, con trazabilidad en `auditoria_log`.
- Proveer una superficie HTTP indicativa en `dev`/`test` que no debilite las guardias de C-04/C-06.
- Dejar un contrato estable para que C-15 y C-16 puedan consumir el presupuesto sin reabrir este change.

**Non-Goals:**

- UI o mockups: **Decision: no (no UI)**; el cambio es de datos y superficie HTTP, por lo que no dispara la política `design-mockups`.
- Asiento de caja, arqueo, cobro, Mercado Pago, webhooks, reembolso y waitlist (C-15/C-19).
- Factura ARCA (C-16), notificaciones (C-10), lectura por paciente y PDF (diferidos por D13/D14).
- Permisos financieros nuevos, protección RBAC de agenda y adaptador de actor real (deuda de C-05).
- Código en `src/`, commits o push durante el propose.

## Decisions

Las decisiones D1–D15 se aplican tal como fueron aprobadas por el PO. Para cada una se registra la alternativa descartada.

| # | Decisión aplicada | Alternativa descartada y motivo |
|---|---|---|
| D1 | C-08 se trata como habilitador técnico cumplido; su deuda de actor/RBAC queda para otro change | Esperar a marcar C-08 `[x]`: bloquearía un CRUD que no depende de esa deuda |
| D2 | Governance BAJO con verificación técnica completa | Elevar a MEDIO: las decisiones sensibles ya están cerradas en el acta |
| D3 | Migración nueva `V020__budgets_and_cash.sql`; V021+ reservado para C-15 y siblings | Reutilizar el “009” histórico o editar V001–V006: contradice la tabla vigente de `CHANGES.md` |
| D4 | `presupuesto.creado` y `presupuesto.modificado`; actor `recepcionista`/`SIMULADO`; metadata acotada | Inventar eventos durante el apply: prohibido por C-05 |
| D5 | `ARS`, `NUMERIC(12,2)`, `HALF_UP`, impuestos incluidos | Tabla de monedas o representación en centavos: speculativa para consultorio único (C-01 A2/B3) |
| D6 | Precio snapshot en el ítem con FK a `tratamientos` | Agregar `precio` a C-07: cambiaría su semántica de lectura y sus tests, prohibido por el acta de C-07 |
| D7 | `ROUND(SUM(cantidad * precio), 2)` al final; sin descuentos ni recargos | Reglas comerciales adicionales: no acordadas (RN-GL-03) |
| D8 | `emitido`/`anulado` como cadenas; anulación con `activo=false` | Enum SQL o estado `borrador`: V006 no usa enums y no hay workflow de aprobación |
| D9 | `turno_id` nullable, varios presupuestos por turno, sin write-back | Vincular obligatoriamente al turno: acoplaría C-14 al ciclo de C-09 sin decisión sobre cancelación |
| D10 | Diferir caja y arqueo a C-15 | Crear `Caja`/`Asiento` aquí: la KB no define su semántica y un presupuesto no es dinero movido |
| D11 | Contrato estable `id`, `total`, `moneda`, `estado`, `turno_id`, ítems | Duplicar campos de pago o factura en C-14: adelantaría decisiones CRITICO de C-15/C-16 |
| D12 | `/presupuestos` en `dev`/`test`, sin RBAC; actor `recepcionista` simulado | Exigir `administrador` o permisos nuevos: convertiría un CRUD BAJO en cambio de seguridad y el factory no existe |
| D13 | Lectura por paciente diferida | Crear scope financiero en C-06/C-09: ampliaría la superficie pública y el riesgo de exposición |
| D14 | PDF diferido | Agregar librería de renderizado: no hay plantilla ni formato aprobado |
| D15 | Sin cobro, MP, ARCA, waitlist, reembolso, notificaciones ni UI | Adelantar esas capacidades: rompería el sequenceo del roadmap y las reglas de DD-05/DD-06 |

## Risks / Trade-offs

- **[Catálogo sin precio] →** el precio se captura por recepción como snapshot; queda documentado que C-07 no es fuente de precios y que no hay seeds.
- **[Cálculo con `BigDecimal` y redondeo] →** una sola regla `HALF_UP` al final, cubierta por tests de límites; se prohíbe `double` para importes.
- **[Sin RBAC real en `/presupuestos`] →** perfil `dev`/`test` y actor simulado explícito; C-04 no se modifica y la deuda queda registrada.
- **[Tipos de auditoría cerrados] →** ampliación propuesta en esta acta y test que rechaza tipos inventados, según C-05.
- **[Metadata con total y moneda] →** solo claves permitidas y sin datos personales ni secretos; se reutiliza el validador de metadata existente.
- **[FK opcional a `turnos`] →** borrado lógico del presupuesto y sin write-back; se documenta que cancelar/reprogramar un turno no altera el presupuesto.
- **[Sin caja ni cobro] →** el contrato para C-15/C-16 se limita a identificador, total, moneda, estado y turno; se explicita en la spec para evitar interpretaciones de “caja” en el nombre del change.
- **[Sin aplicación ejecutable] →** verificación con contexto de test y `PostgresFixture`; arranque real queda para C-17/C-18.

## Migration Plan

Se crea **`V020__budgets_and_cash.sql`** en `src/main/resources/db/migration/` durante el apply, con:

- `presupuestos`: `id BIGINT` PK, `paciente_id BIGINT` FK, `turno_id BIGINT` nullable FK a `turnos`, `estado VARCHAR(20)` con check `emitido|anulado`, `moneda VARCHAR(3)` fija `ARS`, `total NUMERIC(12,2) NOT NULL`, columnas de auditoría de `AuditableEntity` e `activo BOOLEAN`.
- `items_presupuesto`: `id BIGINT` PK, `presupuesto_id BIGINT` FK, `tratamiento_id BIGINT` FK a `tratamientos`, `cantidad INTEGER` con check `> 0`, `precio NUMERIC(12,2)` con check `>= 0`, `descripcion VARCHAR(255)` y columnas de auditoría.
- Índices por `paciente_id`, `turno_id` y `presupuesto_id`; sin seeds, sin enum SQL y sin columnas de pago, impuesto ni caja.

No se edita V001–V006, no se ejecuta `flyway repair` y no se crea ninguna migración por debajo de V020. Rollback: revertir código y eliminar V020 en un entorno sin datos; en entornos con datos, el borrado físico de las tablas requiere decisión humana porque C-14 usa borrado lógico y auditoría.

## Verification Strategy

- Tests unitarios de cálculo, redondeo y validación de ítems sin base de datos.
- Test de migración con `PostgresFlywayTest` que verifique columnas, constraints, FKs y ausencia de columnas financieras de cobro.
- Test de persistencia de borrado lógico y de multitud de presupuestos por turno.
- Test Web MVC de `/presupuestos` sin credenciales y de que las superficies existentes no cambian.
- Test de catálogo y metadata de auditoría, con publicador real contra `auditoria_log`.
- Suite completa con `mvn test`, partiendo de la línea base de 219 tests verdes, y `openspec validate budgets-and-cash --strict`.
