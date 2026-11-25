# Arquitectura Propuesta

> Propuesta **agnóstica de stack**. La entrevista no impone tecnología y el presupuesto es acotado; por eso no se prescribe lenguaje, framework ni base de datos. Todo `(TBD)` debe resolverse antes de fijar dependencias.

## Patrones aplicados

| Patrón | Dónde se usa | Por qué |
|---|---|---|
| Separación por dominio (Agenda, Clínico, Financiero, Notificaciones, Identidad, Auditoría) | Capa de aplicación y dominio | Los requisitos del MVP son claramente delimitados por dominio (discovery §5) |
| Restricción de integridad en base de datos | Turnos, bloqueos y sobreturnos | RN-AG-01: anti-solapamiento debe sobrevivir a escrituras concurrentes |
| *Write-back* real desde reserva pública | Flujos 1 y 2 | Evita el problema de «reservas dobles o pisadas» (discovery §1) |
| *Outbox* + webhooks idempotentes | WhatsApp y Mercado Pago | Notificaciones y pagos son asíncronos; RN-GL-02 |
| Log append-only | Auditoría y firmas | Cumplimiento demostrable (RN-SE-02, RN-SE-04) |
| Tokens públicos con expiración | Link/QR y gestión del turno | Superficie pública sin cuenta de usuario; deadline `(TBD)` |
| RBAC | Panel interno | Roles del discovery §2; detalle en [03_actores_y_roles.md](03_actores_y_roles.md) |
| Exportación/respaldo programados | Cumplimiento | Copia en 48 h y retención 10 años propuestos por el informe `(TBD)` |
| Soft delete / versionado | Historia clínica | No sobrescribir evoluciones (RN-CL-02); borrado físico `(TBD)` |

## Estructura de directorios

Estructura **conceptual**; se adapta al stack que se decida `(TBD)`:

```
turnos-odontologia/
├── knowledge-base/          # esta KB (fuente de verdad documental)
├── docs/discovery/          # entrevista e informe de mercado (solo lectura)
├── src/
│   ├── domain/              # agenda, clinico, financiero, identidad, auditoria
│   ├── application/         # casos de uso (reservar, cancelar, firmar, cobrar)
│   ├── infrastructure/      # persistencia, WhatsApp, Mercado Pago, ARCA (TBD)
│   └── interfaces/          # link/QR público, panel interno, webhooks
├── tests/
└── (sin openspec/ ni .active-orchestrator-state.json modificados por esta fase)
```

> Nota: no se crean archivos de código en esta fase; la estructura es una guía para la fase de implementación.

## Seguridad

- **Autenticación**: staff `(TBD)`; paciente mediante token de enlace/QR con vencimiento `(TBD)`; verificación de identidad para acceso a historia `(TBD)`.
- **Autorización**: RBAC con matriz de [03_actores_y_roles.md](03_actores_y_roles.md); herencia y ámbitos `(TBD)`.
- **Validación de input**: en toda entrada pública; tokens, fechas, duraciones, montos y archivos `(TBD: librería/estrategia)`.
- **Gestión de secretos**: variables de entorno o gestor local `(TBD)`; **nunca** credenciales en repositorio.
- **Protección de datos**: cifrado en tránsito y en reposo; ubicación de datos y subprocessores `(TBD)`; Ley 25.326, Ley 26.529, Decreto 1089/2012 y cláusulas AAIP 198/2023 (RN-SE-04, RN-SE-05).
- **Auditoría**: inmutable, con actor, fecha, entidad, IP/dispositivo `(TBD)`; exportación y respaldo `(TBD: frecuencia/formato)`.
- **Webhooks**: verificación de firma de WhatsApp y Mercado Pago `(TBD)` + idempotencia (RN-GL-02).
- **Cumplimiento demostrable**: dossier descargable con DPA responsable/encargado, registro AAIP/RNBD del responsable, cláusulas de subprocessores, logs, exportación y retención. La sola declaración de Ley 25.326 ya no diferencia (corrección del informe).

## Variables de entorno

Nombres **propuestos**, no confirmados; valores de ejemplo son marcadores, nunca secretos reales.

| Variable | Descripción | Ejemplo | Sensible |
|---|---|---|---|
| `APP_BASE_URL` | URL pública base para links/QR | `https://turnos.(TBD)` | N |
| `DATABASE_URL` | Cadena de conexión a persistencia | `(TBD)` | S |
| `TIMEZONE` | Zona horaria del consultorio | `America/Argentina/Buenos_Aires` `(TBD)` | N |
| `CANCELLATION_WINDOW_HOURS` | Ventana de cancelación en hs hábiles | `48` | N |
| `WHATSAPP_ACCESS_TOKEN` | Token de WhatsApp Business API | `(TBD)` | S |
| `WHATSAPP_PHONE_NUMBER_ID` | Identificador de número | `(TBD)` | S |
| `WHATSAPP_WEBHOOK_VERIFY_TOKEN` | Verificación de webhook | `(TBD)` | S |
| `MERCADOPAGO_ACCESS_TOKEN` | Credencial de Mercado Pago | `(TBD)` | S |
| `MERCADOPAGO_WEBHOOK_SECRET` | Firma de webhook de pagos | `(TBD)` | S |
| `EXPORT_RETENTION_YEARS` | Retención de historia/datos | `10` `(TBD)` | N |
| `BACKUP_CRON` | Programación de respaldos | `(TBD)` | N |
| `BACKUP_ENCRYPTION_KEY` | Clave AES-256 (Base64, 32 bytes) del cifrado en reposo de respaldos y exportaciones (C-17) | `<base64-32-bytes>` | S |
| `BACKUP_DIR` | Destino local mínimo de respaldos; destino cloud AR `(TBD)` (C-17) | `(TBD)` | N |
| `RESTORE_SANDBOX_URL` / `_USER` / `_PASSWORD` | Base aislada donde se prueba cada restore; nunca la productiva (C-17) | `(TBD)` | S |
| `IA_HANDOFF_ENABLED` | Activa asistente con derivación | `(TBD)` | N |

## Restricciones operativas de esta fase

- Sin instalaciones de dependencias, sin servicios externos nuevos y sin commits.
- Sin modificaciones a `.active-orchestrator-state.json` ni a `openspec/` por parte de `kb-creator` en esta ejecución.
- Sin stack impuesto: la elección se documenta como DD-01 pendiente.

## Fuentes

- `docs/discovery/discovery.md` §§7–9.
- `docs/discovery/informe-discovery.md` «Nota preliminar», §§C.3, C.4, D.3.
