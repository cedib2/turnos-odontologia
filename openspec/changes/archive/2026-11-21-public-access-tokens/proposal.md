# Proposal

## Why

C-04 ya entrega actores de staff y credenciales acotadas a `/auth/**`, pero la superficie pública que necesitan C-09 (reserva/gestión) y C-13 (historia del paciente) todavía no tiene un mecanismo propio de acceso: la KB exige enlaces/QR sin cuenta de usuario y deja el algoritmo, el vencimiento y la verificación de identidad como `(TBD)` (RN-AG-06, RN-GL-02). Sin C-06, los cambios de los compañeros no pueden exponer rutas públicas ni distinguir una operación legítima de un token forjado, vencido o reenviado. El PO aprobó explícitamente este **propose** el 2026-11-21 con valores numéricos como metas iniciales no normativas; el **apply** requiere otra aprobación explícita.

## What Changes

- Nueva capability `public-access-tokens`: emisión y validación de tokens de enlace/QR con firma **HMAC-SHA256** y secreto propio `PUBLIC_TOKEN_SECRET`, distinto de `AUTH_JWT_SECRET`. El nombre de la variable se documenta; su valor no se escribe en el repositorio.
- Tres scopes con vigencia objetivo: `RESERVAR` 60 min (sin `turnoId` al emitir), `GESTIONAR` 7 días (ligado a `turnoId`, con rotación permitida) y `HISTORIA` 24 h (ligado a `pacienteId`). `identidadVerificada=false` por defecto y C-06 **nunca** emite `HISTORIA` con `true`; el mecanismo real de verificación queda para C-13.
- Claims mínimos del token: `jti`, `iss`, `sub`/`pacienteId`, `scope`, `iat`, `exp`, `turnoId` opcional e `identidadVerificada` (`typ` opcional según el diseño). El contrato completo se escribe en el delta de spec y en `docs/decisiones/C-06-decisiones-public-access-tokens.md` para consumo de C-09 y C-13.
- `jti` con `used_at`/`revoked_at`; rechazo de `jti` usado o revocado y rotación **solo** para `GESTIONAR`.
- Anti-reenvio (RN-GL-02): `idempotency_key` persistida con unicidad por operación y probada contra PostgreSQL; viaja en cuerpo o path, **nunca** en un request header.
- Transporte por path/query, sin request headers de credenciales; respuestas de superficie pública con `Referrer-Policy: no-referrer` y `Cache-Control: no-store`, y el token nunca aparece en logs.
- Migración reservada `V004__public_access_tokens.sql` **solo de metadatos**: no duplica `Turno.token_publico` (ya existe en V006) y no guarda el token en texto plano; persiste `jti` o hash. En bases donde V006 ya está registrada, Flyway requiere `spring.flyway.out-of-order=true` en dev/test (ya presente en dev; considerar el escenario de test).
- Eventos de auditoría **SIMULADOS** mediante `AuditEventPublisher`; no se persiste auditoría ni se crea API de consulta. Las guardias de C-04 (`AgendaSurfaceTest`, `CurrentActorProviderTest`) quedan intactas: el código público de C-06 no contiene `Authorization`, `Bearer `, `@RequestHeader`, `SecurityContext` ni la ruta `/auth`; setear response headers está permitido.
- Delta aditivo y mínimo en `agenda` para declarar que la superficie pública consume tokens sin credenciales de staff, sin modificar ningún requisito ni guardia existente. `audit-event-emission` permanece sin cambios.

**Fuera de alcance**: endpoints de negocio `/turnos` de C-09 y consulta/descarga de historia de C-13; webhooks de WhatsApp (C-10) y Mercado Pago (C-15) con firma propia; QR/UI y generación de links; cálculo de la ventana de 48 hs hábiles y write-back/liberación atómica; persistencia append-only, hash encadenado, retención y API de auditoría de C-05; ajuste de las guardias de C-04; código de producción, tests de implementación, commits o push en este propose.

## Capabilities

### New Capabilities
- `public-access-tokens`: contrato observable de emisión, scopes/claims, vencimiento, firma, `jti`, rotación, idempotencia y transporte seguro del token de acceso público; incluye el evento SIMULADO de identidad no verificada.

### Modified Capabilities
- `agenda`: un único requisito `ADDED` y acotado que explicita que la superficie pública consume tokens de C-06 sin credenciales de staff. Sus 12 requisitos y todos sus escenarios actuales permanecen intactos; no hay `MODIFIED` ni `REMOVED`. `audit-event-emission` no se modifica y sigue en modo `SIMULADO` hasta C-05.

## Impact

- Código (en el futuro apply): nuevo paquete de tokens público separado de `auth`, servicio de emisión/validación, persistencia de metadatos `jti`/hash y filtro o respuesta HTTP que fija los dos response headers; sin reutilizar `Usuario`/`Rol` para pacientes públicos.
- Esquema: `src/main/resources/db/migration/V004__public_access_tokens.sql` (solo metadatos; sin `token_publico` duplicado ni token en claro).
- Configuración: `.env.example` recibirá **solo el nombre** `PUBLIC_TOKEN_SECRET` durante el apply, sin valor; dev/test con Flyway `out-of-order` cuando V006 ya esté registrada.
- Specs: capability nueva `public-access-tokens`, delta aditivo en `agenda`, `audit-event-emission` intacto; sin tocar `openspec/specs/**` durante el propose.
- Tests (en el futuro apply): un test por escenario del spec, con PostgreSQL/Testcontainers reutilizando `PostgresFixture` para unicidad de `idempotency_key` y escenario de V006 previa; línea base vigente de 147 tests verdes.
- Gobernanza: CRITICO; proposición y apply aprobados por el PO el 2026-11-21. El apply quedó **COMPLETADO_VERIFICADO_SIN_ARCHIVE** (26/26 tareas, 157 tests); el archive sigue pendiente de aprobación posterior.
