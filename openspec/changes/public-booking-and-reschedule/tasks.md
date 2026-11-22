# Tasks

## 1. Contrato público y cableado (Checkpoint 1)

- [ ] 1.1 Crear el paquete `com.turnosodontologia.publicbooking` con los beans de C-06 (firmador, servicio de tokens, store de metadatos e idempotencia), `public.token.issuer` y el nombre de `PUBLIC_TOKEN_SECRET` sin valor en `.env.example`; verificar que el contexto de test levanta sin constantes de prueba y que `SecretsInventoryTest` sigue verde.
- [ ] 1.2 Configurar `public.surface.prefixes=/publico/v1` en dev/test; verificar con un test de Web MVC que las respuestas de éxito y de rechazo incluyen `Referrer-Policy: no-referrer` y `Cache-Control: no-store`.
- [ ] 1.3 Exponer `GET /publico/v1/turnos/disponibilidad` y `POST /publico/v1/turnos`, `/confirmar`, `/cancelar`, `/reprogramar` con token en query y errores `{"error":"<motivo>"}`; verificar tests sin credenciales de staff y que `/turnos`, `/agenda` y `/auth/**` permanecen intactos.
- [ ] 1.4 Implementar la guardia de scopes (`RESERVAR` para disponibilidad/crear, `GESTIONAR` para gestionar) y la comparación `turnoId` del claim contra la ruta; verificar tests de scope inválido y turno ajeno, y que `PublicAgendaConsumption` no se modifica.
- [ ] 1.5 Integrar `PublicIdempotencyStore` en las cuatro operaciones con los nombres `turno.crear`, `turno.confirmar`, `turno.cancelar` y `turno.reprogramar`; verificar `PublicIdempotenciaPostgresTest` y un nuevo test de reenvío que devuelve `409 operacion_duplicada` sin segundo efecto.
- [ ] 1.6 Checkpoint MEDIO 1: el PO aprueba contrato HTTP, wiring, scopes e idempotencia; verificar que la aprobación queda registrada en el acta del change antes de continuar con escrituras.

## 2. Disponibilidad y creación con write-back

- [ ] 2.1 Exponer la consulta de disponibilidad reutilizando `DisponibilidadService` y `ConfiguracionSchedulingService` sin modificarlos; verificar tests de huecos libres, feriado/bloqueo, zona `America/Argentina/Buenos_Aires` y que la consulta no consume el token.
- [ ] 2.2 Implementar la creación con paciente existente, `estado=reservado`, `origen=link`, `jti` del token de gestión y ocupación en una sola transacción; verificar test de integración contra PostgreSQL y evento `turno.creado`.
- [ ] 2.3 Consumir `RESERVAR` solo tras creación exitosa y devolver el token `GESTIONAR` una sola vez en la respuesta; verificar tests de reutilización en consultas y de rechazo del token ya usado.
- [ ] 2.4 Implementar el rechazo atómico por hueco tomado con hasta 3 alternativas del mismo profesional y tratamiento; verificar test de carrera con barrera concurrente y que la rama perdedora no deja turno, ocupación ni evento.

## 3. Ventana de 48 hs hábiles y gestión del turno

- [ ] 3.1 Implementar el cálculo de 2 días hábiles completos (zona acordada, fines de semana, feriados del catálogo, vencimiento `23:59:59` del segundo día hábil anterior); verificar tests unitarios de límites inclusive, feriado y fin de semana.
- [ ] 3.2 Implementar la confirmación con `GESTIONAR` dentro de ventana, sin mover el hueco; verificar test de transición a `confirmado` y emisión de `turno.confirmado`.
- [ ] 3.3 Implementar la cancelación dentro de ventana con `estado=cancelado`, `activo=false` y `Ocupacion.desactivar()` en la misma transacción; verificar test de integración y que la disponibilidad vuelve a ofrecer el intervalo.
- [ ] 3.4 Implementar el rechazo fuera de ventana con `409` y `derivarARecepcion=true`, sin liberar el hueco ni cambiar estado; verificar test de estado inalterado y sin evento.
- [ ] 3.5 Implementar la reprogramación pública dentro de ventana conservando `estado` y `origen`, con liberación y ocupación atómicas; verificar tests de destino libre, destino ocupado con alternativas y evento `turno.reprogramado`.

## 4. Auditoría, notificación y verificación final

- [ ] 4.1 Ampliar el catálogo de auditoría con `turno.confirmado` y `turno.cancelado`, usar actor modo `PACIENTE` y limitar la metadata a las claves permitidas; verificar tests de catálogo, metadata y sanitización del token.
- [ ] 4.2 Agregar el puerto de notificación con implementación simulada en dev/test; verificar test que no invoca WhatsApp ni registra evento de entrega.
- [ ] 4.3 Checkpoint MEDIO 2: el PO aprueba write-back, ventana, cancelación y eventos; verificar que la aprobación queda registrada antes del cierre del change.
- [ ] 4.4 Ejecutar los E2E de Flujo 1 y 2, carrera por hueco, límite de ventana, reenvío duplicado y la suite completa con `mvn test`; verificar la línea base de 157 tests más los nuevos en verde y sin regresión en las guardias de C-04/C-06.
- [ ] 4.5 Validar el change con `openspec validate public-booking-and-reschedule --strict`; verificar salida sin errores y confirmar que no se editó ninguna migración aplicada ni se creó una por debajo de V020.

## Workflow follow-up

- Marcar `[x]` en `CHANGES.md` para C-09 solo después de la verificación técnica de este change.
- Solicitar la aprobación del PO para archivar el change y actualizar los specs principales.
