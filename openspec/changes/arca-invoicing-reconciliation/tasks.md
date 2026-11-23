# Tasks

Referencias: `docs/decisiones/C-16-decisiones.md` (AR-1..AR-14), `proposal.md`, `specs/arca-invoicing/spec.md`, `specs/arca-reconciliation/spec.md`, deltas de auditoría y `design.md`. Ninguna tarea autoriza código sin la aprobación humana exigida por la governance CRITICO.

> **Notas de apply (2026-11-23)**: (1) la unicidad de `comprobantes` se implementó como `UNIQUE (senia_id, tipo)` y no como `senia_id` absoluto, para permitir la nota de crédito sobre la misma seña reembolsada (AR-1, AR-11); queda documentado en `V022__arca_invoicing.sql`. (2) Los guards estructurales `CurrentActorProviderTest`, `AgendaSurfaceTest` y `AuthMigrationV002Test` se ajustaron de forma puntual para las cabeceras salientes a ARCA (AR-7) y para incluir V022 en la lista base de migraciones; no se modificaron V001–V021.

## 1. Esquema y persistencia (V022)

- [x] 1.1 Crear `V022__arca_invoicing.sql` con la tabla `comprobantes` (`senia_id` único, `presupuesto_id` y `turno_id` opcionales sin FK física, `tipo`, `estado`, `moneda` fija `ARS`, `monto NUMERIC(12,2)`, `concepto`, `cae`, `fecha_emision`, `intentos`, `ultimo_error`, `conciliado` y auditoría), verificando con `Migration022Test` sobre `PostgresFlywayTest` que V022 aplica limpia, V001–V021 quedan intactas y no existen tablas de caja, asiento ni arqueo (AR-2, AR-3, AR-14).
- [x] 1.2 Implementar la entidad `Comprobante` y su repositorio sobre `AuditableEntity` con borrado lógico, verificando con tests de persistencia que un `senia_id` duplicado se rechaza, las referencias opcionales sin FK se conservan y la lectura por `senia_id` devuelve el comprobante único (AR-2).

## 2. Emisión sobre seña aprobada e integración REST

- [x] 2.1 Implementar el servicio de emisión que consume `senia_id`, hereda `presupuesto_id`/`turno_id`, factura solo el importe cobrado en `ARS` con letra `C` configurable e IVA no discriminado, verificando con tests que una seña `aprobado` con y sin presupuesto genera una sola factura, que una seña no aprobada se rechaza sin efectos y que no se factura el saldo del presupuesto (AR-1, AR-4, AR-5).
- [x] 2.2 Definir `FacturacionPort` y su adaptador REST con `RestTemplate`, `ARCA_BASE_URL`, `ARCA_ACCESS_TOKEN` y parámetros no secretos de letra/punto de venta, verificando con tests el payload y las cabeceras enviadas y que `pom.xml` no incorpora SDK ni dependencia nueva (AR-7).
- [x] 2.3 Registrar el adaptador NO-OP/simulado en `dev`/`test` con CAE determinista de prueba y defaults seguros para `ARCA_BASE_URL`/`ARCA_ACCESS_TOKEN`, verificando con tests que el contexto arranca sin secretos reales, que el CAE queda marcado como prueba y que no se realiza ninguna llamada de red (AR-7, AR-8, AR-14).
- [x] 2.4 Reutilizar los datos existentes de `pacientes` sin crear campos fiscales ni superficie pública, verificando con tests que una emisión con dato faltante queda `pendiente` y solicita aviso a recepción por el puerto NO-OP, sin exponer DNI, documento ni dirección (AR-6).

## 3. Estados, idempotencia y reintentos

- [x] 3.1 Implementar `tipo` (`factura`/`nota_credito`) y `estado` (`pendiente`/`emitido`/`rechazado`/`anulado`) con transiciones acotadas, verificando con tests la emisión, el rechazo, el reintento desde `rechazado`, la anulación por nota de crédito y el rechazo de transiciones inválidas sin cambios ni auditoría (AR-9).
- [x] 3.2 Registrar `factura.emitir` en `public_idempotencia` (V004) con clave SHA-256 de `senia.id` en la misma transacción que el cambio de estado y el evento, verificando con tests que el reenvío no crea otro comprobante y que un fallo de auditoría revierte clave, comprobante y estado (AR-9).
- [x] 3.3 Implementar el reintento manual con incremento de `intentos` y `ultimo_error` saneado, verificando con tests que se reutiliza la clave, que credenciales o tokens del error no aparecen en `ultimo_error`, logs ni metadata y que no existe reintento automático (AR-9, AR-14).

## 4. Conciliación comprobante–seña

- [x] 4.1 Implementar la conciliación que exige CAE, igualdad de montos y seña `aprobado` y `conciliado`, verificando con tests los cuatro escenarios: conciliada con `factura.conciliada`, seña no conciliada sin evento, montos divergentes informados y comprobante no emitido sin flag (AR-10).
- [x] 4.2 Garantizar que la conciliación solo actualice el flag del comprobante, verificando con tests que la seña conserva su estado, importe y `conciliado`, que una referencia inexistente no crea entidades y que el esquema sigue sin caja, asiento ni arqueo (AR-3, AR-10).

## 5. Nota de crédito

- [x] 5.1 Implementar la nota de crédito manual de recepción que valida `senia.estado = reembolsado`, revierte el total y anula la factura, verificando con tests la emisión válida con `nota_credito.emitida`, el rechazo sobre seña no reembolsada y la ausencia de disparo automático al finalizar el reembolso (AR-11).
- [x] 5.2 Registrar la operación `nota_credito.emitir` con clave derivada del `comprobante.id`, verificando con tests que un reenvío no genera una segunda nota y que la factura no se anula dos veces (AR-9, AR-11).

## 6. Auditoría aprobada por el PO

- [x] 6.1 Ampliar `AuditoriaCatalogo` con `factura.emitida`, `factura.rechazada`, `factura.conciliada`, `factura.anulada` y `nota_credito.emitida`, verificando con un test por escenario que se persiste una fila por operación confirmada con entidad `comprobante`, actores `recepcionista` (`SIMULADO`) y `system` (`SYSTEM`), y que reenvíos y transiciones inválidas no agregan filas (AR-12).
- [x] 6.2 Construir la metadata acotada a `comprobanteId`, `seniaId`, `presupuestoId`/`turnoId`, `monto`, `moneda`, `tipo`, `estado`, `motivo` y `correlationId`, verificando con tests que no aparecen CAE, DNI, dirección, credenciales ni secretos y que cada operación tiene un `correlationId` distinto (AR-12, AR-14).

## 7. Superficie y wiring

- [x] 7.1 Exponer `POST /comprobantes`, `POST /comprobantes/{id}/reintentar`, `POST /comprobantes/{id}/nota-credito` y `GET /comprobantes/{id}` bajo `@Profile({"dev","test"})`, verificando con tests MVC que funcionan sin credenciales de staff, no crean RBAC nuevo y no modifican la cadena `/auth/**` (AR-13).
- [x] 7.2 Registrar el paquete `com.turnosodontologia.invoicing` en `TestJpaConfig`, verificando que el contexto de perfil `test` inicia con los defaults seguros y que la suite existente de C-14 y C-15 sigue verde (AR-13, AR-14).

## 8. Verificación final del change

- [x] 8.1 Ejecutar la suite completa sobre Testcontainers/PostgreSQL desde la línea base de 287 tests verdes, verificando idempotencia, conciliación, reembolso/nota de crédito, auditoría, `Migration022Test` y ausencia de secretos en logs y metadata (AR-14).
- [x] 8.2 Ejecutar `openspec validate arca-invoicing-reconciliation --strict` y `openspec status --change arca-invoicing-reconciliation`, verificando cero errores y los cuatro artefactos completos (AR-14).
- [x] 8.3 Revisar que `CHANGES.md` siga sin marcar C-16 hasta la verificación técnica y que los diferidos del acta y de `design.md` no tengan tareas en este change, dejando constancia de la aprobación humana requerida antes del apply (governance CRITICO).
