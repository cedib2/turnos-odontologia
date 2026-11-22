# Deuda de C-05 — adaptador de `AuthenticatedActor` a `CurrentActorProvider`

- **Fecha**: 2026-11-22
- **Decisor**: Lautaro Ellerbach, PO
- **Change**: C-05 `audit-append-only-log` · **Gobernanza**: CRITICO
- **Estado**: deuda explícita, diferida; **no bloquea C-05**

## Deuda

No se construye en C-05 el adaptador que convierta el `AuthenticatedActor` real de C-04 (`usuarioId`, `nombre`, `rol`, `jti`, `ModoActor`) en el `ActorSimulado` que consume `CurrentActorProvider` en los servicios de agenda. El puerto de actor sigue sirviéndose del proveedor simulado en dev/test y del proveedor real fuera de esos perfiles; la instantánea persistida por C-05 conserva modo, nombre, rol y `jti` sin reabrir C-04 ni introducir lectura de identidad en la superficie de agenda.

## Condición y alcance posterior

El adaptador queda **condicionado a proteger la agenda**: debe tratarse en el change que introduzca esa protección, con su propio propose, spec, decisión PO y tests. No se implementa por convención ni durante el archive de C-05. Debe conservar las guardias `AgendaSurfaceTest` y `CurrentActorProviderTest` y la semántica de los cuatro modos (`REAL`, `SIMULADO`, `SYSTEM`, `PACIENTE`).

## Impacto en C-08

C-08 `agenda-core-overlap` permanece `[ ]` en `CHANGES.md`. Su condición de marcado sigue supeditada a la decisión PO sobre los puertos simulados/reales; C-05 completa el puerto de auditoría, pero esta deuda impide dar por reemplazado el puerto de actor. El marcado de C-08 no se realiza en este change.

## Registro

Esta deuda se registra solo en documentación; no modifica código, specs principales ni `CHANGES.md`. Decisión relacionada: `docs/decisiones/C-05-ajustes-pre-apply-PO.md`, sección «Corrección 4».

## Resolución (2026-11-22)

Deuda resuelta en el cierre de C-08: ver [`C-08-cierre-adaptador-actor-rbac.md`](C-08-cierre-adaptador-actor-rbac.md). El adaptador y la protección RBAC viven en el paquete `auth`; las guardias `AgendaSurfaceTest` y `CurrentActorProviderTest` no se modificaron y los cuatro modos conservan su semántica.
