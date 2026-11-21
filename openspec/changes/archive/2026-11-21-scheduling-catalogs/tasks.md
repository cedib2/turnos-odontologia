# Tasks

## 1. Setup y alineación (sin migraciones/seeds, dev/test)
- [x] 1.1 Confirmar que no se crean ni editan archivos en `src/main/resources/db/migration` (solo se usan V001–V005) y que `Migration005Test.java:38` sigue intacto (`COUNT(tratamientos)==0`); verificar con `./mvnw -q -Dtest=Migration005Test test` en entorno con Docker disponible.
- [x] 1.2 Revisar patrón de `BloqueoController` (`@RestController`, `@Profile({"dev","test"})`) y modelos de request/response existentes para replicar estilo en nuevos controladores (sin auth/RBAC/headers nuevos).

## 2. Tratamientos (CRUD HTTP indicativo `/tratamientos`)
- [x] 2.1 Escribir/ajustar tests que cubran escenarios delta de agenda: en perfil dev/test se puede crear/listar/consultar/actualizar/eliminar vía API de tratamientos con validaciones básicas (código único, `duracionMinutos > 0`, campos requeridos). Tests unitarios con H2 donde sea posible y/o tests de integración con Testcontainers si se necesita verificar unicidad/constraints reales; ejecutar `./mvnw -q test` y ver verde después de implementar (estricto TDD: primero rojo para el caso nuevo definido, luego implementación).
- [x] 2.2 Implementar controlador y DTOs para `/tratamientos` (dev/test), delegando en repositorios o servicio mínimo sin tocar `ConfiguracionSchedulingService`; manejar errores de validación/duplicado de forma consistente con el proyecto. Verificar con los tests de 2.1 y una corrida `./mvnw -q test`.

## 3. Horarios de atención (CRUD HTTP indicativo `/horarios`)
- [x] 3.1 Escribir tests para: crear/listar/consultar/actualizar/eliminar horarios con `profesionalId` opcional, `diaSemana` 1..7, `desde < hasta`; validar que se rechazan entradas inválidas (día fuera de rango, rango invertido). Cubrir con H2 unitario y, si aplica, integración Testcontainers para persistencia; ejecutar `./mvnw -q test` hasta verde (TDD).
- [x] 3.2 Implementar controlador/DTOs para `/horarios` (dev/test) usando `HorarioAtencion`/repositorio; sin reglas extra de solapamiento ni calendario `(TBD)`. Verificar con tests de 3.1.

## 4. Feriados (CRUD HTTP indicativo `/feriados`)
- [x] 4.1 Escribir tests para: crear/listar/consultar/actualizar/eliminar feriados con `fecha` única y `descripcion` obligatoria; verificar rechazo por duplicado en integración PostgreSQL si corresponde. Ejecutar `./mvnw -q test` hasta verde (TDD).
- [x] 4.2 Implementar controlador/DTOs para `/feriados` (dev/test) usando `Feriado`/repositorio; `aplica_a_todos` se expone como campo persistido sin interpretación adicional en este change (no cambiar `ConfiguracionSchedulingService.esFeriado`). Verificar con tests de 4.1.

## 5. Verificación final del delta
- [x] 5.1 Ejecutar `./mvnw -q test` completo y confirmar que no hay regresiones en suites de C-08 (agenda, disponibilidad, migración). Documentar resultado en el resumen del apply (sin cambiar código de producción fuera de lo previsto).
- [x] 5.2 Ejecutar `openspec validate scheduling-catalogs --strict` y corregir cualquier error de formato/escenarios hasta pasar limpio.

## Workflow follow-up

- Aplicar este change con `openspec-apply` (o el flujo indicado por el proyecto) respetando TDD y marcando checkboxes solo con evidencia de ejecución.
- Archivar el change después de revisión y verificación del equipo (no ejecutar archive en este paso de proposal).