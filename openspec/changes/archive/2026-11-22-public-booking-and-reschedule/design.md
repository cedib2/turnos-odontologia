# Design

## Context

Ver `proposal.md` para la motivación. El estado actual que condiciona el diseño es:

- C-06 entregó emisión/validación de tokens, metadatos `jti`, idempotencia, sanitización y response headers como clases sin beans Spring; los tests las construyen con `new`. No hay semilla ni emisor configurados en producción. `PublicAgendaConsumption` solo admite `RESERVAR` e `HISTORIA`.
- C-08 entrega creación y reprogramación atómicas con fila `ocupaciones`, consulta de disponibilidad y write-back, pero no cancelación, no búsqueda por token y no expone `huecosLibres` por HTTP. `estado`/`origen` son strings libres y `tokenPublico` siempre se graba nulo.
- C-07 entrega catálogos sin seeds; la lectura vigente no filtra `activo` ni interpreta `aplica_a_todos`, y la zona es la constante `America/Argentina/Buenos_Aires`.
- V004/V005/V006 cubren metadatos de token, catálogos y turnos/ocupaciones con exclusion constraints. `CHANGES.md` reserva Parte 3 desde **V020**; no se edita migración aplicada.
- Las 17 decisiones del PO están en `docs/decisiones/C-09-decisiones.md`. RN-GL-03 exige que los valores semánticos vengan de una decisión humana, no de convención del agente.

## Goals / Non-Goals

**Goals:**
- Exponer cinco operaciones HTTP públicas que consuman el contrato de C-06 sin modificar `publictoken/`.
- Garantizar que toda escritura pública pase por el camino que crea/desactiva `ocupaciones`, con idempotencia y sin solapamiento.
- Hacer efectiva la ventana de 48 hs hábiles y el ciclo `reservado → confirmado → cancelado` con write-back observable.
- Emitir eventos de auditoría con actor `PACIENTE` y metadata sanitizada, dejando la notificación real para C-10.

**Non-Goals:**
- UI, QR o mockups: **Decision: no (no UI)**; el cambio es superficie HTTP y no dispara la política `design-mockups`.
- Emisión/reemisión de enlaces, protección RBAC de agenda, adaptador `AuthenticatedActor` de C-05 y despliegue ejecutable (C-17/C-18).
- Cambiar catálogos C-07, agregar seeds, fijar anticipación mínima/máxima, integrar WhatsApp o Mercado Pago, crear waitlist o nueva migración.

## Decisions

Las decisiones D1–D17 se aplican tal como fueron aprobadas por el PO. Para cada una se registra la alternativa descartada:

| # | Decisión aplicada | Alternativa descartada y motivo |
|---|---|---|
| D1 | `/publico/v1/turnos`, `GET`/`POST`, token en query `token`, errores `{"error":...}` con 400/409/410/404 | Reutilizar `POST /turnos`: exigiría tocar controladores `@Profile({"dev","test"})` y debilitar guardias de C-04/C-06 |
| D2 | `RESERVAR` para disponibilidad/crear; `GESTIONAR` para confirmar/cancelar/reprogramar con `turnoId` del claim; `GESTIONAR` entregado una vez al crear | Crear scope `CONFIRMAR` o endpoint emisor público: cambiaría el contrato cerrado de C-06 y ampliaría la superficie de ataque |
| D3 | `RESERVAR` se consume tras creación exitosa; `GESTIONAR` vigente hasta `exp`, sin rotación automática | Rotar en cada operación: invalidaría el enlace en manos del paciente y rompería el reenvío idempotente |
| D4 | Conflicto atómico con hasta 3 alternativas del mismo profesional/tratamiento | Waitlist automática: diferida a v0.2 por C-01 (Gap C-19) |
| D5 | 2 días hábiles completos en zona acordada; vencimiento `23:59:59` del segundo día hábil anterior; dentro si `now >= vencimiento` y `now < inicio` | 48 horas corridas: contradice C-01 C2, que fija días hábiles completos |
| D6 | `reservado`/`confirmado`/`cancelado`, `origen=link`; cancelar = estado + `activo=false` + `Ocupacion.desactivar()` | Enum en BD o borrado físico: V006 no define enum y el borrado rompería la trazabilidad append-only |
| D7 | `idempotency_key` en cuerpo; operaciones `turno.crear/confirmar/cancelar/reprogramar`; repetición → `409 operacion_duplicada`; sin payload ni TTL | Guardar la respuesta y TTL: requeriría columnas nuevas y expondría la primera respuesta a un reenvío no autenticado |
| D8 | Reutilizar `turno.creado`/`turno.reprogramado`; agregar `turno.confirmado`/`turno.cancelado`; actor `PACIENTE`; metadata acotada; WhatsApp como puerto NO-OP | Tipo de auditoría para cada notificación: afirmaría entrega sin outbox de C-10 |
| D9 | Sin migración; V004/V005/V006 bastan; cualquier necesidad futura → V020+ | Agregar columna a `turnos`: `token_publico` ya existe y admite el `jti` (≤64 chars) |
| D10 | `pacienteId`/`turnoId` String del claim → `Long`; paciente debe existir; `sub` solo informativo | Crear paciente desde el token: recolectaría datos personales en superficie pública sin consentimiento verificado |
| D11 | Consumir `DisponibilidadService` y catálogo C-07 tal como están; anticipación mín/máx fuera de C-09 | Filtrar `activo`/`aplica_a_todos`: exigiría delta `MODIFIED` en `agenda` y reabrir tests de C-07/C-08 |
| D12 | Governance MEDIO; checkpoint 1 (contrato/wiring/scopes/idempotencia) y checkpoint 2 (ventana/cancelación/eventos) antes del apply | Un único checkpoint final: dejaría pasar errores de contrato hasta el write-back |
| D13 | Beans de C-06 en `com.turnosodontologia.publicbooking`; `public.token.issuer`, `public.surface.prefixes=/publico/v1`, nombre de `PUBLIC_TOKEN_SECRET` sin valor | Modificar clases de `publictoken`: congelar el change C-06 ya verificado (157 tests) |
| D14 | Toda escritura por servicios que insertan/desactivan `ocupaciones`; prohibido insert directo en `turnos`/`bloqueos` | Reforzar BD con constraint nueva (V020+): migración innecesaria si el camino de servicio es obligatorio |
| D15 | No tocar validación de C-06: firma, `iss`, `exp` y disponibilidad de `jti`; no comparar `token_hash` por operación | Comparar hash en cada validación: la firma HMAC ya cubre claims; obligaría a cambiar API y tests de C-06 |
| D16 | `turnos.token_publico` = `jti` del `GESTIONAR`; agregar `findByTokenPublico` | Guardar el token completo o su hash: C-06 prohíbe texto plano y el `jti` basta para localizar el turno |
| D17 | E2E en perfil `test` con `InMemoryAuditEventRecorder`; fuera de test, `JdbcAuditEventPublisher` persiste | Verificar solo contra `auditoria_log` en test: contradice `AuditPersistenceConfig` y no es reproducible |

Nota de governance: el nombre de `PUBLIC_TOKEN_SECRET` se documenta sin valor. Asignar el valor real en cualquier entorno es una acción HIGH que requiere revisión humana y no forma parte de este change.

## Risks / Trade-offs

- **[C-06 sin wiring ni secreto] →** crear beans en el paquete nuevo y dejar el valor del secreto fuera del repo; verificado en el checkpoint 1.
- **[`public.surface.prefixes` no configurado] →** fijar `/publico/v1` en dev/test y afirmar los headers en tests de éxito y rechazo; sin esto la spec `public-access-tokens` se incumple.
- **[`PublicAgendaConsumption` excluye `GESTIONAR`] →** nueva guardia de scopes en `publicbooking` sin editar C-06; test de scope incorrecto y de `turnoId` ajeno.
- **[Anti-solapamiento cruzado depende de `ocupaciones`] →** D14; tests de carrera y de turno↔bloqueo contra PostgreSQL real.
- **[Catálogo sin seeds y lectura permisiva] →** documentar que feriados/horarios dependen de administración y que `activo`/`aplica_a_todos` se ignoran según C-07; no presentar la disponibilidad como clínicamente autorizada.
- **[Cálculo DST en disponibilidad] →** consumir el servicio vigente en zona `America/Argentina/Buenos_Aires`; si se decide corregir `fecha.atTime(...)`, hacerlo como delta de `agenda`, no aquí.
- **[Idempotencia sin replay ni TTL] →** respuesta `409` y registro persistente; documentado en la spec y en el contrato HTTP.
- **[`GESTIONAR` vigente 7 días sin rotación] →** no incluir PII en el enlace, sanitización de logs y control del checkpoint 2.
- **[Fuera de ventana sin liberación automática] →** derivación a recepción con `derivarARecepcion=true`; el hueco sigue ocupado a propósito (C-01 A4 aplica al reembolso, no a este caso).
- **[Auditoría dependiente de perfil] →** E2E usa recorder en memoria; no afirmar retención, respaldo ni evidencia de cumplimiento (RN-SE-02/03).
- **[Sin aplicación ejecutable en `src/main`] →** verificación con contexto Spring de test; arranque real queda para C-17/C-18.

## Migration Plan

No hay cambios de esquema en C-09. Se usan V004 (metadata de token e idempotencia), V005 (catálogos) y V006 (turnos/ocupaciones); `turnos.token_publico` ya existe y no se edita. No se ejecuta `flyway repair` ni se modifica migración aplicada. Si durante el apply aparece una necesidad real de esquema, se crea **V020 o superior** según la reserva de Parte 3 de `CHANGES.md`, con aprobación humana previa. Rollback: revertir código y propiedades; no hay datos de esquema que deshacer.

## Open Questions

Preguntas diferibles que no cambian specs, enfoque ni tareas de C-09:

- ¿Qué proceso administra la carga de feriados nacionales en producción y con qué periodicidad? (operación de C-07; la ventana solo omite los feriados presentes en catálogo).
- ¿Qué plantilla y proveedor WhatsApp se usarán en C-10? (el puerto de este change solo registra una solicitud simulada).
- ¿Cómo se demostrará la retención/exportación exigida por RN-SE-02/03 en el change de cumplimiento correspondiente? (C-09 no acredita esas capacidades).
