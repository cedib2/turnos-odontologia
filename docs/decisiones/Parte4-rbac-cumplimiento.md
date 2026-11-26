# Parte 4 — Control de acceso de cumplimiento y métricas

- **Fecha**: 2026-11-25
- **Decisor**: José Donato Fornasari Donadel, en la integración de `demo-integracion`
- **Governance**: CRITICO (autenticación y RBAC de C-04): decisión humana tomada antes de codear
- **Changes**: C-17, C-18 y C-22 (superficie); C-04 y C-08 (cadena reutilizada)

## Contexto

La revisión de la Parte 4 encontró que sus endpoints (`/cumplimiento/**`, `/metricas/**`, `/precio`) no tenían control de acceso. Al analizarlo:

- En `dev`/`test` **toda** la aplicación corre sin autenticación, por diseño de C-04/C-08 (actor simulado).
- **Todos** los controladores del proyecto son `@Profile({"dev", "test"})`: la spec `agenda` fija la superficie HTTP como indicativa de `dev`/`test`. La agenda tiene su cadena RBAC activa fuera de esos perfiles, aunque sus controladores todavía no estén ahí.
- La matriz de V001 tiene `exportacion:crear` (solo administrador), `auditoria:leer` (administrador y auditor) y `configuracion:leer`, pero no permisos específicos de dossier ni de métricas.

## Decisión

1. **Mismo modelo que la agenda.** Se agrega la política y la cadena de seguridad de la Parte 4, activas fuera de `dev`/`test`, y los controladores siguen en `dev`/`test` como el resto del proyecto. Llevar toda la superficie HTTP a producción queda como un change aparte.
2. **Permisos de V001, sin migración**:

| Operación | Permiso | Roles en el seed |
|---|---|---|
| Exportar, descargar exportación, respaldar, restaurar, escribir en el dossier | `exportacion:crear` | administrador |
| Leer dossier, exportaciones vencidas y métricas | `auditoria:leer` | administrador, auditor |
| Escribir en `/metricas` | ninguno (se niega) | — |
| `/precio` | público | todos |

El rol `paciente` nunca accede. La descarga de una exportación exige `exportacion:crear` y no `auditoria:leer`, aunque la KB marque «L» para el auditor en exportación: el contenido son todos los datos personales y el auditor puede verificar las operaciones sin descargarlos.

## Implementación

- `RbacPolicy`: interfaz común. `AgendaRbacPolicy` la implementa sin cambiar su comportamiento.
- `RbacAccessFilter`: el antiguo `AgendaAccessFilter`, generalizado para recibir cualquier `RbacPolicy`.
- `StaffFilterChain`: construcción común de la cadena (sesión, CSRF y RBAC), usada por la agenda y por cumplimiento.
- `CumplimientoRbacPolicy` y `CumplimientoSecurityConfig`: la política y la cadena nuevas.
- Spec nueva `openspec/specs/cumplimiento-rbac`.

## Alternativas descartadas

- **Llevar solo la Parte 4 a producción**: el dossier y las exportaciones quedarían en producción mientras la agenda, la clínica y las finanzas no.
- **Permisos nuevos con migración V033**: más fiel a la matriz de la KB, pero los roles que agregaría (recepcionista y odontólogo para métricas) figuran como `(TBD)` en la KB.

## Pendiente

- Productivizar la superficie HTTP de todo el proyecto, como un change propio.
- Si se confirman los roles `(TBD)` de la KB para métricas, agregar sus permisos con una migración.
