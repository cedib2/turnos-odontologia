# C-09 — Decisiones del PO para `public-booking-and-reschedule`

- **Change**: `C-09 public-booking-and-reschedule`
- **Governance local**: MEDIO (checkpoints); `CHANGES.md:352-367`
- **Decisor**: Lautaro Ellerbach, Product Owner (Parte 3)
- **Fecha**: 2026-11-22
- **Estado**: aprobado — opciones recomendadas más simples, seguras y alineadas con C-01..C-08
- **Alcance de este documento**: cierra los `(TBD)` detectados en la exploración para habilitar `/opsx:propose`. No implementa código, no fija secretos con valor real y no autoriza commits.
- **Fuentes**: `CHANGES.md` (C-09, reservas Flyway), `docs/decisiones/C-01-decisiones-habilitantes.md`, `C-06-decisiones-public-access-tokens.md`, `C-07-scheduling-catalogs-checkpoints.md`, `C-08-agenda-core-overlap-checkpoints.md`, `knowledge-base/03,05,06,07,09`, `openspec/specs/agenda/spec.md`, `openspec/specs/public-access-tokens/spec.md` y relevamiento de `src/`.

> Estas decisiones son la respuesta exigida por RN-GL-03: ningún `(TBD)` se resuelve por convención del agente. Lo que aquí no se decide sigue `(TBD)` y se registra como pendiente explícito, no como valor inventado.

## Decisiones aprobadas

### D1 — Contrato HTTP

- **Decisión**: superficie nueva bajo el prefijo `/publico/v1/turnos`, separada de la superficie dev/test `POST /turnos` (que no se modifica). Verbos: `GET` para disponibilidad y `POST` para crear, confirmar, cancelar y reprogramar. Token en **query** (`?token=...`) en todas las operaciones; `turnoId` en path para las operaciones `GESTIONAR` y verificación contra el claim `turnoId`. Versionado explícito `v1` en la ruta.
- **Errores**: cuerpo `{"error": "<motivo>"}`; `400` token inválido/petición inválida, `410` token vencido con `requiereNuevoEnlace=true`, `409` hueco tomado / fuera de ventana / operación duplicada, `404` entidad inexistente.
- **Justificación**: satisface la sugerencia indicativa de `/turnos` de `CHANGES.md` sin tocar los controladores `@Profile({"dev","test"})` ni debilitar las guardias de C-04; `v1` evita romper clientes futuros; un único mecanismo de transporte reduce superficie de fuga y simplifica la sanitización.

### D2 — Scopes por operación

- **Decisión**: `RESERVAR` para consultar disponibilidad y crear el turno; `GESTIONAR` para confirmar, cancelar y reprogramar, ligado al `turnoId` del claim y verificado contra el path. **No se crea un scope nuevo** (`CONFIRMAR`). El token `GESTIONAR` se emite internamente al crear el turno y se devuelve **una sola vez** en la respuesta de creación; no se agrega endpoint emisor en C-09.
- **Justificación**: usa el contrato ya aprobado en C-06 sin modificarlo; la creación es el único momento en que el sistema conoce el `turnoId`, por lo que es el punto natural de vinculación. Evita una ruta de emisión pública adicional.

### D3 — Ciclo de vida del token

- **Decisión**: `RESERVAR` se marca como usado (`marcarUsado`) **después** de una creación exitosa; puede usarse para varias consultas de disponibilidad antes de eso. `GESTIONAR` **no se rota automáticamente** en cada operación: permanece vigente hasta `exp` o hasta una rotación explícita invocada mediante el mecanismo existente de C-06. Un token vencido se rechaza sin efectos y la respuesta indica `requiereNuevoEnlace=true`; C-09 **no implementa reemisión** de enlaces.
- **Justificación**: evita que un mismo link de reserva cree varios turnos con claves de idempotencia distintas, sin romper el reenvío duplicado de una operación `GESTIONAR`. La rotación automática invalidaría el token en manos del paciente y complicaría la repetición idempotente.

### D4 — Hueco tomado entre consulta y confirmación

- **Decisión**: rechazo atómico (`409`) sin turno, sin ocupación y sin evento; se devuelven hasta **3 alternativas** calculadas con `DisponibilidadService` para el mismo profesional y tratamiento en la fecha solicitada, posteriores al inicio pedido. Sin alternativas → lista vacía. Sin waitlist.
- **Justificación**: aplica DD-03/RN-AG-01 con `write-back` real y aprovecha el servicio existente; la waitlist está diferida a v0.2 por C-01 (Gap C-19).

### D5 — Cómputo exacto de la ventana de 48 hs hábiles

- **Decisión**: «48 hs hábiles» = **2 días hábiles completos** inmediatamente anteriores al día local del `inicio` del turno en `America/Argentina/Buenos_Aires`. Se cuentan hacia atrás desde `fecha(inicio) - 1`, omitiendo sábados, domingos y las fechas presentes en la tabla `feriados` (vía `ConfiguracionSchedulingService.esFeriado`). El vencimiento de la ventana es `23:59:59` del segundo día hábil anterior. Una cancelación/reprogramación está **dentro de ventana** si `now >= vencimiento` y `now < inicio`. Fuera de ventana → `409` con `derivarARecepcion=true`, **sin liberar el hueco**, sin cambio de estado y sin evento de cancelación.
- **Justificación**: concreta C-01 C2 (días hábiles completos, feriados nacionales, zona acordada) sin contradecir DD-07. No agrega tablas: usa el catálogo de C-07. Los feriados efectivos dependen de los datos cargados por administración; mientras no haya seeds, solo se omiten fines de semana (limitación documentada, no bloqueante).

### D6 — Valores de `estado`, `origen` y transiciones

- **Decisión**: `estado` ∈ `reservado` (creación) → `confirmado` (confirmación) → `cancelado` (cancelación). La reprogramación **conserva el estado** y solo actualiza `inicio`/`fin` con el write-back ya existente. `origen` = `link` para toda reserva pública (QR es solo presentación del mismo enlace). Cancelar = `estado="cancelado"` + `activo=false` en `turnos` + `Ocupacion.desactivar()` en la misma transacción. Confirmar cambia solo `estado`.
- **Justificación**: son los únicos valores que el flujo necesita y ya aparecen en la enumeración de `knowledge-base/04_modelo_de_datos.md`; evita inventar una semántica más amplia mientras la spec exige no fijar valores por convención. La baja lógica libera ambas exclusion constraints de forma atómica.

### D7 — Idempotencia

- **Decisión**: clave `idempotency_key` en el cuerpo JSON de las cuatro operaciones `POST` (nunca en header). Nombres de operación: `turno.crear`, `turno.confirmar`, `turno.cancelar`, `turno.reprogramar` (coherente con los nombres ya usados en `PublicIdempotenciaPostgresTest`). Reenvío con misma operación y clave → `409` `{"error":"operacion_duplicada"}`, sin segundo efecto. El registro de la clave se hace **dentro de la misma transacción** que la escritura del turno. No se almacena la respuesta ni se agrega TTL.
- **Justificación**: cumple RN-GL-02 y la spec de C-06 con el esquema V004 existente; responder `409` en lugar de guardar un payload evita una migración y no expone datos de la primera respuesta a un reenvío no autenticado.

### D8 — Eventos de auditoría y WhatsApp

- **Decisión**: auditoría reutiliza `turno.creado` y `turno.reprogramado` y agrega `turno.confirmado` y `turno.cancelado` al catálogo; actor `ActorSimulado.pacienteSinUsuario()` (modo `PACIENTE`); metadata solo `turnoId`, `canal=link`, `origen=link`, hash de la clave de idempotencia y `motivo`, con `correlationId` nuevo. Quedan prohibidas las claves del `AuditoriaMetadataValidator` (`token`, `telefono`, `email`, `dni`, etc.). WhatsApp se modela como **puerto de solicitud de notificación con implementación NO-OP/simulada en dev/test**, que C-10 reemplazará por el outbox; no se agrega su tipo al catálogo de auditoría ni se afirma entrega.
- **Justificación**: mantiene el límite de evidencia de C-05/C-08, evita filtrar el token en metadata y deja el acoplamiento con C-10 detrás de un puerto, igual patrón que `AuditEventPublisher`.

### D9 — Migraciones Flyway

- **Decisión**: C-09 **no crea migración**: V004 (metadata de tokens e idempotencia), V005 (catálogos) y V006 (turnos/ocupaciones) son suficientes; `turnos.token_publico` ya existe. Si durante el apply surge una necesidad de esquema, se usa **V020 o superior**, reserva de la Parte 3 (`CHANGES.md:35-48`); nunca se edita una migración aplicada.
- **Justificación**: evita esquema especulativo y respeta la tabla de numeración que prevalece sobre los números históricos de los scopes.

### D10 — Paciente e identidad desde el token

- **Decisión**: `pacienteId` del claim (String) se parsea a `Long`; si no es numérico o falta, se rechaza como `token_no_valido`. `sub` es informativo y **no autoriza**. El paciente debe existir en `pacientes`; C-09 **no crea pacientes** ni recolecta datos personales nuevos. Para `GESTIONAR`, el `turnoId` del claim se parsea a `Long` y debe coincidir con el `turnoId` del path; si no, `token_no_valido`.
- **Justificación**: `Turno.paciente_id` es obligatorio y `Paciente` exige documento/contacto/consentimiento; recolectarlos en superficie pública aumentaría el riesgo de cumplimiento sin necesidad del flujo.

### D11 — Catálogo de scheduling (C-07)

- **Decisión**: C-09 **no modifica** `ConfiguracionSchedulingService`, no agrega seeds y no cambia el tratamiento de `activo` ni `aplica_a_todos`; consume `DisponibilidadService`, `esFeriado`, `estaDentroDeHorario` y `duracionMinutos` tal como están. La anticipación mínima/máxima sigue `(TBD)` fuera de C-09.
- **Justificación**: respeta la decisión de C-07 y el requisito «Sin efecto en lectura de disponibilidad de C-08»; cambiar esa semántica exigiría deltas `MODIFIED` y reabrir tests de C-07/C-08.

### D12 — Alcance, checkpoints y verificación

- **Decisión**: governance MEDIO con dos checkpoints: (1) contrato HTTP + wiring + scopes + idempotencia; (2) write-back, cancelación, ventana y eventos. Definición de done: tests E2E de Flujo 1 y 2, carrera por hueco contra PostgreSQL real, límite de ventana, reenvío duplicado y validación estricta del change. Sin UI (`Decision: no (no UI)`), sin RBAC de agenda, sin adaptador de actor de C-05 y sin despliegue.
- **Justificación**: coincide con `CHANGES.md` y con la política de design-mockups para cambios no visuales; la deuda del adaptador queda condicionada a proteger la agenda, no a C-09 (`docs/decisiones/C-05-deuda-adaptador-actor.md`).

### D13 — Arranque, wiring y configuración

- **Decisión**: C-09 declara los beans de C-06 en un paquete nuevo `com.turnosodontologia.publicbooking` (sin modificar las clases de `publictoken`), con propiedades `public.token.issuer` (default no secreto) y `PUBLIC_TOKEN_SECRET` **solo como nombre** en `.env.example`, sin valor. Configura `public.surface.prefixes=/publico/v1` en los perfiles dev/test para que los headers obligatorios apliquen. La verificación E2E se apoya en el contexto Spring de test, igual que los tests existentes. La ausencia de `@SpringBootApplication` en `src/main` queda registrada como limitación de infraestructura fuera de C-09 (C-17/C-18), no como bloqueante de este change.
- **Justificación**: C-06 entregó servicios probados con `new`, sin beans; cablearlos es condición necesaria para exponer endpoints. Tocar secretos/configuración amerita checkpoint antes de escribir el nombre de la variable; no se introduce valor real ni dependencia nueva.

### D14 — Camino de escritura obligatorio

- **Decisión**: todas las escrituras públicas pasan por servicios de aplicación que insertan/desactivan `Ocupacion` (los existentes `CreacionTurnoService`/`ReprogramacionService` o variantes públicas que reutilicen la misma lógica). Queda prohibido insertar directamente en `turnos` o `bloqueos`. La cancelación desactiva turno y ocupación en una sola transacción. No se agrega constraint nueva en C-09; V020+ queda reservado si se decide reforzar la BD más adelante.
- **Justificación**: las exclusiones de `turnos` y `bloqueos` son independientes; solo `ocupaciones` garantiza el anti-solapamiento cruzado. Forzar el camino de servicio evita depender de un insert correcto sin cambiar el esquema.

### D15 — Validación del token en base de datos

- **Decisión**: **no modificar** el contrato de validación de C-06 (firma HMAC, `iss`, `exp`, disponibilidad de `jti`). `token_hash` permanece como metadato de auditoría; no se compara en cada validación y `rotar` no se revalida en C-09.
- **Justificación**: la firma ya cubre `jti`, `iat` y `exp`; comparar el hash no aporta seguridad adicional y obligaría a cambiar la API y los tests de C-06. C-09 solo invoca `marcarUsado` según D3.

### D16 — `Turno.token_publico`

- **Decisión**: almacenar el **`jti` del token `GESTIONAR`** (≤64 caracteres, cabe en `VARCHAR(120)`), nunca el token en texto plano; agregar `TurnoRepository.findByTokenPublico(String jti)`. Si en el futuro se rota el token, se actualiza este campo con el nuevo `jti`. Sin migración nueva.
- **Justificación**: C-06 permite expresamente guardar `jti` o hash; usar `jti` permite localizar el turno desde el token sin duplicar secreto y aprovecha el índice `ix_turnos_token_publico` ya creado.

### D17 — Perfil de verificación de auditoría

- **Decisión**: los tests E2E corren en perfil `test` con `InMemoryAuditEventRecorder` y verifican emisión (tipo, actor modo `PACIENTE`, `correlationId`, metadata sanitizada). En perfiles no test, `JdbcAuditEventPublisher` persiste en `auditoria_log`. C-09 no agrega API de consulta ni presenta los eventos como evidencia de cumplimiento.
- **Justificación**: respeta el condicionamiento por perfil de `AuditPersistenceConfig` y el límite de evidencia heredado de C-05/C-08.

## Pendientes explícitos (no resueltos aquí)

- Anticipación mínima/máxima de reserva (RN-AG-04) — sigue `(TBD)` fuera de C-09.
- Seeds de feriados, horarios y duraciones (C-07) — siguen `(TBD)`; la ventana solo omite fines de semana hasta que se carguen.
- Emisión/reemisión de enlaces y QR, y ausencia de aplicación ejecutable en `src/main` (C-17/C-18).
- Protección RBAC de la agenda y adaptador `AuthenticatedActor` → `CurrentActorProvider` (deuda de C-05, change posterior).
- Webhooks de WhatsApp y Mercado Pago (C-10/C-15) y lista de espera (C-19, v0.2).

## Checkpoints D12 — aprobación registrada

- **Checkpoint 1 — contrato HTTP, wiring, scopes e idempotencia**: aprobado el 2026-11-22 por el usuario/PO durante la implementación («Aprobar ambos ahora», opción recomendada). Cubre `/publico/v1/turnos`, token en query, scopes `RESERVAR`/`GESTIONAR`, verificación de `turnoId`, `idempotency_key` en cuerpo con operaciones `turno.crear|confirmar|cancelar|reprogramar`, beans de C-06 en `com.turnosodontologia.publicbooking` y headers `no-referrer`/`no-store`. Sin objeciones ni cambios al contrato aprobado.
- **Checkpoint 2 — write-back, cancelación, ventana y eventos**: aprobado el 2026-11-22 por el usuario/PO en la misma decisión. Cubre creación y liberación atómicas vía `Ocupacion`, rechazo `409` por hueco tomado con alternativas, ventana de 2 días hábiles completos en `America/Argentina/Buenos_Aires`, transiciones `reservado`/`confirmado`/`cancelado`, auditoría con actor `PACIENTE` y metadata sanitizada, y notificación simulada sin entrega real. Sin objeciones ni cambios al diseño aprobado.

Evidencia de cierre: `mvn test` en verde (219 tests, 0 fallos, 0 errores) y `openspec validate public-booking-and-reschedule --strict` válido. Ninguna aprobación autoriza commits, push ni archivado del change sin pedido explícito.

## Regla de aplicación

Ninguna de estas decisiones autoriza escritura en `src/`, commits ni push. La implementación queda para el ciclo OPSX `apply` de C-09, con los dos checkpoints de D12 y verificación técnica antes de marcar `[x]` en `CHANGES.md`.
