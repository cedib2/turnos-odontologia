# Modelo de Datos

> Modelo **conceptual**, independiente de tecnología. Los tipos son lógicos; la implementación física y los nombres exactos son `(TBD)`.

## Dominios

| Dominio | Descripción |
|---|---|
| Identidad y acceso | Usuarios internos, roles, permisos, sesiones y tokens públicos de reserva |
| Agenda | Profesionales, sillones, horarios, feriados, turnos, bloqueos y sobreturnos |
| Paciente | Datos de contacto e identificación del paciente |
| Clínico | Ficha, anamnesis, odontograma FDI, evoluciones y consentimientos |
| Financiero | Presupuestos, ítems, caja, señas y conciliación con Mercado Pago |
| Notificaciones | Plantillas de WhatsApp, mensajes y eventos de confirmación/recordatorio |
| Auditoría y cumplimiento | Logs inmutables, exportaciones, respaldos, DPA, RNBD y evidencia |
| Configuración | Duraciones por tratamiento, ventana de cancelación, multisede `(TBD)` |
| Ocupación `(TBD)` | Lista de espera, métricas de ocupación y ausentismo (diferenciador) |

## ERD (Entity Relationship Diagram)

```
                       +----------------+
                       |   Consultorio |
                       | (único? TBD)   |
                       +-------+--------+
                               | 1
              +----------------+-----------------+
              |                                  |
      +-------+-------+                   +------+------+
      | Profesional   |                   |   Sillón    |
      +-------+-------+                   +------+------+
              | 1                                | 1
              +-------------+  N:1  +------------+
                            |       |
                      +-----+-------+------+
                      |      Turno        |<----> Bloqueo
                      | (incluye sobre-   |      (solapa prohibida)
                      |  turno)           |
                      +--+--------+------+
                         |        |
                N:1      |        |  1:N
              +----------+        +-------------+
              | Paciente |        | Tratamiento |
              +----+-----+        +-------------+
                   | 1
                   +----------------+------------------+
                   |                                   |
           +-------+--------+                  +-------+--------+
           |  FichaClínica  |                  |  Presupuesto   |
           +---+--------+---+                  +---+------------+
               |        |                         | 1:N
      +--------+   +----+------+           +------+------+
      |Anamnesis|   |Odontograma|          | Ítem / Seña |
      +--------+   | FDI + temp|          +------+------+
                   +----+------+                 |
                        | 1:N                    +--> PagoMP / Conciliación
              +---------+----------+
              | Evolución / Consentimiento |
              +---------------------------+

   Usuario/Rol ---> permisos sobre todo lo anterior
   AuditoríaLog <--- cualquier cambio relevante (append-only)
   Notificación <--- Turno (confirmación/recordatorio vía WhatsApp)
```

## Entidades

### Paciente
- Atributos: `id`, `nombre`, `apellido`, `documento_identidad` `(TBD tipo)`, `fecha_nacimiento` `(TBD)`, `telefono`, `email` `(TBD)`, `direccion` `(TBD)`, `consentimiento_datos`, `fecha_alta`.
- Relaciones: 1:N con Turno, FichaClínica, Presupuesto.
- Constraints: identificador de contacto único `(TBD)`; datos personales cifrados/tratados según Ley 25.326.
- Índices: `documento_identidad`, `telefono`.

### Profesional y Sillón
- Atributos Profesional: `id`, `nombre`, `matricula` (para firma de evolución), `especialidad` `(TBD)`, `activo`.
- Atributos Sillón: `id`, `nombre`, `activo`, `consultorio_id` `(TBD)`.
- Relaciones: N:M con Turno vía disponibilidad; 1:N con Evolución.
- Constraints: matrícula obligatoria para firmar evolución `(TBD)`.
- Índices: `profesional.activo`, `sillon.activo`.

### Turno
- Atributos: `id`, `paciente_id`, `profesional_id`, `sillon_id`, `inicio`, `fin`, `duracion_minutos`, `tipo_tratamiento_id`, `estado` (disponible/reservado/confirmado/cancelado/reprogramado/asistido/ausente `(TBD)`), `es_sobreturno`, `origen` (link/QR/recepción/WhatsApp `(TBD)`), `token_publico`, `created_at`, `updated_at`.
- Relaciones: N:1 con Paciente, Profesional, Sillón, Tratamiento; 1:N con Notificación, Seña, ListaDeEspera `(TBD)`.
- Constraints: **RN-AG-01** — no solapamiento para el mismo profesional o el mismo sillón, enforcement en base de datos `(TBD)`; sobreturnos sujetos a regla explícita `(TBD)`.
- Índices: `(profesional_id, inicio, fin)`, `(sillon_id, inicio, fin)`, `estado`, `token_publico`.

### Bloqueo y Feriado
- Bloqueo: `id`, `profesional_id` `(TBD nullable)`, `sillon_id` `(TBD nullable)`, `desde`, `hasta`, `motivo`.
- Feriado: `fecha`, `descripcion`, `aplica_a_todos` `(TBD)`.
- Constraints: un bloqueo también ocupa agenda y debe respetarse en la disponibilidad pública.

### FichaClínica, Anamnesis y Odontograma
- Ficha: `id`, `paciente_id`, `historia_version`, `fecha_apertura`.
- Anamnesis: `id`, `ficha_id`, `respuestas` (estructura `(TBD)`), `fecha`, `autor_id`.
- Odontograma: `id`, `ficha_id`, `pieza_fdi`, `temporal` (booleano), `estado`, `cara` `(TBD)`, `fecha`, `autor_id`, `version`.
- Relaciones: 1:N con Evolución y Consentimiento.
- Constraints: codificación FDI; dientes temporales incluidos; historia versionada; evolución firmada con matrícula (RN-CL-03); retención 10 años `(TBD)` y copia al paciente en 48 h `(TBD)`.

### Presupuesto, Ítem, Seña y PagoMP
- Presupuesto: `id`, `paciente_id`, `turno_id` `(TBD)`, `estado`, `total`, `fecha`.
- Ítem: `id`, `presupuesto_id`, `tratamiento_id`, `cantidad`, `precio`, `descripcion`.
- Seña/PagoMP: `id`, `turno_id`, `presupuesto_id` `(TBD)`, `monto`, `moneda`, `estado`, `proveedor="mercadopago"`, `external_reference`, `idempotency_key`, `fecha_aprobacion`, `conciliado` `(TBD)`.
- Constraints: un evento de pago se aplica una sola vez (idempotencia); seña por paridad, no diferenciador.

### Usuario, Rol y Permiso
- Usuario: `id`, `email`, `hash_password` o proveedor externo `(TBD)`, `rol_id`, `activo`, `ultimo_acceso`.
- Rol: `paciente`, `recepcionista`, `odontologo`, `administrador` `(TBD)`, `auditor` `(TBD)`.
- Permiso: `rol_id`, `recurso`, `accion`, `ambito` (propio/todos/config).
- Constraints: RBAC; permisos efectivos y herencia `(TBD)`.

### AuditoríaLog y Evidencia
- Log: `id`, `fecha`, `actor_id`, `accion`, `entidad`, `entidad_id`, `ip`, `dispositivo` `(TBD)`, `hash_encadenado` `(TBD)`.
- Evidencia: `dossier_id`, `dpa`, `registro_rnbd_aaip`, `subprocesadores`, `clausulas_aaip_198_2023`, `exportacion_id`, `respaldo_id`, `periodo`.
- Constraints: append-only; exportación completa en 48 h `(TBD)`; retención 10 años `(TBD)`.

### Notificación y Plantilla
- Plantilla: `id`, `canal="whatsapp"`, `codigo_meta`, `estado_aprobacion`, `idioma`.
- Notificación: `id`, `turno_id`, `plantilla_id`, `destinatario`, `estado`, `proveedor_message_id`, `intentos`, `fecha`.
- Relaciones: 1:N con Turno; handoff humano registrado si hay IA `(TBD)`.

### ListaDeEspera y Métricas `(TBD)`
- ListaDeEspera: `id`, `paciente_id`, `preferencias`, `deposito_requerido`, `estado`, `fecha`.
- Métrica: `sillon_id`, `periodo`, `ocupacion`, `ausentismo`, `metodologia` (obligatoria para que sea verificable).

## Seed data inicial

| Datos | Estado |
|---|---|
| Roles base (paciente, recepcionista, odontólogo, administrador `(TBD)`) | Requerido |
| Permisos por rol según [03_actores_y_roles.md](03_actores_y_roles.md) | Requerido; detalle `(TBD)` |
| Catálogo de tratamientos con duración variable | Requerido; valores `(TBD)` |
| Catálogo FDI de piezas permanentes y temporales | Requerido |
| Horarios laborales y feriados del consultorio | Requerido; valores `(TBD)` |
| Plantillas de WhatsApp aprobadas | Requerido; códigos `(TBD)` |
| Plantillas de anamnesis y consentimiento | Requerido; textos `(TBD)` |
| Configuración de Mercado Pago y logging de auditoría | Requerido `(TBD)` |

## Fuentes

- `docs/discovery/discovery.md` §§5, 7, 8.
- `docs/discovery/informe-discovery.md` §A.1, §A.2, §C.3, §D.3.
