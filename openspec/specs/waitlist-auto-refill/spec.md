# waitlist-auto-refill Specification

## Purpose

Define el comportamiento observable de la lista de espera: alta de candidatos, ofrecimiento simulado de un hueco liberado, expiración de la oferta y adjudicación atómica de un solo candidato, sin notificación real ni gancho asíncrono.

## Requirements

### Requirement: Alta de candidato a la lista de espera

El sistema SHALL registrar una entrada de waitlist con profesional, sillón, rango horario deseado, identificador de candidato y `deposito_requerido`, conservando `senia_id` opcional sin FK física. El sistema SHALL rechazar dos entradas activas para el mismo candidato y rango y MUST NOT crear turnos ni ocupaciones en el alta.

#### Scenario: Alta válida

- **GIVEN** un candidato, un profesional, un sillón y un rango horario deseado
- **WHEN** se registra la entrada con depósito requerido
- **THEN** se crea una entrada activa sin turno ni ocupación y se emite `waitlist.creado`

#### Scenario: Entrada duplicada

- **GIVEN** una entrada activa ya registrada para el mismo candidato y rango
- **WHEN** se intenta registrar otra
- **THEN** se rechaza sin efectos y no se emite evento de auditoría

### Requirement: Ofrecimiento simulado y expiración de la oferta

El sistema SHALL ofrecer el hueco al primer candidato elegible mediante un puerto de notificación con implementación NO-OP/simulada, sin WhatsApp real, outbox ni plantillas. La oferta SHALL expirar transcurrido el plazo configurado, medido con un reloj inyectado, y SHALL pasar al siguiente candidato sin liberar un turno ya adjudicado. El plazo productivo permanece `(TBD) -> forma de obtención` (PO Parte 3 y piloto).

#### Scenario: Ofrecimiento al primer candidato

- **GIVEN** un hueco liberado y dos candidatos elegibles ordenados por antigüedad
- **WHEN** se procesa el relleno
- **THEN** se ofrece al primero, se emite `waitlist.ofrecido` y no se contacta al segundo

#### Scenario: Expiración sin pago y paso al siguiente

- **GIVEN** una oferta vencida según el reloj inyectado y un segundo candidato elegible
- **WHEN** expira la oferta
- **THEN** se emite `waitlist.expirado`, se ofrece al siguiente y no se crea ninguna seña para el candidato expirado

#### Scenario: Sin notificación real en pruebas

- **GIVEN** el perfil `test` con el adaptador NO-OP
- **WHEN** se ofrece un hueco
- **THEN** no se realiza ninguna llamada de red ni se requiere plantilla o credencial de WhatsApp

### Requirement: Adjudicación atómica del primer candidato elegible

El sistema SHALL adjudicar el hueco en una sola transacción: verificar disponibilidad, registrar la operación `waitlist.adjudicar` en `public_idempotencia` con clave derivada del turno liberado, crear la ocupación por el camino de servicio existente, solicitar el depósito cuando corresponda y emitir los eventos de auditoría. Un reenvío, un pago fuera de plazo o una doble candidatura MUST NOT producir una segunda adjudicación ni dos señas.

#### Scenario: Adjudicación confirmada

- **GIVEN** una oferta aceptada dentro del plazo y un hueco aún libre
- **WHEN** se confirma la adjudicación
- **THEN** se crea una sola ocupación activa, una sola seña si el depósito es requerido y se emite `waitlist.adjudicado`

#### Scenario: Reenvío de la adjudicación

- **GIVEN** una adjudicación ya registrada
- **WHEN** se repite la misma operación con la misma clave
- **THEN** no se crea otra ocupación, otra seña ni otro evento

#### Scenario: Reversión atómica

- **GIVEN** una adjudicación cuya emisión de auditoría falla
- **WHEN** se confirma la transacción
- **THEN** se revierten ocupación, seña, clave de idempotencia y eventos, y el hueco queda libre para el siguiente candidato

### Requirement: Superficie indicativa de waitlist en dev/test

El sistema SHALL exponer el alta de entradas, la consulta por candidato y la ejecución del relleno como rutas HTTP indicativas solo en los perfiles `dev` y `test`, sin token público de C-06, sin credenciales de staff y sin RBAC nuevo.

#### Scenario: Rutas disponibles en prueba

- **GIVEN** el perfil `test`
- **WHEN** se consulta la superficie de waitlist
- **THEN** las rutas responden sin credenciales de staff y no modifican la cadena `/auth/**`

#### Scenario: Rutas ausentes en producción

- **GIVEN** un perfil distinto de `dev` y `test`
- **WHEN** se intenta acceder a la superficie de waitlist
- **THEN** las rutas no están registradas
