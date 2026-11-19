# Flujos Principales

Cada flujo se documenta de extremo a extremo. Los componentes están en términos lógicos; su tecnología es `(TBD)`.

## Flujo 1: Reserva online por link/QR

**Disparador**: el paciente abre el enlace/publica el QR.  
**Actor**: Paciente.

**Pasos**:
1. Capa pública valida token y obtiene huecos libres de Agenda.
2. Agenda aplica RN-AG-01, RN-AG-02, RN-AG-04 y devuelve disponibilidad.
3. Paciente elige profesional/sillón, tratamiento y horario.
4. Aplicación crea el Turno con transacción que impide solapamiento (RN-AG-01).
5. Si corresponde, se genera seña en Mercado Pago (RN-FI-02).
6. Se emite evento de notificación (WhatsApp) y se confirma al paciente.
7. Auditoría registra creación, origen y actor (RN-SE-02).

```
Paciente → Link/QR → Aplicación → Agenda/DB → Mercado Pago (opcional)
                           ↓              ↓
                     Notificación    Auditoría
                     (WhatsApp)
```

**Casos de error**:
- Hueco tomado entre consulta y confirmación → rechazo atómico y sugerencia de alternativas `(TBD)`.
- Token vencido → solicitar nuevo enlace `(TBD)`.
- Webhook de pago duplicado → idempotencia, sin doble cobro (RN-GL-02).
- WhatsApp sin plantilla aprobada → no enviar y registrar fallo.

## Flujo 2: Cancelación / reprogramación con ventana de 48 hs hábiles

**Disparador**: el paciente usa el enlace de gestionar turno.  
**Actor**: Paciente; Recepcionista si está fuera de ventana.

**Pasos**:
1. Validar token y identidad del titular `(TBD)`.
2. Calcular ventana de 48 hs hábiles (RN-AG-05); feriados y zona horaria `(TBD)`.
3. Si está dentro de ventana: cancelar o reprogramar con validación de disponibilidad.
4. Liberar hueco y actualizar agenda con *write-back* (RN-AG-06).
5. Notificar al paciente; si aplica, ofrecer hueco a lista de espera (RN-AG-07, `(TBD)`).
6. Registrar motivo, actor y estado en auditoría.

```
Paciente → Aplicación → Regla 48 hs → Agenda/DB → WhatsApp
                              ↓
                        Lista de espera (TBD)
```

**Casos de error**:
- Fuera de ventana → derivar a recepción y no liberar automáticamente `(TBD)`.
- Reprogramación a hueco ya ocupado → nueva propuesta.
- Seña pagada → política de reembolso `(TBD)`.

## Flujo 3: Confirmación y recordatorio por WhatsApp

**Disparador**: turno creado o reprogramado; recordatorio programado.  
**Actor**: Sistema, Paciente.

**Pasos**:
1. Aplicación emite evento de notificación (plantilla aprobada).
2. Adaptador WhatsApp envía mensaje con enlace de confirmación.
3. Paciente confirma; webhook actualiza estado del turno.
4. Si hubiera asistente con IA y el paciente describe urgencia, se registra handoff humano en la historia (RN-CL-06); inclusión `(TBD)`.
5. Se registran estado, intentos y proveedor_message_id (RN-NT-03).

```
Aplicación → Outbox (TBD) → WhatsApp API → Paciente
                  ↑                           ↓
              Agenda/DB  ←── webhook ── confirmación
```

**Casos de error**:
- Plantilla rechazada por Meta → bloquear envío y avisar a administración.
- Webhook sin firma válida → descartar (RN-NT-03).
- Reintento duplicado → idempotencia.

## Flujo 4: Registro clínico y firma

**Disparador**: el odontólogo atiende al paciente.  
**Actor**: Odontólogo.

**Pasos**:
1. Odontólogo abre ficha desde el turno.
2. Completa anamnesis y odontograma FDI con piezas temporales (RN-CL-01).
3. Registra evolución y la firma con matrícula (RN-CL-03).
4. Registra consentimiento con firma electrónica; proveedor `(TBD)`.
5. Se versiona la historia (RN-CL-02) y se escribe auditoría (RN-SE-02).
6. Si corresponde, genera presupuesto (Flujo 5).

```
Odontólogo → Ficha → Anamnesis/Odontograma → Evolución firmada → Auditoría
```

**Casos de error**:
- Matrícula ausente → impedir firma.
- Intento de sobrescribir evolución → crear nueva versión.
- Firma electrónica no disponible → `(TBD: política de contingencia)`.

## Flujo 5: Presupuesto y seña con Mercado Pago

**Disparador**: se acuerda un tratamiento.  
**Actor**: Recepcionista, Paciente.

**Pasos**:
1. Recepcionista crea presupuesto con ítems (RN-FI-01).
2. Genera solicitud de seña en Mercado Pago por paridad (RN-FI-02).
3. Paciente paga; Mercado Pago envía webhook.
4. Aplicación valida firma, aplica idempotencia y marca turno/presupuesto (RN-FI-03).
5. Conciliación y caja `(TBD)`; auditoría del evento.

```
Recepcionista → Presupuesto → Mercado Pago → Paciente
                     ↑              ↓ webhook
                   Caja ← Aplicación (idempotente)
```

**Casos de error**:
- Webhook duplicado → no re-aplicar pago.
- Pago rechazado → notificar y mantener turno `(TBD: regla de retención del hueco)`.
- Facturación ARCA → `(TBD, ver IN-01)`.

## Flujo 6: Acceso, exportación, respaldo y evidencia de cumplimiento

**Disparador**: solicitud del paciente o tarea programada/responsable de datos.  
**Actor**: Paciente, Administrador `(TBD)`.

**Pasos**:
1. Verificar identidad y permisos (RN-SE-01).
2. Generar copia de historia en 48 h `(TBD)` o exportación completa (RN-SE-03).
3. Registrar cada acceso/exportación en log append-only (RN-SE-02).
4. Ejecutar respaldo con frecuencia `(TBD)`; conservación 10 años `(TBD)`.
5. Componer dossier: DPA, RNBD/AAIP, cláusulas AAIP 198/2023, logs y exportación (RN-SE-04).

```
Solicitud → Autorización → Generación → Descarga
                 ↓              ↓
             Auditoría      Dossier (evidencia)
```

**Casos de error**:
- Identidad no verificada → negar acceso y registrar intento.
- Fallo de respaldo → alerta a administración `(TBD)`.
- Subprocesador sin cláusulas publicadas → bloquear inclusión en dossier `(TBD)`.

## Flujo 7: Relleno automático de waitlist con depósito `(TBD — diferenciador)`

**Disparador**: cancelación o liberación de hueco.  
**Actor**: Sistema; Paciente en lista de espera.

**Pasos**:
1. Detectar hueco liberado y buscar candidatos por preferencias.
2. Requerir depósito configurable (RN-AG-07).
3. Ofrecer el turno por WhatsApp; confirmar pago y adjudicar con transacción.
4. Actualizar agenda, conciliar pago y publicar métrica de ocupación/ausentismo con metodología.

**Casos de error**:
- Candidato no paga en plazo `(TBD)` → pasar al siguiente.
- Doble adjudicación → transacción atómica e idempotencia.

## Fuentes

- `docs/discovery/discovery.md` §§3, 5, 7, 8.
- `docs/discovery/informe-discovery.md` §§C.2, C.4, D.3.
