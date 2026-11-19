# Reglas de Negocio

Cada regla tiene un código único `RN-{DOMINIO}-{NN}` para trazabilidad. `(TBD)` significa que el registro de entrevista no fija el valor; no se inventa.

## Dominio: Agenda (RN-AG)

- **RN-AG-01**: No se pueden reservar turnos superpuestos para el mismo profesional **ni** para el mismo sillón. Debe estar garantizado en la capa de datos, no solo en la UI. *Justificación*: es el problema operativo central (discovery §1, §7).
- **RN-AG-02**: La duración del turno es variable según el tipo de tratamiento; los valores por tratamiento son `(TBD)`.
- **RN-AG-03**: Se permiten **bloqueos** de agenda (profesional/sillón/período) y **sobreturnos**. La regla que habilita un sobreturno sin violar RN-AG-01 es `(TBD)` (¿sillón distinto?, ¿override explícito y auditado?).
- **RN-AG-04**: Horarios laborales y feriados del consultorio limitan la disponibilidad pública. Calendario, zona horaria y anticipación mínima/máxima `(TBD)`.
- **RN-AG-05**: Cancelación y reprogramación permitidas dentro de una ventana configurable de **48 hs hábiles**; fuera de ventana requiere acción de recepción `(TBD)`. Días hábiles y cómputo de horas `(TBD)`.
- **RN-AG-06**: Reserva por link/QR con *write-back* real a la agenda (confirmación inmediata y liberación del hueco). Token de enlace con expiración `(TBD)`.
- **RN-AG-07**: Lista de espera para rellenar cancelaciones. El relleno automático con depósito es diferenciador y su activación en MVP es `(TBD)`.

## Dominio: Clínico (RN-CL)

- **RN-CL-01**: Toda ficha incluye anamnesis y odontograma con nomenclatura **FDI**, incluyendo **dientes temporales**.
- **RN-CL-02**: La historia clínica es versionada; no se sobrescriben evoluciones ni odontogramas anteriores.
- **RN-CL-03**: La evolución se firma con la **matrícula** del profesional; los consentimientos usan firma electrónica conforme Ley 25.506 `(TBD: proveedor/implementación)`.
- **RN-CL-04**: Acceso del paciente conforme Ley 26.529 art. 14 y copia de la historia en 48 h `(TBD: parámetros operativos)`.
- **RN-CL-05**: Conservación de la historia clínica por 10 años según Decreto 1089/2012 `(TBD: confirmar aplicación y política de borrado)`.
- **RN-CL-06**: Si existe asistente con IA, toda derivación humana ante síntomas (dolor, sangrado, postoperatorio) debe registrarse en la historia con override humano auditable. Inclusión de IA en MVP `(TBD)`.

## Dominio: Financiero (RN-FI)

- **RN-FI-01**: El sistema emite presupuestos asociados al paciente y, opcionalmente, al turno.
- **RN-FI-02**: Se permite cobrar **seña** con **Mercado Pago**. Es requisito de paridad, **no** un diferenciador (corrección del informe: DentalSoft, AgendaPro, FLAP, DentApp y Mi Agenda Profesional ya lo documentan).
- **RN-FI-03**: Los eventos de pago son idempotentes y se concilian contra el turno; el detalle de conciliación y arqueo es `(TBD)`.
- **RN-FI-04**: Facturación ARCA: `(TBD)` — el registro la ubica simultáneamente como restricción, etapa posterior y parte del MVP sugerido (IN-01).

## Dominio: Notificaciones (RN-NT)

- **RN-NT-01**: WhatsApp Business API es **obligatoria** en el MVP para confirmación y recordatorio, usando plantillas aprobadas por Meta.
- **RN-NT-02**: La cancelación/reprogramación libera el hueco y dispara aviso al paciente y, si aplica, a la lista de espera `(TBD)`.
- **RN-NT-03**: Los mensajes y webhooks se registran con estado e intentos; verificación de firma `(TBD)`.
- **RN-NT-04**: Costos y límites del proveedor deben ser predecibles para el consultorio: sin packs sorpresa dentro del plan `(TBD: confirmar modelo comercial)`.

## Dominio: Seguridad y cumplimiento (RN-SE)

- **RN-SE-01**: RBAC con los roles definidos; permisos efectivos y herencia `(TBD)`.
- **RN-SE-02**: Toda acción relevante sobre agenda, ficha y dinero genera un registro de auditoría **append-only**.
- **RN-SE-03**: Exportación completa de datos y respaldo periódico; plazos, formato y frecuencia `(TBD)` (el informe propone 48 h y 10 años).
- **RN-SE-04**: Cumplimiento de Ley 25.326, Ley 26.529 y Decreto 1089/2012 con **evidencia demostrable**: DPA responsable/encargado, cláusulas AAIP 198/2023 para subprocessors, registro RNBD/AAIP del responsable, roles, logs y exportación. **No basta con declararlo**: la declaración ya es habitual en Bilog, Odonthia, AgendaPro y otros.
- **RN-SE-05**: Los datos personales y clínicos se tratan con cifrado en tránsito y en reposo; la ubicación de la infraestructura y de los subprocessores es `(TBD)`.
- **RN-SE-06**: El precio del producto debe ser único, público y predecible, con costos de API detallados por separado; modelo exacto `(TBD)`.

## Dominio: Excepciones globales

- **RN-GL-01**: Ante conflicto entre anti-solapamiento y sobreturno, prevalece RN-AG-01 salvo override explícito, con motivo y actor registrados en auditoría.
- **RN-GL-02**: Toda operación crítica (creación de turno desde link público, cobro de seña, confirmación de asistencia) debe ser idempotente o estar protegida contra reenvíos.
- **RN-GL-03**: Las decisiones pendientes `(TBD)` no se resuelven por convención: requieren respuesta del Product Owner antes de codificarse.

## Fuentes

- `docs/discovery/discovery.md` §§5, 7, 8, 9, 11.
- `docs/discovery/informe-discovery.md` «Nota preliminar», §§C.3, C.4, D.3.
