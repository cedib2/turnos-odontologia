# C-04 — Fix de ruta estable para la documentación del límite no distribuido

- **Decisor**: Lautaro Ellerbach, PO
- **Fecha**: 2026-11-21
- **Change relacionado**: `C-04 auth-rbac` (archivado) · **Desbloquea**: `C-06 public-access-tokens` T1.3
- **Estado**: aprobado y aplicado; safety net reanudado

## Motivo

`LoginRateLimiterTest.disenoYDecisionesDeclaranQueElLimiteNoEsDistribuido` leía `openspec/changes/auth-rbac/design.md`, una ruta de change **activo**. Al archivar C-04, esa carpeta se movió a `openspec/changes/archive/2026-11-21-auth-rbac/`, por lo que el test falló con `NoSuchFileException` en el safety net de C-06: 147 tests, 0 failures, 1 error. No era un defecto de comportamiento ni una regresión de C-06.

## Decisión

Leer únicamente `docs/decisiones/C-04-decisiones-auth-rbac.md` como fuente estable de la declaración del límite en memoria. El PO verificó que ese documento contiene las cinco frases exigidas («en memoria», «no distribuido», «se esquiva rotando de IP», «no se comparte entre réplicas» y «se pierde al reiniciar el proceso») en sus líneas 20 y 33. No es necesario usar la ruta archivada.

## Alcance

- **Único cambio**: eliminar la lectura de `openspec/changes/auth-rbac/design.md` en `src/test/java/com/turnosodontologia/auth/LoginRateLimiterTest.java`.
- Sin cambios de assertions, guardias, comportamiento ni código de producción.
- Sin cambios en `openspec/changes/archive/**`, `CHANGES.md`, specs ni secretos.
- Sin commits, push ni archive.

## Regla derivada para C-06

Ningún test de verificación documental debe leer desde `openspec/changes/<change-activo>/`. Las rutas bajo `openspec/changes/archive/` son estables y están permitidas; para C-06 se usa `docs/decisiones/C-06-decisiones-public-access-tokens.md` en lugar de la ruta activa del delta.
