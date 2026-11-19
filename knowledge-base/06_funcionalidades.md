# Funcionalidades

Organizadas por **épica** y luego por **historia de usuario** (`US-NNN`). Los criterios con `(TBD)` requieren decisión antes de implementarse.

## Épica 1: Agenda y turnos (MVP)

### US-001 — Agenda multiprofesional y multisillón
**Como** recepcionista  
**Quiero** ver y editar la agenda por profesional y por sillón  
**Para** centralizar la operación y detectar solapamientos.

**Criterios de aceptación**:
- [ ] Vista día/semana por profesional y por sillón.
- [ ] Duración variable según tipo de tratamiento (valores `(TBD)`).
- [ ] Bloqueos y sobreturnos con regla explícita `(TBD)`.
- [ ] El sistema rechaza solapamientos incluso ante escrituras concurrentes (RN-AG-01).

**Reglas relacionadas**: RN-AG-01, RN-AG-02, RN-AG-03, RN-AG-04.

### US-002 — Administrar disponibilidad
**Como** recepcionista  
**Quiero** configurar horarios laborales, feriados y duraciones  
**Para** publicar solo huecos realmente atendibles.

**Criterios de aceptación**:
- [ ] Alta/edición de horarios y feriados; calendario y zona horaria `(TBD)`.
- [ ] Cambios reflejados en el link/QR público sin recargar `(TBD: mecanismo)`.
- [ ] Anticipación mínima/máxima de reserva `(TBD)`.

**Reglas relacionadas**: RN-AG-02, RN-AG-04.

## Épica 2: Reserva online y autogestión del paciente (MVP)

### US-003 — Reservar por link o QR
**Como** paciente  
**Quiero** elegir un horario disponible desde un enlace o QR  
**Para** obtener el turno al instante sin llamar.

**Criterios de aceptación**:
- [ ] El enlace muestra solo huecos válidos según RN-AG-01 y RN-AG-04.
- [ ] Confirmación inmediata con *write-back* a la agenda (RN-AG-06).
- [ ] Enlace con token con expiración `(TBD)`.
- [ ] Si el hueco se tomó, el sistema propone alternativas `(TBD)`.

**Reglas relacionadas**: RN-AG-01, RN-AG-06, RN-GL-02.

### US-004 — Confirmar asistencia
**Como** consultorio  
**Quiero** que el paciente confirme su turno  
**Para** reducir ausentismo con datos verificables.

**Criterios de aceptación**:
- [ ] Enlace de confirmación enviado por WhatsApp.
- [ ] Estado de confirmación visible en la agenda.
- [ ] Métrica de confirmaciones/ausencias con metodología `(TBD)`.

**Reglas relacionadas**: RN-NT-01, RN-AG-06.

### US-005 — Cancelar o reprogramar
**Como** paciente  
**Quiero** cancelar o reprogramar desde el enlace  
**Para** liberar el horario y recolocarme sin llamar.

**Criterios de aceptación**:
- [ ] Aplicación de la ventana de 48 hs hábiles (RN-AG-05).
- [ ] Reprogramación valida disponibilidad y vuelve a aplicar anti-solapamiento.
- [ ] Si aplica, el hueco se ofrece a la lista de espera `(TBD)`.
- [ ] Fuera de ventana, deriva a recepción `(TBD)`.

**Reglas relacionadas**: RN-AG-05, RN-AG-07, RN-NT-02.

## Épica 3: Notificaciones por WhatsApp (MVP)

### US-006 — Recordatorios y confirmaciones
**Como** paciente  
**Quiero** recibir recordatorios por WhatsApp  
**Para** no olvidar mi turno.

**Criterios de aceptación**:
- [ ] WhatsApp Business API obligatoria, con plantillas aprobadas (RN-NT-01).
- [ ] Recordatorio y confirmación con registro de estado e intentos (RN-NT-03).
- [ ] Costos visibles y predecibles `(TBD)`.

**Reglas relacionadas**: RN-NT-01, RN-NT-03, RN-NT-04.

### US-007 — Derivación humana `(TBD)`
**Como** paciente con urgencia  
**Quiero** que el asistente me derive a una persona  
**Para** recibir atención clínica segura.

**Criterios de aceptación**:
- [ ] Si se habilita IA, reglas explícitas de derivación ante dolor, sangrado o postoperatorio (RN-CL-06).
- [ ] Cada handoff queda registrado en la historia clínica.
- [ ] Inclusión en MVP: `(TBD)`.

**Reglas relacionadas**: RN-CL-06.

## Épica 4: Historia clínica (MVP)

### US-008 — Registrar ficha, anamnesis y odontograma
**Como** odontólogo  
**Quiero** registrar la ficha clínica con anamnesis y odontograma FDI  
**Para** documentar la atención con criterio clínico propio.

**Criterios de aceptación**:
- [ ] Odontograma FDI con piezas temporales (RN-CL-01).
- [ ] Historia versionada, sin sobrescritura (RN-CL-02).
- [ ] Estructura de anamnesis `(TBD)`.

**Reglas relacionadas**: RN-CL-01, RN-CL-02.

### US-009 — Firmar evolución y consentimientos
**Como** odontólogo  
**Quiero** firmar la evolución con mi matrícula y registrar consentimientos  
**Para** dar validez y trazabilidad al registro.

**Criterios de aceptación**:
- [ ] Firma con matrícula (RN-CL-03).
- [ ] Consentimientos con firma electrónica; proveedor `(TBD)`.
- [ ] Registro de auditoría por cada firma.

**Reglas relacionadas**: RN-CL-03, RN-SE-02.

### US-010 — Acceso y copia de la historia por el paciente
**Como** paciente  
**Quiero** acceder y obtener copia de mi historia clínica  
**Para** ejercer mis derechos bajo Ley 26.529 art. 14.

**Criterios de aceptación**:
- [ ] Verificación de identidad `(TBD)`.
- [ ] Copia en 48 h `(TBD: parámetros)`.
- [ ] Registro de cada acceso/exportación (RN-SE-02).

**Reglas relacionadas**: RN-CL-04, RN-SE-02, RN-SE-03.

## Épica 5: Presupuesto, caja y señas (MVP)

### US-011 — Emitir presupuesto
**Como** recepcionista  
**Quiero** generar un presupuesto por tratamiento  
**Para** acordar el costo antes de la atención.

**Criterios de aceptación**:
- [ ] Ítems con tratamiento, cantidad y precio.
- [ ] Asociación opcional al turno `(TBD)`.
- [ ] Impresión/PDF `(TBD)`.

**Reglas relacionadas**: RN-FI-01.

### US-012 — Cobrar seña con Mercado Pago
**Como** paciente  
**Quiero** pagar una seña para confirmar mi turno  
**Para** asegurar mi horario.

**Criterios de aceptación**:
- [ ] Integración Mercado Pago **por paridad**, no como diferenciador (RN-FI-02).
- [ ] Idempotencia ante reenvío de webhook (RN-FI-03, RN-GL-02).
- [ ] Conciliación turno–pago `(TBD)`.
- [ ] Regla de reembolso ante cancelación dentro/fuera de ventana `(TBD)`.

**Reglas relacionadas**: RN-FI-02, RN-FI-03, RN-AG-05.

## Épica 6: Seguridad, auditoría y cumplimiento (MVP)

### US-013 — Roles y permisos
**Como** administrador  
**Quiero** asignar roles y permisos  
**Para** limitar el acceso a datos sensibles.

**Criterios de aceptación**:
- [ ] Matriz de [03_actores_y_roles.md](03_actores_y_roles.md) implementada.
- [ ] Herencia y permisos efectivos `(TBD)`.

**Reglas relacionadas**: RN-SE-01.

### US-014 — Auditar, exportar y respaldar
**Como** responsable de datos  
**Quiero** consultar auditoría, exportar todo y respaldar  
**Para** cumplir y demostrar el cumplimiento.

**Criterios de aceptación**:
- [ ] Log append-only de acciones relevantes (RN-SE-02).
- [ ] Exportación completa y respaldo; plazos y formato `(TBD)` (informe propone 48 h y 10 años).
- [ ] Dossier descargable con DPA, AAIP/RNBD y cláusulas a subprocessores `(TBD: alcance)`.

**Reglas relacionadas**: RN-SE-02, RN-SE-03, RN-SE-04.

## Épica 7: Diferenciadores (posterior al MVP, salvo decisión contraria)

### US-015 — Motor de ocupación `(TBD)`
**Como** consultorio  
**Quiero** rellenar automáticamente cancelaciones desde una lista de espera con depósito  
**Para** mejorar la ocupación con métricas verificables.

**Criterios de aceptación**:
- [ ] Waitlist con depósito configurable y liberación automática del hueco.
- [ ] Métricas de ocupación y ausentismo con metodología, período y por sillón.
- [ ] Conciliación con Mercado Pago y comprobante `(TBD)`.
- [ ] Inclusión en v0.1: `(TBD)` — discovery la clasifica como opcional.

**Reglas relacionadas**: RN-AG-07, RN-FI-03, RN-NT-04.

### US-016 — Precio único y predecible `(TBD)`
**Como** consultorio  
**Quiero** un plan todo incluido con recordatorios, seña y handoff  
**Para** evitar costos sorpresa.

**Criterios de aceptación**:
- [ ] Precio público, único y estable; costos de API detallados aparte.
- [ ] Modelo comercial exacto `(TBD)`.

**Reglas relacionadas**: RN-NT-04, RN-SE-06.

## Épica 8: Etapas posteriores (fuera de v0.1)

- US-017 — Validación y liquidación de obras sociales/prepagas `(TBD: alcance por obra social)`.
- US-018 — Facturación ARCA y receta electrónica con validez legal `(TBD: CRUCE CON IN-01)`.
- US-019 — Periodontograma con curvas, radiología/PACS e imágenes 3D.
- US-020 — Google Calendar, API pública e interoperabilidad FHIR.
- US-021 — Multisede avanzada e IA conversacional amplia con reglas auditables.

## Fuentes

- `docs/discovery/discovery.md` §§3, 5, 6, 7, 8, 11.
- `docs/discovery/informe-discovery.md` §§C.2, C.3, C.4, D.3.
