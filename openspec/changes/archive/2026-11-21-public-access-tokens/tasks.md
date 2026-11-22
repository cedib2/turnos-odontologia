# Tasks

Criterios de aceptación: `specs/public-access-tokens/spec.md` (8 escenarios) y `specs/agenda/spec.md` (2 escenarios adicionales). Detalle técnico: `design.md`. Contrato para C-09/C-13: `docs/decisiones/C-06-decisiones-public-access-tokens.md`. El apply fue aprobado por el PO y las 26 tareas quedaron ejecutadas y verificadas; el change sigue detenido antes del archive a la espera de aprobación posterior.

> Proceso vigente (decisión del PO, 2026-11-21, `Lautaro Ellerbach, PO`; mismo ajuste de tiempos de C-04): (1) un test por escenario del spec, sin triangulación extra porque C-06 no tiene excepciones; (2) un ciclo por grupo — escribir tests, implementar, correr una vez — sin fase `REFACTOR` separada; (3) durante el desarrollo correr solo los tests del grupo con `mvn test -Dtest=...`, y la suite completa al cerrar; (4) reutilizar el único PostgreSQL compartido en `src/test/java/com/turnosodontologia/support/PostgresFixture.java`. Invariantes: cobertura de cada escenario, 147 tests previos en verde y `openspec validate public-access-tokens --strict` válido. No ajustar `AgendaSurfaceTest` ni `CurrentActorProviderTest`; no tocar `openspec/specs/**`, `CHANGES.md` ni `openspec/specs/audit-event-emission/spec.md`.

## 1. T1 — Contrato documentado, secretos y baseline

- [x] 1.1 Crear `docs/decisiones/C-06-decisiones-public-access-tokens.md` con decisor `Lautaro Ellerbach, PO`, fecha 2026-11-21, valores como metas iniciales no normativas, tabla de scopes/claims/flag y las seis decisiones más las restricciones del PO; verificar que la tabla coincide literalmente con la del spec y que el doc declara que C-09 y C-13 deben consumirla.
- [x] 1.2 Agregar únicamente el nombre `PUBLIC_TOKEN_SECRET` a `.env.example`, sin valor y distinto de `AUTH_JWT_SECRET`; verificar por inspección que no hay secretos con valor real ni cambios en otros nombres.
- [x] 1.3 Verificar el baseline con `mvn test` (147 tests previos en verde) y `openspec validate public-access-tokens --strict`; registrar ambos resultados antes de escribir código del apply.

## 2. T2 — Migración V004 solo metadatos (escenario 8)

- [x] 2.1 RED: escribir el test de migración que verifica, sobre PostgreSQL/Testcontainers con V006 registrada y `out-of-order=true`, que V004 se aplica, que el SQL no crea `Turno.token_publico` y que el esquema resultante no contiene el token en texto plano; ejecutar `mvn test -Dtest=...` y verificar rojo.
- [x] 2.2 GREEN: crear `src/main/resources/db/migration/V004__public_access_tokens.sql` solo con tablas de metadatos (`jti`/hash, idempotencia), unicidades e índices, sin duplicar `token_publico` ni guardar el token en claro; verificar que el test del grupo pasa con `mvn test -Dtest=...`.

## 3. T3 — Emisión, contrato de scopes e identidad no verificada (escenario 4)

- [x] 3.1 RED: escribir el test del escenario 4 — token `HISTORIA` vigente con `identidadVerificada=false` que debe denegar lectura/exportación y publicar un evento SIMULADO sin persistirlo ni exponer consulta; ejecutar `mvn test -Dtest=...` y verificar rojo.
- [x] 3.2 GREEN: implementar emisión y validación con los claims mínimos, vigencias por scope, vinculación `turnoId`/`pacienteId`, `identidadVerificada=false` invariable y publicación vía `AuditEventPublisher`; verificar que el test del grupo pasa con `mvn test -Dtest=...` y que no se emite `HISTORIA` con `true`.

## 4. T4 — Token vencido (escenario 1)

- [x] 4.1 RED: escribir el test del escenario 1 — token firmado con `exp` vencido presentado en superficie pública, sin efectos de negocio y con indicación de pedir nuevo enlace según C-09; ejecutar `mvn test -Dtest=...` y verificar rojo.
- [x] 4.2 GREEN: implementar la validación de `exp` y el rechazo sin efectos; verificar que el test del grupo pasa con `mvn test -Dtest=...`.

## 5. T5 — Token forjado o emisor inválido (escenario 2)

- [x] 5.1 RED: escribir el test del escenario 2 — token con firma inválida y token con `iss` distinto, ambos rechazados sin identidad, sin efectos y sin revelar la causa; ejecutar `mvn test -Dtest=...` y verificar rojo.
- [x] 5.2 GREEN: implementar verificación HMAC-SHA256 en tiempo constante y validación estricta de `iss` con secreto propio; verificar que el test del grupo pasa con `mvn test -Dtest=...`.

## 6. T6 — `jti` reutilizado y rotación solo de GESTIONAR (escenario 3)

- [x] 6.1 RED: escribir el test del escenario 3 — `jti` usado/revocado rechazado, intento de rotar `RESERVAR`/`HISTORIA` denegado y rotación de `GESTIONAR` que emite nuevo `jti` y marca el anterior `used_at`; ejecutar `mvn test -Dtest=...` y verificar rojo.
- [x] 6.2 GREEN: implementar el registro de `jti`, `used_at`/`revoked_at` y la transición atómica de rotación restringida; verificar que el test del grupo pasa con `mvn test -Dtest=...`.

## 7. T7 — Idempotencia PostgreSQL (escenario 5)

- [x] 7.1 RED: escribir el test del escenario 5 con `PostgresFixture` — mismo `idempotency_key` en la misma operación no produce segundo efecto, mientras la misma clave en operación distinta sí es admitida; ejecutar `mvn test -Dtest=...` y verificar rojo.
- [x] 7.2 GREEN: implementar la unicidad por `(operacion, idempotency_key)` y la devolución de repetición sin segundo efecto; verificar que el test del grupo pasa con `mvn test -Dtest=...` contra PostgreSQL y que la clave nunca viaja en request headers.

## 8. T8 — Response headers de superficie pública (escenario 7)

- [x] 8.1 RED: escribir el test del escenario 7 con un slice de test sin endpoints de negocio en producción — respuestas de éxito y rechazo incluyen `Referrer-Policy: no-referrer` y `Cache-Control: no-store`, sin leer request headers; ejecutar `mvn test -Dtest=...` y verificar rojo.
- [x] 8.2 GREEN: implementar el componente que fija ambos headers mediante la configuración de prefijos públicos; verificar que el test del grupo pasa con `mvn test -Dtest=...`.

## 9. T9 — Token ausente de logs (escenario 6)

- [x] 9.1 RED: escribir el test del escenario 6 — operaciones válidas, vencidas, forjadas y rechazadas con token en path/query, verificando con appender que el token y sus fragmentos identificables no aparecen en logs ni en eventos SIMULADOS; ejecutar `mvn test -Dtest=...` y verificar rojo.
- [x] 9.2 GREEN: implementar la sanitización de URI/query, excepciones y metadata de eventos; verificar que el test del grupo pasa con `mvn test -Dtest=...`.

## 10. T10 — Delta de agenda: consumo sin credenciales de staff (2 escenarios)

- [x] 10.1 RED: escribir el test del primer escenario del delta — validación con token vigente por path/query sin credenciales de staff, sin login/RBAC ni request headers de identidad, con controladores de agenda/catálogos intactos; ejecutar `mvn test -Dtest=...` y verificar rojo.
- [x] 10.2 RED: escribir el test del segundo escenario del delta — inspección de que los 12 requisitos y escenarios de `agenda`, `AgendaSurfaceTest`, `CurrentActorProviderTest` y `audit-event-emission` siguen intactos; ejecutar `mvn test -Dtest=...` y verificar rojo.
- [x] 10.3 GREEN: implementar el punto de consumo público sin tocar los controladores existentes ni las guardias; verificar que ambos tests del grupo pasan con `mvn test -Dtest=...`.

## 11. T11 — Verificación integral y cierre

- [x] 11.1 Ejecutar la suite completa con `mvn test` y verificar 147 tests previos en verde más los nuevos, sin failures ni errors; registrar el conteo como evidencia.
- [x] 11.2 Ejecutar `openspec validate public-access-tokens --strict` y `openspec validate --specs` (3/3), y verificar que `openspec list --json` muestra exactamente un change activo.
- [x] 11.3 Verificar que `CHANGES.md` sigue sin marcar C-06, que `openspec/specs/**` y `audit-event-emission` no cambiaron, que no hay secretos con valor real, commits ni push, y que las guardias de C-04 no fueron editadas.
- [x] 11.4 Verificar en los artefactos que el contrato de scopes/claims/flag aparece en el spec y en el doc de decisiones, y que el apply sigue detenido antes del archive a la espera de aprobación del PO.

## Workflow follow-up

- El PO aprobó el propose y el apply de este change CRITICO, y autorizó corregir la ruta rota de `LoginRateLimiterTest` hacia `openspec/changes/archive/2026-11-21-auth-rbac/design.md`.
- La ejecución se detiene antes del archive: ese paso requiere una decisión posterior del PO. No se sincronizan deltas, no se marca C-06 en `CHANGES.md` y no se toca `openspec/specs/**`.
