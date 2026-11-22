# C-06 — Decisiones del PO sobre `public-access-tokens`

- **Change**: `C-06 public-access-tokens`
- **Governance**: `CRITICO`
- **Decisor**: Lautaro Ellerbach, PO
- **Fecha**: 2026-11-21
- **Estado**: propose y apply aprobados explícitamente por el PO; **apply COMPLETADO y verificado**, detenido antes del archive a la espera de aprobación posterior
- **Alcance de este documento**: contrato de scopes, claims y flag `identidadVerificada`, decisiones y restricciones aprobadas. Es la fuente que **C-09 y C-13 deben consumir**; el mismo contrato queda escrito en `openspec/changes/public-access-tokens/specs/public-access-tokens/spec.md`.
- **Dependencias**: C-04 (`auth-rbac`) ya archivado. C-06 **no depende de C-05**. Orden aprobado por el PO: `C-04 → C-06 → C-05`.
- **Decisión visual**: `Decision: no (no UI)` — C-06 no cambia páginas, pantallas, componentes, layout, estilos ni interacciones.
- **Fuentes**: `knowledge-base/03_actores_y_roles.md`, `08_arquitectura_propuesta.md`, `05_reglas_de_negocio.md` (RN-AG-06, RN-GL-02), `10_preguntas_abiertas.md`, `07_flujos_principales.md`, `06_funcionalidades.md`, `CHANGES.md`, `.active-orchestrator-state.json` v24.

> Todos los valores numéricos y plazos siguientes son **metas iniciales no normativas** aprobadas para el propose. No se inventan valores por convención (RN-GL-03) y ningún secreto tiene valor real en el repositorio.

## Contrato de scopes, claims e identidad

| Scope | Uso | Vinculación | Exp | Verificación |
|---|---|---|---|---|
| `RESERVAR` | consultar disponibilidad y crear turno (C-09) | sin `turnoId` al emitir | 60 min | no requiere identidad verificada |
| `GESTIONAR` | confirmar/cancelar/reprogramar (C-09) | ligado a `turnoId`, rotación permitida | 7 días | no requiere identidad verificada |
| `HISTORIA` | solo lectura/exportación de propia historia (C-13) | ligado a `pacienteId` | 24 h | `identidadVerificada=false` por defecto; C-06 nunca emite `true` |

**Claims mínimos**: `jti`, `iss`, `sub`/`pacienteId`, `scope`, `iat`, `exp`, `turnoId` opcional e `identidadVerificada`. `typ` también puede incluirse si el diseño lo requiere.

**Invariantes de superficie**: token por path/query; nunca `@RequestHeader`, `Authorization`, `Bearer ` ni `/auth`; response headers `Referrer-Policy: no-referrer` y `Cache-Control: no-store`; token nunca en logs. El `idempotency_key` viaja en cuerpo o path, nunca en un request header.

## Decisiones aprobadas

1. **Firma**: HMAC-SHA256 con `PUBLIC_TOKEN_SECRET` propio, distinto de `AUTH_JWT_SECRET`. Solo el **nombre** de la variable se agregará a `.env.example` durante el apply, sin valor. No se modifica `.env.example` durante el propose.
2. **Vigencia**: `RESERVAR` 60 min, `GESTIONAR` 7 días, `HISTORIA` 24 h (metas iniciales no normativas).
3. **Identidad**: `identidadVerificada=false` por defecto. C-06 **nunca** emite `HISTORIA` con `true`. El mecanismo real de verificación queda para C-13.
4. **Idempotencia**: `idempotency_key` persistida con unicidad por operación y probada contra PostgreSQL; viaja en cuerpo o path, nunca en request header.
5. **Transporte y headers**: token en path/query sin request headers; response headers `Referrer-Policy: no-referrer` y `Cache-Control: no-store`. Los tests se extienden para que el token nunca aparezca en logs.
6. **`jti` y revocación**: `jti` con `used_at`/`revoked_at`. Rotación **solo para `GESTIONAR`**.

## Restricciones adicionales del PO

- `V004__public_access_tokens.sql` será **solo metadatos**: no duplicar `Turno.token_publico` (ya existe en V006) y no guardar el token en texto plano; solo `jti` o hash.
- No ajustar las guardias de C-04 (`AgendaSurfaceTest`, `CurrentActorProviderTest`). El código público de C-06 no debe contener `Authorization`, `Bearer `, `@RequestHeader`, `SecurityContext` ni la ruta `/auth`. Setear response headers está permitido; leer request headers no.
- El contrato de scopes, claims y flag `identidadVerificada` queda documentado en el spec delta y en este documento para que C-09 y C-13 puedan consumirlo.
- Sin commits, sin push y sin secretos con valor real.
- C-06 no reutiliza `Usuario`/`Rol` para pacientes públicos; `AuditEventPublisher` sigue SIMULADO, sin auditoría persistente ni API de consulta.
- En bases donde V006 ya está registrada, Flyway requiere `spring.flyway.out-of-order=true` en dev/test; ya está en `application-dev.yml`, y el futuro apply debe considerar el escenario de test con V006 previa.

## Escenarios que debe cubrir el apply

1. Token vencido → rechazo; cubre C-09 «token vencido → solicitar nuevo enlace».
2. Token forjado → firma/iss inválidos rechazados; cubre C-09/C-13.
3. Token reutilizado → `jti` usado/revocado rechazado; rotación solo `GESTIONAR`; cubre C-09/C-13.
4. Identidad no verificada → `HISTORIA` denegada y evento SIMULADO emitido; cubre C-13.
5. Reenvío duplicado → misma `idempotency_key` no produce segundo efecto, con unicidad probada en PostgreSQL; cubre RN-GL-02/C-09/C-13.
6. Token nunca aparece en logs.
7. Response headers `Referrer-Policy: no-referrer` y `Cache-Control: no-store` presentes en respuestas de superficie pública.
8. `V004` solo metadatos: sin `Turno.token_publico` duplicado y sin token en texto plano.

## Alcance explícitamente diferible

- Endpoints de negocio `/turnos` de C-09 y consulta/descarga de historia de C-13.
- Webhooks de WhatsApp/C-10 y Mercado Pago/C-15 con firma propia.
- QR/UI y generación de links.
- Cálculo de ventana de 48 hs hábiles y write-back/liberación atómica.
- Persistencia append-only, hash encadenado, retención y API de auditoría de C-05.
- Ajuste de guardias de C-04 (quedan intactas).

## Proceso vigente para el futuro apply

- Un test por escenario del spec; sin triangulación extra salvo excepciones explícitas (C-06 no tiene excepciones).
- Un ciclo por grupo: escribir tests, implementar, correr una vez; sin fase `REFACTOR` separada.
- Durante desarrollo correr solo tests del grupo con `mvn test -Dtest=...`; suite completa al cerrar.
- Reutilizar el único contenedor PostgreSQL compartido en `src/test/java/com/turnosodontologia/support/PostgresFixture.java`.
- Invariantes: cobertura de cada escenario, tests previos en verde (147 antes de C-06) y `openspec validate public-access-tokens --strict` válido.
- El apply fue aprobado por el PO el 2026-11-21; se completaron las 26 tareas y se detuvo antes del archive. Sin archive, commits ni push.

## Apply: bloqueo resuelto y verificación final

- **Aprobación**: `Lautaro Ellerbach, PO`, 2026-11-21, con proceso vigente y restricciones de superficie pública.
- **Bloqueo preexistente**: `mvn test` → 147 tests, 0 failures, **1 error** en `LoginRateLimiterTest.disenoYDecisionesDeclaranQueElLimiteNoEsDistribuido:102`; el test leía `openspec/changes/auth-rbac/design.md`, ruta eliminada por el archive de C-04. El archivo vigente es `openspec/changes/archive/2026-11-21-auth-rbac/design.md`.
- **Resolución autorizada**: el PO autorizó corregir esa ruta en `src/test/java/com/turnosodontologia/auth/LoginRateLimiterTest.java`; registro en `docs/decisiones/C-04-fix-ruta-estable-documentacion.md`. No se flexibilizó el safety net ni se modificaron las guardias de C-04.
- **Progreso final**: 26/26 tareas en `openspec/changes/public-access-tokens/tasks.md`, con un test por escenario y implementación mínima por grupo.
- **Verificación**: `mvn test` → **157 tests, 0 failures, 0 errors** (58 clases con reporte Surefire); `openspec validate public-access-tokens --strict` → `Change 'public-access-tokens' is valid`; `openspec validate --specs` → `Totals: 3 passed, 0 failed (3 items)`; `openspec list --json` → exactamente un change activo, `public-access-tokens`.
- **Invariantes**: `CHANGES.md` sigue con C-06 en `[ ] pendiente`; `openspec/specs/**` y `audit-event-emission` sin cambios; sin referencias a `openspec/changes/` en tests; token solo en path/query; token ausente de logs; `idempotency_key` hasheada en PostgreSQL; `identidadVerificada=false`; rotación solo de `GESTIONAR`; eventos SIMULADOS sin auditoría persistente ni `/audit`.
- **Sin secretos ni versionado**: `.env.example` contiene solo el nombre `PUBLIC_TOKEN_SECRET`, sin valor; no hubo commits ni push.
- **No realizado**: archive, sincronización de deltas a `openspec/specs/**` y marcado de C-06 en `CHANGES.md`. Pendiente de decisión posterior del PO.
