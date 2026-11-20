# C-03 — Decisiones aprobadas: modelos core y migraciones

- **Change**: `C-03 core-models-and-migrations`
- **Governance**: CRÍTICO
- **Estado**: decisiones aprobadas por el PO; esquema habilitado para implementación (aún **no** aplicado; `CHANGES.md` sigue en `[ ]` hasta verificación técnica)
- **Fecha**: 2026-11-20
- **Aprobación**: mensaje del PO (2026-11-20): `D1=a, D2=a, D3=a, D4=a, D5=a, D6=a, D7=a, D8=a, D9=a, D10=b, D11=a`
- **Análisis de origen**: observation #17 de Engram, topic_key `opsx/c-03/analysis` (inventario `D1..D11`, 9 conflictos, propuesta descriptiva de la Migración 001).
- **Fuentes consultadas**: `knowledge-base/03_actores_y_roles.md`, `04_modelo_de_datos.md`, `05_reglas_de_negocio.md`, `08_arquitectura_propuesta.md`, `09_decisiones_y_supuestos.md` (SU-01/SU-02), `10_preguntas_abiertas.md` (IN-01..IN-06); `docs/decisiones/C-01-decisiones-habilitantes.md`, `docs/decisiones/C-02-resoluciones-stack.md`; `CHANGES.md` §C-03 (líneas 204-218); `pom.xml`; `AGENTS.md`.

## Nota sobre D10
El ejemplo de "aceptar todas las recomendaciones" enviado al PO listaba `D10=a` por error de transcripción. La **recomendación real** del análisis era `D10=b` (índices parciales `WHERE activo = true`). El PO confirmó `D10=b`; por lo tanto **no existe desviación** respecto del análisis y se registra `b` como decisión aprobada.

## Tabla de decisiones aprobadas

| ID | Tema | Opción aprobada | Decisor | Impacto aguas abajo |
|---|---|---|---|---|
| D1 | Motor de BD | **a** — PostgreSQL 17 (fallback 16) en cloud región AR | PO + equipo técnico | C-08 (RN-AG-01), C-11 (JSONB), C-17 (backups), C-18 (DPA) |
| D2 | Documento y campos de `Paciente` | **a** — `documento_tipo` + `documento_numero VARCHAR(32)`, unique compuesto, `telefono` obligatorio, opcionales nullable, HMAC *blind index* si hay cifrado columnar | PO + odontólogo/responsable legal | C-06, C-08, C-11/C-13, C-17 |
| D3 | `Consultorio` único vs multisede | **a** — consultorio único, una fila seed, sin `sede_id`, sin test de aislamiento por sede | PO (confirmación de cierre) | C-04, C-07, C-08, C-19 |
| D4 | Nombres físicos | **a** — `snake_case` plural, mapeo explícito `@Table/@Column`, IDs `BIGINT` secuencia | Equipo técnico con aprobación PO | Todas las migraciones, C-08, C-11, C-17 |
| D5 | Auditoría / soft delete | **a** — `created_at/created_by/updated_at/updated_by/activo`, sin borrado físico en v0.1; log append-only en C-05 | PO + responsable legal | C-05, C-11, C-17, C-18 |
| D6 | Repository / UoW | **a** — Spring Data JPA por agregado + `@Transactional` en servicios; UoW = `EntityManager`; sin repositorio genérico custom | Equipo técnico con aprobación PO | C-04, C-08, C-11, C-17 |
| D7 | Roles seed | **a** — `paciente`, `recepcionista`, `odontólogo`, `administrador` + `auditor` lector; `dirección` diferido con C-19 | PO | C-04, C-05, C-17/C-18, C-19 |
| D8 | Permisos efectivos y herencia | **a** — `permiso` + `rol_permiso` planos y explícitos, ámbito propio/todos/config, sin herencia implícita | PO | C-04, C-11, C-17/C-18 |
| D9 | Ley 25.326 | **a** — consentimiento con `fecha_consentimiento`, `version_politica`, `finalidad`/`canal`; cifrado en reposo; retención 10 años sin borrado físico | PO + responsable legal | C-05, C-13, C-17, C-18 |
| D10 | Índices y unicidad | **b** — índices parciales `WHERE activo = true` para `profesional.activo` y `sillon.activo`, unique coherente con `activo`; RN-AG-01 se implementa en C-08 (Migración 006), no en 001 | PO + equipo técnico | C-08, C-09, C-17 |
| D11 | `Usuario` vs `Paciente` | **a** — separados; `Usuario` = staff con `rol_id`, `Paciente` = datos personales, acceso paciente vía token de C-06; rol `paciente` sembrado sin cuenta | PO + equipo técnico | C-04, C-06, C-13, C-17 |

## Detalle y fundamentos

### D1 — Motor de base de datos
- **Decisión**: PostgreSQL 17; si la región AR del proveedor cloud no ofrece 17, usar 16. Driver, URL y credenciales se definen en el apply, nunca en el repositorio.
- **Fundamento**: C-02 §Resoluciones de entorno verificado dejó el motor `(TBD)`; C-01 B2 exige BD relacional con restricción/serialización transaccional; B6 fija cloud región AR con DPA y cláusulas AAIP 198/2023; RN-AG-01 exige enforcement en capa de datos. PostgreSQL permite `FOR UPDATE`/`SERIALIZABLE` y exclusion constraints con `btree_gist` + `tstzrange` para C-08, además de JSONB para la anamnesis de C-11 (decisión C4).
- **Descartadas**: H2 (no viable en producción por concurrencia y respaldos); MySQL/MariaDB (sin exclusion constraints nativas → RN-AG-01 dependería más de código).

### D2 — `documento_identidad` y campos opcionales de `Paciente`
- **Decisión**: `documento_tipo` (DNI/CUIT/pasaporte) + `documento_numero VARCHAR(32)` con unique compuesto; `telefono` obligatorio (WhatsApp es obligatorio en el MVP, decisión A3); `fecha_nacimiento`, `email` y `direccion` nullable. Si se requiere cifrado columnar, usar *blind index* HMAC para conservar la búsqueda por índice.
- **Fundamento**: `knowledge-base/04_modelo_de_datos.md` §Paciente marca `(TBD tipo)` y "identificador de contacto único `(TBD)`"; `10_preguntas_abiertas.md` §Resumen lista tipos de documento y campos opcionales.

### D3 — `Consultorio` único
- **Decisión**: existe la tabla `consultorio` con **una** fila seed; no se agrega `sede_id` a `Profesional`/`Sillón`; se elimina el test de "aislamiento por sede" del alcance de C-03.
- **Fundamento**: C-01 A2 y B3 (consultorio único, DD-10/SU-01) ya cerraron la decisión. Los textos "único vs multisede `(TBD)`" en `CHANGES.md:207` y "aislamiento por sede si aplica `(TBD)`" en `:211` son residuales y se consideran **no aplica**.

### D4 — Nombres físicos
- **Decisión**: tablas en `snake_case` plural (`pacientes`, `profesionales`, `sillones`, `consultorios`, `usuarios`, `roles`, `permisos`, `rol_permiso`); columnas `snake_case`; mapeo explícito con `@Table`/`@Column`; IDs `BIGINT` con secuencia.
- **Fundamento**: `knowledge-base/04_modelo_de_datos.md` cabecera define el modelo como conceptual con nombres físicos `(TBD)`; C-02 fijó el paquete base `com.turnosodontologia`. El mapeo explícito evita que el esquema cambie con versiones de Hibernate.

### D5 — Auditoría y soft delete
- **Decisión**: mixin con `created_at`, `created_by`, `updated_at`, `updated_by`, `activo`. **Sin borrado físico ni `deleted_at` en v0.1**. El log append-only de RN-SE-02 se implementa en C-05, no en C-03.
- **Fundamento**: `CHANGES.md:208`; `08_arquitectura_propuesta.md` §Patrones (borrado físico `(TBD)`, no sobrescribir evoluciones RN-CL-02); retención de 10 años (RN-SE-03/04/05).

### D6 — Repository / Unidad de Trabajo
- **Decisión**: Spring Data JPA por agregado; transacciones con `@Transactional` en servicios de aplicación; UoW = `EntityManager`/transacción de Spring. **No** se construye un repositorio genérico custom. El bloqueo de RN-AG-01 se invoca desde el caso de uso en C-08.
- **Nota de implementación**: Lombok está declarado en `pom.xml` pero no descargado; no se aplica hasta que las dependencias estén disponibles. Se mantienen `record`s/enums Java 17 si el harness debe compilar solo con JDK.

### D7 — Roles seed
- **Decisión**: sembrar `paciente`, `recepcionista`, `odontólogo`, `administrador` y `auditor` (lectura de auditoría). `dirección` queda **diferido** junto con C-19/métricas (v0.2). El rol `paciente` se siembra sin crear cuenta de usuario (ver D11).
- **Fundamento**: `04_modelo_de_datos.md` §Seed data inicial exige los 4 primeros; `03_actores_y_roles.md` marca `auditor` y `dirección` como `(TBD)`; C-05/C-18 requieren lectura de auditoría.

### D8 — Permisos efectivos y herencia
- **Decisión**: tablas `permiso` (`recurso`, `accion`, `ambito`) y `rol_permiso` con filas explícitas derivadas de la matriz RBAC de `03_actores_y_roles.md`. Ámbitos `propio`/`todos`/`config`. **Sin herencia implícita** entre roles. C-04 solo consume la matriz.
- **Fundamento**: `03_actores_y_roles.md` §RBAC y `04_modelo_de_datos.md` §Usuario/Rol/Permiso marcan permisos efectivos y herencia como `(TBD)`; RN-SE-01.

### D9 — Ley 25.326
- **Decisión**: `Paciente` lleva `consentimiento_datos`, `fecha_consentimiento`, `version_politica`, `finalidad` y `canal`; cifrado en reposo de la BD; retención 10 años parametrizada por `EXPORT_RETENTION_YEARS`, sin borrado automático. Evento de auditoría en C-05 y owner legal en C-18.
- **Fundamento**: `CHANGES.md:210`; RN-SE-04 ("no basta con declararlo"), RN-SE-05 (cifrado en tránsito y reposo); decisión C-01 D (marco normativo ampliado).

### D10 — Índices y unicidad
- **Decisión**: índices parciales `WHERE activo = true` sobre `profesional.activo` y `sillon.activo`; índice sobre `telefono`; unique `(documento_tipo, documento_numero)` coherente con `activo` (D2). `timestamptz` para toda fecha/hora. **RN-AG-01 no se implementa en la Migración 001**: su constraint de exclusion va en C-08 (Migración 006); 001 solo garantiza tipos y FKs que lo permitan.
- **Fundamento**: `CHANGES.md:209`; `04_modelo_de_datos.md` §Turno (RN-AG-01 enforcement en BD `(TBD)`); PostgreSQL (D1) soporta índices parciales y exclusion constraints.

### D11 — `Usuario` vs `Paciente`
- **Decisión**: tablas separadas. `Usuario` = personal (staff) con `rol_id`, `email`, `hash_password`/proveedor externo, `activo`, `ultimo_acceso`. `Paciente` = datos personales. El acceso del paciente se resuelve con token de enlace/QR en C-06. El rol `paciente` se siembra sin filas en `usuario`.
- **Fundamento**: `04_modelo_de_datos.md` §Usuario/Rol/Permiso; `08_arquitectura_propuesta.md` §Seguridad (token con vencimiento `(TBD)`, verificación de identidad `(TBD)`).

## Conflictos resueltos

1. **Motor BD heredado** → cerrado en D1 (PostgreSQL 17/16).
2. **Consultorio único vs texto residual multisede** → D3 cierra como "no aplica"; queda pendiente la limpieza textual en `CHANGES.md:207/211` y en C-08 (`IN-04`).
3. **Roles `auditor`/`dirección` no confirmados** → D7 crea `auditor`, difiere `dirección` con C-19.
4. **Cifrado columnar vs índices** → D2/D9 exigen HMAC *blind index* si se cifra por columna.
5. **Soft delete vs retención/append-only** → D5 prohíbe borrado físico en v0.1.
6. **Lombok/JUnit declarados pero no descargados** → D6 los deja diferidos; el harness de C-02 sigue siendo la base ejecutable hasta que se autorice descargar dependencias.
7. **"Identificador de contacto único `(TBD)`" ambiguo** → D2 lo define como el par `(documento_tipo, documento_numero)`, con `telefono` obligatorio para WhatsApp.
8. **IN-04 residual en `CHANGES.md:301` (C-08)** → no bloquea 001; debe cerrarse textualmente en C-08.
9. **Governance CRÍTICO** → este documento registra la aprobación humana previa requerida; el apply aún no se ejecutó y `CHANGES.md` no se marcó.

## Queda fuera de la Migración 001
- `Turno`, `Bloqueo` y el constraint anti-solapamiento (C-08 / Migración 006).
- Sesiones y tokens de staff (C-04) y token público de paciente (C-06).
- Log de auditoría append-only (C-05).
- Catálogos de tratamientos/duraciones (C-07), historia clínica/odontograma (C-11), exportaciones y respaldos (C-17).

## Próxima acción
Habilitado el apply de C-03 con Strict TDD. Requiere resolver antes: si se autoriza descargar dependencias Maven (JPA/Flyway/JUnit) para compilar entidades con anotaciones y ejecutar tests reales, o si se mantiene el harness sin dependencias de C-02. No se crea ningún change OpenSpec; `CHANGES.md` se marca recién tras verificación técnica.
