# Parte 4 — C-17, C-18, C-21 y C-22: checkpoints y decisiones

- **Autor**: Joaquín Adrián Velázquez Carrera
- **Rama**: `feat/Parte4-J-Velazquez`
- **Fecha**: 2026-11-25
- **Migraciones**: V030 (C-17) y V031 (C-18), dentro del rango reservado de la Parte 4 (V030+).
- **Verificación**: `mvn test` → 224 tests verdes (194 previos + 30 nuevos), Postgres 17 vía Testcontainers.
- **Estado de governance**: C-17 y C-18 son **ALTO** → implementados y **pendientes de revisión humana** antes de archivar. C-21 y C-22 son **BAJO** → verificados por tests; quedan `[ ]` hasta que existan sus dependencias reales (C-09/C-10/C-15), igual que C-08 con sus puertos simulados.

## Cómo se trabajó sin esperar a las Partes 2 y 3

| Dependencia | Contrato usado | Qué falta del otro lado |
|---|---|---|
| C-13 (copia de historia) | Puerto `ProveedorHistoriaClinica`; sin implementación la exportación declara `(pendiente: C-13 no implementado)` | Implementar el puerto en C-13 |
| C-15 / C-10 / C-14 (tablas nuevas) | Volcado por catálogo de PostgreSQL: toda tabla nueva entra al respaldo y a la exportación | Nada; si una tabla guarda secretos, agregarla a `ExportacionService.TABLAS_CREDENCIALES` |
| C-09 (reserva pública) | Adopción = `turnos.token_publico IS NOT NULL`; ventana = eventos `turno.reprogramado` / `turno.cancelado` con `metadata.inicioAnterior` (ISO-8601) | C-09 debe completar `token_publico` y emitir `turno.cancelado` con `inicioAnterior` (agregar el tipo al catálogo) |
| C-10 (confirmaciones) | Métrica `CONFIRMACIONES` = `NO_DISPONIBLE` | Exponer confirmaciones en el outbox/webhooks |
| C-19 (ocupación/ausentismo) | Métricas `DIFERIDA` (C-01: v0.2) | — |
| C-12 (firma) | El dossier mínimo no la requiere | — |

Único cambio en código de otra parte: `ReprogramacionService` (C-08) agrega `inicioAnterior` a la metadata de `turno.reprogramado` (aditivo, con aserción nueva en `ReprogramacionIntegrationTest`). También se ajustó `AuthMigrationV002Test` para que valide el rango V001–V009 en lugar de la lista exacta de migraciones, y `application-test.yml` limita el pool por contexto (`minimum-idle: 1`, `maximum-pool-size: 5`) para no agotar `max_connections` con los contextos nuevos.

## Decisiones técnicas tomadas (equipo técnico, sin valores de negocio)

1. **Formato de trabajo JSON-v1** (filas `to_jsonb` de PostgreSQL). El formato **oficial** de entrega sigue `(TBD, RN-SE-03)` y figura así en cada exportación y en el dossier.
2. **Cifrado en reposo AES-256-GCM** con clave `BACKUP_ENCRYPTION_KEY`. Sin clave, el respaldo/exportación **falla**: nunca se escribe en claro (RN-SE-05). Gestión de clave/KMS `(TBD, B6)`.
3. **Restore aislado con guardián**: antes de `flyway clean` se compara `base@host:puerto` del entorno aislado con la productiva; si coinciden, se rechaza. Luego se verifica checksum y se compara fila a fila.
4. **Exportación sin credenciales**: se excluyen `auth_sesiones`, `auth_tokens_refresh`, `auth_jti_revocados`, `public_token_jti`, `public_idempotencia` y `usuarios.hash_password`. El respaldo (cifrado) sí los incluye para que el restore sea completo.
5. **48 hs hábiles** = horas de lunes a viernes no feriados, zona AR (lectura literal de C-01 C2 «días hábiles completos»).
6. **Precio**: se aplica C-01 A5=2 (sin desglose de API), que supersede la frase del scope de C-21 «costos de API detallados por separado».

## `(TBD)` que siguen abiertos — no se inventaron

| Ítem | Dónde se configura | Comportamiento sin valor |
|---|---|---|
| Plazo de copia (informe: 48 h) | `cumplimiento.exportacion.plazo-horas` | Sin plazo ni vencimiento; el dossier lo marca `(TBD, IN-05)` |
| Retención (informe: 10 años) | `cumplimiento.respaldo.retencion-anios` | `retener_hasta` null; dossier `(TBD, IN-06)` |
| Frecuencia de respaldo | `BACKUP_CRON` | No hay respaldo programado |
| Destino de respaldos (cloud AR) | `BACKUP_DIR` (adaptador local mínimo) | La operación falla con «destino no configurado» |
| Canal de alerta | Bean `AlertaRespaldo` | Log de nivel ERROR |
| Owner legal / responsable de datos | Evidencia manual `OWNER_LEGAL` | Dossier no publicable |
| Monto, moneda, periodicidad del plan | `precio.plan.*` | `/precio` responde `PENDIENTE` sin cifra |
| Metas de las métricas | — | `(TBD) — baseline del piloto` |
| ¿Bloqueos cuentan como ocupación? | — | Ocupación `DIFERIDA` |

## Pendiente de revisión humana

- C-17/C-18 (ALTO): revisar este documento, la V030/V031 y los specs en `openspec/changes/{export-backup-restore,compliance-evidence-dossier}` antes de archivar.
- Designar owner legal (C-01 D) para poder publicar el dossier.

## Observaciones de revisión (2026-11-25) y cómo se atendieron

1. **`design.md` faltante** en C-18, C-21 y C-22: agregados; los cuatro changes quedan con planificación completa.
2. **`turno.cancelado`**: no se da de alta desde la Parte 4. Es responsabilidad de C-09 agregarlo a `AuditoriaCatalogo.TIPOS` junto con `inicioAnterior` en la metadata. Queda documentado en `MetricasService.EVENTOS_VENTANA` y en el design de C-22.
3. **Exportación por paciente / credenciales**: al integrar C-14/C-15, sumar las tablas del paciente a `ExportacionService.FILTROS_PACIENTE`. Al integrar C-10/C-15, sumar las tablas con secretos a `TABLAS_CREDENCIALES`. Queda documentado en el código.
4. **`.env.example`**: se agregaron `BACKUP_ENCRYPTION_KEY`, `BACKUP_DIR`, `RESTORE_SANDBOX_URL`, `RESTORE_SANDBOX_USER` y `RESTORE_SANDBOX_PASSWORD`. Además, la retención ahora también se toma de `EXPORT_RETENTION_YEARS` (ya listada) si no se define `cumplimiento.respaldo.retencion-anios`.
5. **Flakiness de conexiones en la suite completa**: con los contextos de Spring nuevos, Postgres llegaba al límite por defecto de 100 conexiones y algunos tests fallaban según el orden de ejecución. `PostgresFixture` ahora levanta el contenedor con `max_connections=300`. Verificado con dos corridas completas seguidas: 224/224.
