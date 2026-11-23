# Tasks

## 1. Esquema y persistencia (V021)

- [x] 1.1 Crear `V021__mercadopago_deposits.sql` con la tabla `senias` (`turno_id` y `presupuesto_id` opcionales, `monto NUMERIC(12,2)`, `moneda` fija `ARS`, `estado`, `proveedor`, `external_reference`, `idempotency_key`, `fecha_aprobacion`, `conciliado`), checks y unicidad; verificado con `Migration021Test` sobre `PostgresFlywayTest`: V021 aplica limpia, V001–V020 quedan intactas y no existen tablas de caja, asiento ni arqueo.
- [x] 1.2 Implementar la entidad `Seña` y su repositorio sobre `AuditableEntity` con borrado lógico y FKs opcionales; verificado con tests de persistencia que cubren seña con y sin presupuesto, referencia duplicada rechazada y lectura por `external_reference`.

## 2. Solicitud de seña e integración REST

- [x] 2.1 Definir el puerto de cobro y su adaptador HTTP con `RestTemplate`, `APP_BASE_URL`, `POST /v1/preferences`, `external_reference = "senia-" + UUID` y `X-Idempotency-Key`; verificado con tests que comprueban el payload, la clave enviada y que no se agrega ninguna dependencia al `pom.xml`.
- [x] 2.2 Exponer `POST /senias` bajo `@Profile({"dev","test"})`, sin credenciales de staff ni RBAC nuevo, reutilizando el contrato de `Presupuesto` de C-14; verificado con tests WebMvc de éxito con presupuesto, sin presupuesto y de reenvío con la misma clave sin segunda seña.
- [x] 2.3 Documentar el uso exclusivo de `MERCADOPAGO_ACCESS_TOKEN`, `MERCADOPAGO_WEBHOOK_SECRET` y `APP_BASE_URL` según `.env.example`, sin valores reales; verificado con test que lee la configuración del perfil de test y con revisión de que no se versionan secretos.

## 3. Webhook: firma e idempotencia

- [x] 3.1 Implementar la verificación HMAC-SHA256 local de `x-signature` (`ts`/`v1`) con `x-request-id` y `MERCADOPAGO_WEBHOOK_SECRET`, sin reconsulta a la API; verificado con tests unitarios de firma válida, ausente, malformada, inválida y de que el secreto no aparece en respuestas, logs ni metadata.
- [x] 3.2 Exponer `POST /publico/v1/webhooks/mercadopago` sin token público de C-06, respondiendo `400` sin efectos ante firma inválida; verificado con tests WebMvc que comprueban que la seña sigue `pendiente` y que no se inserta fila de auditoría.
- [x] 3.3 Registrar la deduplicación en `public_idempotencia` con operación `pago.webhook`, clave SHA-256 de `x-request-id` (o `external_reference`+`ts`) y en la misma transacción que el cambio de estado; verificado con Testcontainers/PostgreSQL: reenvío duplicado responde `200` sin segundo efecto ni segunda fila, y una reversión no deja clave registrada.

## 4. Estados y conciliación

- [x] 4.1 Implementar el mapping de estados del proveedor a `pendiente`/`aprobado`/`rechazado`/`cancelado`/`reembolsado`, transiciones solo desde `pendiente`, descarte de eventos tardíos y `fecha_aprobacion` como instante observado; verificado con tests parametrizados de los seis estados origen y de una transición inválida sobre estado terminal.
- [x] 4.2 Implementar la conciliación que marca `conciliado` solo si `turno_id` o `presupuesto_id` resuelve a una entidad existente, con evento `pago.conciliado`; verificado con Testcontainers/PostgreSQL para referencia válida, referencia inexistente (sin crear entidades) y ausencia de tablas de caja/arqueo.

## 5. Reembolso y aviso al paciente

- [x] 5.1 Exponer `POST /senias/{id}/reembolsar` para recepción, ejecutar el reembolso total contra la API de Mercado Pago solo para seña `aprobado` y registrar `reembolsado`; verificado con tests de reembolso exitoso, rechazo para seña `rechazado` y de que no se modifica C-09 ni la ventana de 48 hs.
- [x] 5.2 Agregar el puerto de notificación con implementación NO-OP/simulada para avisar al paciente sin envío real; verificado con test que registra la solicitud de aviso al rechazar el pago y con revisión de que no se crea tipo de auditoría de notificación.

## 6. Auditoría de dinero

- [x] 6.1 Ampliar `AuditoriaCatalogo` con `pago.solicitado`, `pago.aprobado`, `pago.rechazado`, `pago.reembolsado` y `pago.conciliado`, respetando el acta de C-05 y el acta `C-15-decisiones.md`; verificado con test de catálogo que acepta los cinco tipos y rechaza cualquier otro, y con un test por escenario de los eventos aprobados.
- [x] 6.2 Emitir cada evento con entidad `senia`, actores `SYSTEM`/`SIMULADO`, `correlationId` propio y metadata acotada (`seniaId`, `turnoId`, `presupuestoId`, `monto` como texto, `moneda`, `estado`, `externalReference`, `motivo`); verificado con Testcontainers/PostgreSQL: una fila por operación confirmada, ninguna por firma inválida o webhook duplicado, y rechazo de claves sensibles por `AuditoriaMetadataValidator`.

## 7. Verificación integral

- [x] 7.1 Ejecutar `mvn test` desde la raíz y confirmar que la línea base de 241 tests de C-14 sigue verde más los nuevos tests de seña, webhook duplicado, pago rechazado, conciliación, reembolso y auditoría; verificado con salida del comando sin fallos ni tests omitidos.
- [x] 7.2 Ejecutar `openspec validate mercadopago-deposits-reconciliation --strict` y revisar proposal, specs, design y tasks contra MP-1..MP-12, la enmienda C-01 A5 y la exclusión de caja/arqueo; verificado con salida sin errores y con la confirmación de que no se reprodujo la redacción supersedida.
- [x] 7.3 Confirmar que V021 es la única migración nueva de este change y que no se editaron V001–V020; verificado con la revisión del árbol de `src/main/resources/db/migration/` y con `Migration021Test`.
