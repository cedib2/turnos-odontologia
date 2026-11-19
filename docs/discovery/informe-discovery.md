# Informe de investigación de mercado — Sistemas de gestión odontológica

**Fecha de elaboración y consulta:** 19 de noviembre de 2026
**Alcance:** Argentina (prioridad) · Latinoamérica · soluciones internacionales inspiradoras
**Metodología:** búsqueda y lectura de fuentes públicas verificables — sitios oficiales, documentación y centros de ayuda, páginas de precios, términos y políticas de privacidad, marketplaces y reseñas con autor identificable, y notas periodísticas citadas con fecha. Se consultaron 60+ URLs; todas las referencias de este informe tienen fecha de consulta **2026-11-19**.

## Convenciones de evidencia

| Marca | Significado |
|---|---|
| **[C] Comprobado** | Funcionalidad, precio o declaración consta en documento oficial, help center, tabla de precios, términos o ficha de marketplace. |
| **[A] Afirmación comercial** | Copy de marketing, métrica autopublicada o promesa sin metodología, auditoría ni fecha de corte. |
| **No evidenciado** | No aparece demostrado públicamente en las fuentes consultadas. **No implica que la función no exista**: implica que no puede afirmarse. |

Criterio crítico aplicado: cuando un proveedor publica una cifra sin metodología, se registra como *afirmación*, no como dato. Cuando dos páginas oficiales del mismo proveedor difieren, se reporta la discrepancia.

---

## Nota preliminar: correcciones al informe previo

El análisis de fuentes primarias obliga a rectificar dos conclusiones del documento que originó este trabajo:

1. **«Ningún sistema odontológico cobra seña con Mercado Pago» es falso.** DentalSoft documenta «cobrá señas y deudas»; AgendaPro ofrece «seña online para confirmar asistencia» con integración Mercado Pago; también FLAP, DentApp y Mi Agenda Profesional publican señas vía Mercado Pago. [C]
2. **«Ningún proveedor declara adecuación a la Ley 25.326» es falso.** Bilog (términos actualizados al 29/10/2026), Odonthia (política de privacidad) y AgendaPro (FAQ del producto dental) declaran expresamente la norma. [C] Otra cosa —no demostrada— es que exista certificación, inscripción RNBD por proveedor o auditoría pública.

Además: las cifras de adopción de Bilog difieren entre páginas de su propio sitio (+1.500 clínicas/+5.000 usuarios en un caso; +3.000/+20.000 en otro); los «1.219 turnos» y «7 de cada 10 conversaciones» de Órbita son autopublicados y **no** aparecen en las notas de TN y Ámbito de julio de 2026; Odonthia publica **tres juegos de precios distintos** en fuentes oficiales; y el «ARS 14.900» de AgendaPro no está documentado. [C]

**Consecuencia:** la tesis de oportunidad no puede apoyarse en «nadie lo hace». Debe apoyarse en **evidenciar mejor** lo que ya se ofrece de modo opaco o no verificable.

---

# A. Tabla comparativa

Ordenada por relevancia para el mercado argentino. `NE` = No evidenciado. Todas las URLs se listan en el apartado *Fuentes consultadas* (2026-11-19).

## A.1 — Identificación, agenda, turnos y automatización

| # | Sistema (empresa · país) | Segmento · modalidad | Agenda | Turnos digitales | Automatización |
|---|---|---|---|---|---|
| 1 | **Bilog** (Bilog · AR) | Consultorio, clínica, gerenciadoras · SaaS web + app iOS/Android [C] | Diaria/semanal por profesional, arrastre, estados, presentismo; evita superposición online [C]. Duración variable, bloqueos, sobreturnos: **NE** | Link/QR, consulta, reserva y cancelación online [C]; reprogramación, chatbot transaccional, lista de espera: **NE** | Recordatorios WhatsApp/email/SMS, confirmaciones [C]; ausentes, recupero, controles, campañas: **NE** |
| 2 | **Órbita / Hiorbita** (Órbita Global · AR) | Consultorio, clínica, red multisede · SaaS en AWS [C] | Por sillón, anti-choque en base de datos, día/semana ampliada/mes, drag & drop, bloqueos, sobreturno, lista de espera, turnos en serie [C]; duración variable: **NE** | Asistente WhatsApp 24/7 con audios: da, reagenda, cancela, confirma con botones, deriva urgencias, pide reseña [C]; API Meta oficial [C]; reserva web como producto: **NE** | Recordatorios (14 plantillas Meta, dos rondas en turnos largos), recontacto, campañas, controles automáticos [C] |
| 3 | **Dentalink** (Healthatom/Engenis · CL) | Consultorio a multicentro, laboratorios · SaaS 100 % nube [C] | Global/diaria/semanal por profesional y sillón, estados, sobreagendamiento paralelo, reprogramación individual o masiva, duraciones por odontólogo [C]; bloqueos, lista de espera: **NE** | Agendamiento online en los tres planes [C]; turnos por WhatsApp y Contact Center IA 24/7 que agenda/confirma/reprograma/cancela [C]; lista de espera: **NE** | Recordatorios y confirmaciones WhatsApp/email, recupero de ausentes, reagendamiento y cobranza, encuestas, email marketing, tareas de control [C] |
| 4 | **DentalTec** (Tándem Digital · AR) | Odontólogo, consultorio, círculos y federaciones (CORA) · SaaS 100 % web [C] | Turnos configurables por odontólogo, día, horario e intervalo; multi-odontólogo con secretaría y 10 roles [C]; vista semanal, sillones, duración variable, bloqueos, sobreturnos, anti-solapamientos: **NE** | Confirmación de asistencia por WhatsApp [C]; «integración oficial WhatsApp» [A]; reserva 24/7, link, web, chatbot, cancelación, reprogramación, lista de espera: **NE** | Recordatorios automáticos y confirmación por WhatsApp [C]; ausentes, recupero, controles, campañas: **NE** |
| 5 | **DentalCore** (Alfredo Di Tullio · AR) | Consultorio y clínica; 8 países · SaaS navegador [C] | Multi-doctor día/3 días/semana/mes, mapa de calor, drag & drop, filtro, kanban, bloqueos, timer, sala de espera [C]; sillones, duración variable, sobreturnos, control de solapamientos: **NE** | Confirmación/cancelación por link vía WhatsApp, recordatorios 48/24 h [C]; bandeja compartida Meta [C]; reserva 24/7 autónoma, redes, chatbot, lista de espera: **NE** | 7 comunicaciones automáticas, 25 tipos de tareas, recall por segmentos, recupero de tratamientos no agendados, controles vencidos, calendario de contenido [C] |
| 6 | **AgendaPro** (AgendaPro · CL/AR) | Consultorio, clínica, centro médico, multisede · SaaS + apps iOS/Android [C] | Por odontólogo, práctica y sillón; duración por práctica [C]; bloqueos, sobreturnos, anti-solapamientos: **NE** | 24/7, web, Google Maps/Reserve, WhatsApp/Instagram/Messenger vía IA, confirmar/reagendar/cancelar [C]; lista de espera: **NE** | Recordatorios WhatsApp 1/2/3 días, segmentación y recuperación [C]; «hasta 90 % menos ausencias» [A] |
| 7 | **DentalSoft** (DentalSoft · AR) | Consultorio y clínica, multi-sucursal · nube 100 % [C] | Día/semana/mes, drag & drop, bloqueos, urgencias y sobreturnos con detección de solapamiento, multi-profesional/sucursal [C] | 24/7, web propia, confirmación/cancelación/reprogramación, portal, chatbot; WhatsApp como botón/chat [C]; lista de espera: **NE** | Recordatorios y confirmación WhatsApp/email, cobro de deudas, recuperación de inactivos, reseñas Google [C] |
| 8 | **Odonthia** (Odonthia · AR) | Consultorio, clínica multisede, CRM clínico · web app freemium/PWA [C] | Día/semana/mes, multi-profesional [C]; bloqueos, sobreturnos, anti-solapamientos: **NE** | Reserva online propia, ficha QR, portal del paciente [C]; WhatsApp y lista de espera: **NE** en plan gratuito | WhatsApp automático y campañas de email solo en planes de pago [C]; IA por créditos [C] |
| 9 | **DentiClick** (Maximiliano Espeche · AR/VE) | Consultorio y clínica, multilocación en plan Centro · SaaS + app iOS [C] | Día/semana/mes, estados, reprogramación [C]; bloqueos, sobreturnos, anti-solapamientos: **NE** | Mini-web con subdominio y reservas 24/7 [C]; WhatsApp, chatbot reservador, lista de espera: **NE** | Asistente IA web [C]; recordatorios WhatsApp/correo según App Store [C]; campañas: **NE** |
| 10 | **Odonto.ar** (Odonto.ar · AR) | Independiente, consultorio, clínica hasta 10 usuarios · SaaS web [C] | Día/semana/mes, traslado, duración por motivo, disponibilidad y anticipación configurables [C]; bloqueos, sobreturnos, anti-solapamientos: **NE** | Link público 24/7, reglas de cancelación/reprogramación [C]; WhatsApp, chatbot, lista de espera: **NE** | Recordatorios/reprogramación por comunicación interna [A]; campañas y recupero: **NE** |
| 11 | **DenPro** (Elite Digital Services LLC · .ar/UE) | Independiente, multilocalidad, cadenas/DSO, universidades · cloud [C/A] | Calendario drag & drop, multi-profesional, recurrente, lista de espera [C/A]; día/semana, sobreturnos, anti-solapamientos: **NE** | Reserva por link/QR 24/7, confirmación SMS/email [C/A]; WhatsApp, chatbot, redes: **NE** | Recordatorios SMS/email 48 h y 2 h, CRM de reactivación [A] |
| 12 | **Dentiqa** (Violet Wave · AR/LATAM) | Clínica; sucursales en Enterprise · SaaS multi-tenant [C] | Día/semana, multi-dentista, bloqueos, reprogramación validada, evita doble reserva [C/A]; sobreturnos y anti-solapamiento explícito: **NE** | Chatbot WhatsApp 24/7 agenda/cancela, recordatorios 24 h y 2 h [C/A]; link/web y lista de espera: **NE** | Campañas WhatsApp (cumpleaños, reactivación, post-visita), CRM 7 etapas, coach IA [C/A] |
| 13 | **Benty** (BENTY SRL · AR) | Independiente, consultorio, clínica · SaaS online [C] | Turnos por profesional, espacio y disponibilidad; listado de espera [A]; vista día/semana y anti-solapamientos: **NE** | Portal con reserva online, actualización de datos y contacto WhatsApp [C/A]; 24/7: **NE**; bot de consultas [A] | Mensajes configurables de recordatorio/confirmación, bot WhatsApp [C/A]; ausentes, recupero, campañas: **NE** |
| 14 | **Doctocliq** (Doctocliq · PE) | Consultorio a clínica multisede · SaaS + apps [C] | Día/semana/mes, vista por sillón y profesional, agrupación por equipo (Avanzado), bloqueos, duración variable [C]; anti-solapamientos, sobreturnos: **NE** | Link propio integrable en web/redes, paciente agenda solo, confirma/reprograma/cancela [C/A]; lista de espera: **NE** | Recordatorios WhatsApp/email (50-150/mes), campañas, segmentación, reseñas; Soyla IA responde y agenda (módulo adicional) [C/A] |
| 15 | **Dentidesk** (Dentidesk Chile SpA · CL) | Consultorios, clínicas, sindicatos, universidades · nube + apps [C] | Horarios personalizables, notas, recordatorios, reprogramación [A]; multi-profesional, sillones, anti-solapamientos sin detalle; duración variable, sobreturnos: **NE** | **WhatsApp = enlace manual a WhatsApp Web con mensajes predefinidos** [C]; confirmación automática por email [C]; reserva online paciente: **NE** | Confirmación automática por email, liquidación automática a dentistas [A]; campañas/recupero: **NE** |
| 16 | **Clinic Cloud / Doctoralia** (Doctoralia Internet S.L. · ES) | Clínicas dentales y multiespecialidad · SaaS cloud [C] | Agendas por profesional (1/3/5 según plan), vistas, bloqueos, actualización en tiempo real [C/A]; duración variable, sobreturnos, anti-solapamientos: **NE** | Portal y **reserva 24/7 vía Doctoralia** sincronizada [C]; recordatorios SMS a convenir, lista de espera inteligente en Doctoralia Pro [A]; WhatsApp: **NE** salvo recordatorios Pro | Recordatorios, campañas email/SMS, reseñas, Noa Reception (acceso anticipado), Noa Notes/Summary de pago [C/A] |
| 17 | **Open Dental** (Open Dental Software Inc. · US) | Consultorio cualquier tamaño · instalación local + Cloud opcional [C] | Por consultorio/profesional/empleado, Pinboard, drag & drop, búsqueda de huecos, blockouts, vistas semana [C]; anti-solapamiento no declarado; duración variable y ASAP [C] | **Web Sched** (Recall/New/Existing/ASAP): agenda online, alta automática, link para reclamar/rechazar huecos [C]; WhatsApp y chatbot: **NE** | eReminders, eConfirmations, Integrated Texting, Mass Email, recall [C] |
| 18 | **Curve Dental** (CD Newco LLC · US) | Consultorios y grupos · PMS cloud [C] | Sidekick, vistas por sillón/profesional, incrementos configurables, blockouts, **Self-Scheduling 24/7** con permisos por tipo de cita [C/A]; anti-solapamiento: **NE** | Reserva 24/7 desde web, confirmación y recordatorios, **Smart Fill** para cancelaciones con waitlist y planes no agendados, two-way texting [C/A]; WhatsApp: **NE** | Curve GRO: recordatorios, recare, campañas por segmentos, Smart Action List [C/A] |
| 19 | **tab32** (tab32 Inc. · US) | Consultorios (Alpine) y grupos/DSO (Summit) · cloud [C] | Columnas por sillón, ASAP/waitlist, online booking con disponibilidad en tiempo real, «no double-booking» [C/A]; multi-sede en Summit [C] | Booking nativo vía HelloPatient, portal sin login, reprogramación, formularios, depósitos para reducir cancelaciones [C/A]; WhatsApp: **NE** | AutoRemind y **Agent Layer** IA: confirmación por texto/llamada, seguimiento de ausentes, backfill, hygiene recall, reactivación [C/A] |
| 20 | **NexHealth** (NexHealth Inc. · US) | Consultorios, grupos/DSO · capa de scheduling sincronizada vía Synchronizer [C] | Agenda del PMS; reglas por tipo de cita, horarios, duraciones y rotaciones; «prevent double bookings» por sync [C/A]; sillones/sobreturnos: **NE** | **Reserva 24/7** en web, Google Search/Maps, SMS, email, QR y redes; recalls 1-clic; **waitlist automatizada**; depósitos [C/A]; chatbot: **NE** | Recordatorios, mensajería y campañas, recall, reseñas Google [C]; Copilot en piloto limitado 2026 [A] |
| 21 | **Weave** (Weave Communications Inc. · US) | Clínicas dentarias; plataforma de comunicación, **no PMS clínico** [C] | Calendario con write-back al PMS, tipos de cita y duraciones, disponibilidad por proveedor [C]; sillones, sobreturnos, anti-solapamientos: **NE** | Appointment Requests desde web/Google con **aprobación manual**; Online Scheduling con waitlist; confirmación SMS y cancelación/reprogramación con link [C] | Recordatorios con análisis de sentimiento, Missed Call Text, QuickFill/waitlist, recall y reactivación segmentada, reseñas [C] |
| 22 | **BlueDental** (Morelli/Dental Morelli · BR) | Individual a clínica multi-dentista · nube, portugués [A] | «Fácil agendamento» multi-profesional y multiclínica [A]; día/semana, sillón, duración, bloqueos, sobreturnos, anti-solapamientos: **NE** | Envío de mensajes e-mail/WhatsApp para confirmar, cumpleaños y retorno [A]; **link de reserva 24/7 y chatbot: NE** | Confirmación y lembretes [A]; recupero/campañas: **NE** |

## A.2 — Clínico, administración, integraciones, operación, seguridad y comercio

| # | Sistema | Clínico | Administración | Integraciones | Operación | Seguridad / cumplimiento | Comercial y adopción |
|---|---|---|---|---|---|---|---|
| 1 | Bilog | Ficha, historia, odontograma, evolución, imágenes con apoyo IA, presupuestos [C]; anamnesis solo Premium [C]; periodontograma, plan: **NE** | Caja, pagos, obras sociales, liquidaciones, KPIs [C]; factura electrónica vía Facturante [C]; señas, MP: **NE** | Recordatorios WhatsApp, link/QR, firma electrónica, radiología/iAngela [C]; API, WhatsApp Business API, Google, MP, ARCA directa: **NE** | Sucursales ilimitadas (Premium), roles por perfil y sucursal [C]; auditoría y exportación: **NE**; migración, capacitación, soporte local [C] | Backup 24/7, sensibilidad protegida, control de acceso [C]; **declara Ley 25.326/25.506/26.529** en términos [C]; auditoría: **NE** | Freemium 1 profesional/50 pacientes [C]; Lite y Premium **a cotizar** [C]; cifras de adopción **contradictorias** entre páginas oficiales [A] |
| 2 | Órbita | Odontograma dictado por voz, perio 6 sitios, historia firmada con matrícula y versiones, RX, consentimientos, presupuestos y planes [C] | Caja por sede con arqueo, **Mercado Pago**, factura ARCA y Libro IVA, convenios/OS con presentación por lotes, liquidaciones, Excel [C] | **WhatsApp API Meta oficial**, AWS, ARCA, MP, captación web/ads, firma con matrícula [C]; Google, API pública, PACS: **NE** | Sedes/cajas separadas, 5 perfiles y permisos, log de accesos, exportación total, migración [C]; soporte por plan, 45 tutoriales [C] | 2FA, registro de apertura/exportación, evolución firmada con historial, acceso del paciente art. 14 Ley 26.529 [C]; Ley 25.326 y backup: **NE** | `/precios` muestra **«precio a cotizar»** [C]; costos Meta visibles (USD 0,19/paciente atendido, 0,026/respuesta, 0,0618/recontacto) [C]; métricas 1.219/7 de 10 **autopublicadas** [A] |
| 3 | Dentalink | Historia, odontograma/periodontograma, imágenes, videos 3D, consentimientos, ortodoncia, IA en RX y firma como adicional [C]; anamnesis estructurada: **NE** | Pagos presenciales/online, créditos, cuotas, caja, convenios y OS, inventario, comisiones, reportes y Excel [C]; señas, MP, ARCA en AR: **NE** (facturación electrónica documentada solo MX/CO) | Google Calendar **unidireccional** [C], API de pago [C], WhatsApp Business, Contact Center IA [C]; MP, ARCA, PACS: **NE** | Usuarios ilimitados, permisos por rol, multicentro, descarga de información [C]; soporte por plan [C] | AWS, cifrado y backups diarios [A]; ISO 27001 y «HIPAA practices» [A]; DPA publicado [C]; Ley 25.326: **NE** | USD 35/50/75 por profesional [C], «desde», recurrentes, sin permanencia [C]; Contact Center USD 50 con 80 conversaciones [C]; Capterra 5,0 sobre **2 reseñas** [C]; +15.000 clientes [A] |
| 4 | DentalTec | Historia digital, odontograma interactivo CIE-10, imágenes [C]; anamnesis, perio, planes, presupuestos, RX: **NE** | **Validación de prácticas**, facturación planillas + ARCA, refacturación, liquidaciones, cuenta corriente, PDF/Excel [C]; caja, señas, MP: **NE** | **+15 OS vía SOAP**, ARCA, WhatsApp [C]; Google, API, MP, RX, firma: **NE** | 10 roles, auditoría «quién/qué/cuándo/dónde», JWT + bcrypt [C]; sucursales y exportación: **NE**; soporte 24/7 [A] | Trazabilidad y control de acceso [C]; cifrado en reposo, backup, Ley 25.326/26.529: **NE** | Demo gratis [C]; suscripción **NE**; packs WhatsApp USD 9/25/48 [C]; −64 % débitos, 2.000+ profesionales [A] |
| 5 | DentalCore | 15 módulos, odontograma 50+ hallazgos, perio 6 sitios (clasificación 2018), RX, anamnesis versionada, 22 consentimientos, planes en 8 fases, presupuestos [C]; receta ReNaPDiS **en trámite** [C] | Factura ARCA A/B/C/E con CAE y QR, caja con arqueo, **MP QR/link**, 65 OS (14 con nomenclador), liquidaciones, analíticas [C]; señas: **NE** | MP, WhatsApp API Meta, ARCA, **importación HL7 FHIR R4/R5 y CSV**, firma [C]; Google, API pública, PACS: **NE** | Varias clínicas, 4 roles, «odontólogo sin acceso», auditoría por ficha con IP/dispositivo, exportación PDF/FHIR [C]; soporte lun-vie [C]; Academy [C] | **Registro AAIP RL-2026-29513183**, Ley 25.326 art. 12 y Disp. 60-E/2016, 2FA, cifrado, backup, rol de encargado [C]; Ley 26.529 [C] | USD 50/mes 2 usuarios + USD 10 extra (AR$ 75.000 + 15.000) [C], 1 semana gratis, migración sin costo [C]; SMS extra USD 0,35 [C]; adopción **NE**, 0 reseñas G2 [C] |
| 6 | AgendaPro | Anamnesis, evoluciones, odontograma, RX, consentimiento y firma electrónica [C]; perio: **NE** | Caja, presupuestos, **señas/cuotas con MP**, honorarios, reportes [C]; facturación electrónica «próximamente» [A]; OS y ARCA: **NE** | Google Calendar, Google Reserve, MP, API (USD 200/mes), WhatsApp/Instagram/Messenger, CENS/FONASA [C/A] | Multisede, reportes consolidados, acceso por profesional, soporte diario [C]; auditoría/exportación: **NE** | AWS, cifrado, backups, **declara Ley 25.326 y 26.529** [A/C] | 14 días gratis; «desde $13.900 ARS» en dental vs $7.900/$15.900/$69.900 en `/planes`; IA +$15.000/profesional [C]; 30.000 clínicas/135.000 profesionales [A] |
| 7 | DentalSoft | Ficha, odontograma FDI 18 estados con historial, ortodoncia, imágenes y .stl, presupuestos, planes, recetas [C]; perio: **NE** | Caja/cierres, cobros, **MP en Pro**, señas, OS y liquidaciones, PDF/Excel [C]; ARCA: **NE** | Google Calendar, WhatsApp Meta (USD 0,026/mensaje), MP [C]; API, ARCA, firma: **NE** | Usuarios por rol, multisede, migración asistida, soporte [C]; auditoría: **NE** | Multi-tenant aislado, TLS, hash, logs, Ley 25.326 y AAIP [C]; Ley 26.529: **NE** | 3 meses gratis (50 turnos), Gestión $30.000, Pro $60.000 [C]; 5,0/11 reseñas de Google en sitio propio [C/A] |
| 8 | Odonthia | Ficha, odontograma FDI, historia versionada, perio 6 sitios, endodoncia, implantes, fotos, visor 3D, estética, presupuestos [C] | Caja, reportes, OS **sin liquidación**, comisiones, laboratorio, CSV [C]; MP y ARCA: **NE** | Google/Apple/Outlook, Claude/ChatGPT [C]; WhatsApp Business API y ARCA: **NE** | Roles, sedes, chat interno (pago), exportación/importación [C]; auditoría: **NE** | Firma con IP/dispositivo/hash y documento bloqueado [C]; validez legal no certificada; normativa AR: **NE** en producto (sí en privacidad [C]) | **Gratis ilimitado** [C]; IA $76.600 o $74.500 o $20.900 según página oficial, Max $158.100/$153.700/$46.900 [C] — **tres versiones** |
| 9 | DentiClick | Odontograma por caras con versiones, anamnesis, alertas, RX/visor 3D, presupuestos, recetas; perio e implantes según App Store [C] | Caja, cuenta corriente, pagos/recibos, OS con nomenclador y CSV/PDF [C]; MP y ARCA: **NE** | Mini-web propia, IA [C]; WhatsApp API, Google, MP, ARCA: **NE** | Multi-profesional, roles, sucursales (Centro), backup semanal [C]; auditoría: **NE** | Supabase/Vercel fuera de AR, cifrado, copias diarias, aislamiento, Ley 25.326/26.529 y firma 25.506 [C] | Gratis 1 profesional/30 pacientes; Consultorio US$12, Clínica US$29, Centro US$35 [C]; sin ratings suficientes [C] |
| 10 | Odonto.ar | Historia, odontograma con autor/fecha, RX, OS, presupuestos [C]; perio: **NE** | Panel de finanzas, nomenclador personalizable, importación Excel [C]; caja, facturación, MP pacientes: **NE** | MP **solo para la suscripción** [C]; WhatsApp, Google, ARCA, APIs: **NE** | Hasta 10 usuarios, permisos, mensajería interna [C]; auditoría/exportación: **NE** | Supabase, cifrado en tránsito/reposo, RLS, Ley 25.326 y AAIP [C] | 30 días gratis; US$29/49/99 pagaderos en ARS por MP, renovación manual [C]; adopción **NE** |
| 11 | DenPro | Historia, odontograma FDI, perio, imágenes, planes y presupuestos [C/A] | Facturación automática, pagos online por link, planes, multimoneda, exportación [C/A]; MP, OS argentinas, ARCA: **NE** | Contabilidad (Pohoda, Money S3, DATEV), terminal [A]; WhatsApp, Google, ARCA: **NE** | Usuarios ilimitados en Team, control de acceso, migración 1-3 días [C/A]; auditoría [A] | AES-256, centros ISO 27001 UE, RGPD [A]; Ley 25.326/26.529: **NE** | 30 días gratis; $19.900/$29.900, −15 % anual [C]; «50+ consultorios» [A] |
| 12 | Dentiqa | 8 pestañas, odontograma versionado, perio BOP/NIC, planes, evoluciones con firma, imágenes [C/A] | Facturación por profesional y estadísticas; **«no procesa pagos de pacientes: solo registra cobros»** [C]; MP es para la suscripción; ARCA: **NE** | WhatsApp Business API oficial (Violet Wave verificado por Meta), Claude Haiku/Sonnet [C/A]; Google, ARCA, RX: **NE** | 4 roles, multiclínica Enterprise, implementación y capacitación, migración, audit logs [C/A] | TLS, AES-256-GCM, tenant aislado, backups, audit logs [C/A]; Ley 26.529 invocada [A], 25.326: **NE** | 14 días gratis; US$89/149/249 en ARS vía MP [C]; «clínicas fundadoras» [A] |
| 13 | Benty | Historia, odontograma con gráfico/dictado, perio, imágenes, planes, laboratorio, recetas digitales [C/A] | Presupuestos, liquidaciones de staff, pagos y facturación electrónica [C/A]; OS [A en terceros], MP, ARCA: **NE** | WhatsApp [C/A]; Google, API, MP, ARCA: **NE** | Roles/permisos, licencias, liquidaciones, backups, migración asesorada [C/A] | Ley 25.326 y 26.529, usuario/contraseña, backups [C] | **Precios no publicados** [C]; 1 mes bonificado [C]; adopción **NE** |
| 14 | Doctocliq | Odontograma FDI norma MINSA, perio (movilidad, sangrado, placa, sondaje), anamnesis, evolución, imágenes, presupuestos por fases [C/A] | Caja, multicajas, comisiones, reportes; link de pago **solo Perú**; facturación electrónica **solo PE/MX/CO/EC/RD** [C] | WhatsApp Business (módulo Marketing), API de citas, importación Google Calendar, Kommo [C]; MP/facturación AR: **NE** | Sucursales como adicional, 8 usuarios gratuitos, permisos solo Avanzado, migración asistida [C] | Cifrado/respaldo **NE**; cumple normativas fiscales de los países listados, no Ley 25.326 [C/NE] | Gratis 30 pacientes/citas; US$19/29/49 [C], promo hasta 30 % [C]; módulos adicionales [C]; +4.000 profesionales [A] |
| 15 | Dentidesk | Odontograma (nomenclatura chilena/internacional/universal), perio, endodoncia, ortodoncia, imágenes, presupuestos [C]; consentimiento «en camino» [A] | Facturación integrada al SII chileno, >20 reportes, caja, inventario, comisiones [A] | Integraciones «en un solo lugar» sin detalle [A]; API, Google, WhatsApp API, pagos: **NE** | Multisucursal, usuarios administrativos ilimitados, cargos por odontólogo [C/A] | «Estrictas medidas de seguridad» [A]; cifrado, respaldo, auditoría, Ley 25.326: **NE** | **Sin precio público**, cotización, trial 15 días [C]; «número 1 en LATAM» [A] |
| 16 | Clinic Cloud | Historia, plantillas con odontograma y perio **solo en Max**, firma certificada en Max, recetas REMPe, consentimientos, imágenes [C] | TicketBai/Verifactu, series, caja, presupuestos, liquidaciones, stock, TPV [C]; **ninguna aplicable a Argentina** | Doctoralia nativa, videoconsulta, Noa [C]; API y WhatsApp Business API: **NE** | Sin permanencia, almacenamiento 4-10 GB, soporte Premium en Max, franquicias [C/A] | Cifrado, copias, control de accesos, servidores UE, RGPD y Ley 41/2002; ISO 27001 [A]; Ley 25.326: **NE** | 29/49/79 EUR + IVA (promo 14,5/24,5/39,5) [C]; Odontograma recién en plan Max [C]; >1.000 clínicas [A] |
| 17 | Open Dental | Tooth Chart 2D/3D, planes en portal, imágenes, eClipboard, Web Forms, eRx, IA en imágenes [C] | Billing/claims USA, Payment Portal, PayConnect/PaySimple [C]; facturación AR: **NE** | eServices/eConnector, procesadores US [C]; API pública, WhatsApp, MP, Google: **NE** | Multi-sede con descuento, ODTouch/ODMobile, soporte [C]; exportación: **NE** | Enfoque HIPAA, cifrado documentado [C]; BAA y Ley 25.326: **NE** | USD 199→149/mes EE.UU., **USD 89 otros países**, eServices 165, Web Sched 75 c/u, texto 0,04 [C]; trial y garantía 90 días [C] |
| 18 | Curve Dental | Charting, perio gráfico, imágenes FDA-cleared, planes con eSignature, e-Prescribe, Curve Go, IA ambiental [C/A] | Insurance Billing USA, Curve Pay, Apple/Google Pay, text-to-pay, BNPL, memberships [C/A] | Integraciones propias [C]; API, WhatsApp, facturación/radiología local: **NE** | Multi-sede, Curve Chat, app móvil, soporte 24/7/365, 8 sesiones [C/A] | HIPAA y ciberseguridad declarados, BAA, GDPR [A/C]; Ley 25.326: **NE**; «U.S.-only» [C] | **Sin precio público** [C]; G2 ease 92 %, soporte 90 % [A-AP]; 100.000+ profesionales [A] |
| 19 | tab32 | Charting, odontograma, planes, voice perio, radiología cloud con IA (PiAI/Pearl), ePrescribe [C/A] | RCM USA: eligibility, claims, ERA/EOB, Stripe, planes de pago; tarifas live USD 0,25 claim, 0,50 attach, 0,75 eligibility [C] | Stripe, DrFirst [C]; API, Google, WhatsApp, facturación AR: **NE** | Unlimited providers/users, migración cotizada (desde USD 1.250), exportación gratuita al salir [C] | HIPAA declarado, **SOC 2 verificado por tercero**, GDPR, BAA [C]; Ley 25.326: **NE** | **USD 95/mes** usuarios ilimitados, 14 días con tarjeta, sin setup [C]; agentes IA USD 25 c/u [C]; 1.000+ prácticas [A] |
| 20 | NexHealth | Forms digitales y iPad; treatment/payment plans posteados al ledger; **odontograma/imagen propios: NE** (depende del PMS) | Payments con link y auto-post, verificación de elegibilidad USA [C]; facturación AR: **NE** | **Synchronizer** con decenas de PMS sin costo, API [C]; WhatsApp/MP: **NE** | Multi-sede, onboarding white-glove, dashboard [C] | **HIPAA y cifrado en reposo [C]**; GDPR/BAA/Ley 25.326: **NE** | Sin precio público [C]; Synchronizer gratis, sin booking fees [A]; 20.000+ practices [A] |
| 21 | Weave | Formularios digitales; **ficha, odontograma e imágenes: NE** (no es PMS) [C] | Text-to-Pay, Online Bill Pay, BNPL, planes, recordatorios de cobro [C]; facturación AR: **NE** | Write-back al PMS, VoIP propio [C]; API, WhatsApp, MP: **NE** | Hasta 15 teléfonos, IVR, colas, app, onboarding 1:1 [C] | HIPAA/BAA **NE** en páginas consultadas [NE] | Desde USD 199/mes; SMS 1.500/3.000/15.000 [C]; «9 pacientes puntuales más por semana» [A] |
| 22 | BlueDental | Odontograma visual por diente/cara/arcada, prontuário, planejamento, orçamento desde el odontograma [A] | Financiero, contas a pagar/receber, comisiones, estoque, recibos [A] | WhatsApp Web vinculado (no API documentada) [A]; API, pagos, RX, firma: **NE** | Multiclínica, niveles de acceso, soporte lun-vie [A] | «Criptografia avançada e backups automáticos» [A]; LGPD/Ley 25.326: **NE** | «100 % gratuito» [A] vs Capterra US$ 5,98/mes y **0 reseñas** [C] — contradicción; +8.500 usuarios [A] |

---

# B. Matriz de puntuación (0-5)

Pesos definidos por el requerimiento: turnos y automatización **25 %**; funcionalidad odontológica clínica **20 %**; integraciones locales y WhatsApp **15 %**; administración, cobros y facturación **15 %**; experiencia del paciente **10 %**; seguridad, exportación y trazabilidad **10 %**; precio y facilidad de adopción **5 %**.

**Regla de puntuación:** se puntúa solo lo públicamente demostrado. `No evidenciado` se penaliza conservadoramente; una afirmación comercial [A] no equivale a funcionalidad comprobada. Puntajes provisionales, sensibles a verificación en demo. Diferencias inferiores a 0,3 puntos no son significativas.

| # | Sistema | Turnos 25 % | Clínica 20 % | Integr. 15 % | Admin 15 % | Paciente 10 % | Segur. 10 % | Precio 5 % | **Total** |
|---|---|---|---|---|---|---|---|---|---|
| 2 | Órbita | 4,5 | 4,5 | 4,0 | 4,0 | 4,5 | 3,5 | 2,0 | **4,1** |
| 5 | DentalCore | 3,5 | 4,5 | 4,0 | 4,5 | 3,0 | 4,5 | 4,5 | **4,0** |
| 7 | DentalSoft | 4,0 | 4,5 | 3,5 | 4,0 | 4,0 | 4,0 | 4,0 | **4,0** |
| 3 | Dentalink | 4,5 | 4,5 | 3,5 | 3,5 | 4,0 | 3,5 | 3,5 | **4,0** |
| 6 | AgendaPro | 4,5 | 3,5 | 4,0 | 3,5 | 4,5 | 3,5 | 3,0 | **3,9** |
| 1 | Bilog | 4,0 | 4,5 | 3,0 | 4,0 | 4,0 | 3,0 | 3,0 | **3,8** |
| 20 | tab32 | 4,5 | 4,0 | 2,5 | 3,0 | 4,0 | 4,0 | 4,5 | **3,8** |
| 14 | Doctocliq | 4,0 | 4,5 | 3,0 | 3,0 | 4,0 | 2,5 | 4,5 | **3,7** |
| 18 | Curve Dental | 4,5 | 4,5 | 2,0 | 3,0 | 4,5 | 3,0 | 2,5 | **3,7** |
| 17 | Open Dental | 4,5 | 4,5 | 2,0 | 3,0 | 3,5 | 3,5 | 3,0 | **3,6** |
| 21 | NexHealth | 5,0 | 2,0 | 4,0 | 2,5 | 5,0 | 3,5 | 2,5 | **3,6** |
| 13 | Dentiqa | 3,5 | 4,0 | 3,5 | 2,5 | 3,5 | 3,5 | 3,5 | **3,5** |
| 4 | DentalTec | 3,0 | 3,5 | 4,0 | 4,5 | 2,5 | 3,5 | 2,5 | **3,5** |
| 16 | Clinic Cloud | 4,0 | 3,5 | 2,0 | 3,0 | 4,5 | 3,5 | 4,0 | **3,5** |
| 8 | Odonthia | 3,5 | 4,5 | 2,5 | 2,5 | 3,5 | 3,5 | 3,5 | **3,4** |
| 9 | DentiClick | 3,5 | 4,0 | 2,0 | 3,0 | 3,5 | 3,5 | 4,5 | **3,4** |
| 11 | DenPro | 3,5 | 4,0 | 2,0 | 3,0 | 3,0 | 3,0 | 4,0 | **3,2** |
| 15 | Dentidesk | 2,5 | 4,5 | 1,5 | 4,0 | 2,5 | 2,5 | 2,0 | **3,0** |
| 12 | Benty | 3,0 | 3,5 | 2,0 | 3,0 | 3,0 | 3,0 | 2,0 | **2,9** |
| 10 | Odonto.ar | 3,0 | 3,5 | 1,5 | 2,0 | 3,0 | 3,5 | 4,0 | **2,8** |
| 19 | Weave | 4,0 | 1,0 | 3,0 | 2,5 | 4,5 | 2,0 | 2,5 | **2,8** |
| 22 | BlueDental | 2,5 | 3,5 | 1,5 | 3,0 | 3,0 | 2,0 | 5,0 | **2,8** |

**Lectura prudente.** El agrupamiento en 3,4–4,1 es más informativo que el orden exacto: las diferencias dentro del grupo están dentro del margen de error de una evaluación basada solo en fuentes públicas. Bilog lidera en *evidencia pública y adopción declarada*, no necesariamente en calidad de producto. DentalCore y DentalSoft destacan por *precio y cumplimiento documentado*; Órbita y Dentalink por *automatización*; NexHealth, Curve y tab32 son referencias de *diseño de reserva*, no productos adoptables en Argentina. Weave y BlueDental puntúan bajo por su naturaleza (capa de comunicación y producto gratuito, respectivamente), no por falta de valor en su nicho.

---

# C. Análisis competitivo

## C.1 Funcionalidades ya estándar de mercado

Con evidencia comprobada en múltiples proveedores de la muestra:

- Agenda por profesional —y con frecuencia por sillón— con vistas día/semana, drag & drop y estados.
- Odontograma digital con historia, imágenes y presupuestos; historia clínica electrónica con evoluciones.
- Recordatorios y confirmaciones automáticos por WhatsApp, email o SMS; ya sea vía API oficial (Órbita, Dentalink, DentalCore, Dentiqa) o enlace manual (Dentidesk, BlueDental).
- Link de reserva online y portal del paciente, presente incluso en planes de entrada (Dentalink, Odonthia, DentiClick, Odonto.ar).
- Caja, reportes y exportación; roles y permisos por perfil.
- Prueba gratuita o plan gratuito como mecanismo estándar de entrada (Bilog, Odonthia, DentiClick, Doctocliq, BlueDental, Dentalink, tab32).
- Facturación **local al país de origen**: SII en Dentidesk (CL), TicketBai/Verifactu en Clinic Cloud (ES), facturación PE/MX/CO/EC/RD en Doctocliq, ARCA en DentalTec, DentalCore y Órbita.

**Conclusión:** agenda + odontograma + recordatorios + caja ya no diferencian. Son requisitos de entrada.

## C.2 Funcionalidades diferenciadoras reales

Solo cuando hay evidencia concreta; se distingue el hecho de la promesa:

| Diferencial | Quién lo demuestra | Naturaleza de la evidencia |
|---|---|---|
| Validación y liquidación de obras sociales | **DentalTec** (+15 OS vía SOAP, refacturación, liquidaciones); **Bilog**; módulo AR de **Dentalink** | [C] en DentalTec y Bilog; principal foso local |
| IA conversacional con handoff clínico | **Órbita** «Juez IA» deriva ante dolor, sangrado o postoperatorio; **Dentalink** Contact Center IA | [C] derivación documentada en notas TN (16/07/2026) y Ámbito (28/07/2026); eficacia [A] |
| Odontograma dictado por voz + evolución firmada con matrícula | **Órbita** | [C] en funcionalidades; el dictado por box tiene costo adicional |
| Reserva con *write-back* real y anti-doble-reserva | **Open Dental Web Sched**, **NexHealth Synchronizer**, **LocalMed**, **tab32** | [C] patrón construido sobre PMS; no existe igual en Argentina con PMS local |
| Relleno inteligente de huecos (*smart fill* / waitlist con depósito) | **Curve Smart Fill**, **Weave QuickFill**, **NexHealth waitlist + depósitos**, **Órbita lista de espera** | [C] en Curve/NexHealth; en Órbita es lista con orientación, no relleno autónomo [A] |
| Interoperabilidad clínica | **DentalCore** con importación HL7 FHIR R4/R5 y exportación | [C] único caso explícito en la muestra |
| Cumplimiento documentado | **DentalCore** con registro AAIP RL-2026-29513183 y rol de encargado; **Bilog**, **Odonthia**, **AgendaPro** declaran Ley 25.326 | [C] declaración/registro; **no equivale a auditoría pública** |

## C.3 Vacíos frecuentes del mercado argentino

Corregidos con la evidencia de la sección preliminar:

1. **Transparencia y estabilidad de precios — vacío real.** Odonthia publica tres precios incompatibles; AgendaPro muestra $13.900 en la página dental y $7.900/$15.900/$69.900 en `/planes`; Órbita pasó a «precio a cotizar» y factura costos de Meta aparte; DentalTec separa WhatsApp en packs USD 9/25/48. No es falta de precios: es falta de **precio predecible**.
2. **Métricas verificables de ocupación y ausentismo — vacío real.** Todos prometen reducciones (−64 % débitos, −40 % ausentismo, «hasta 90 % menos ausencias»), ninguno publica metodología, muestra ni período. La oportunidad es entregar métricas auditables por sillón y clínica.
3. **Seguridad como evidencia, no como adjetivo — vacío real.** Hay declaraciones de Ley 25.326, ISO 27001 e HIPAA, pero no se encontraron informes de auditoría, inscripciones RNBD por proveedor ni cláusulas AAIP 198/2023 publicadas para subprocessors (AWS, Meta, Claude). El marco normativo exige más: Ley 25.326 (arts. 2, 7, 9, 12, 21, 25), Ley 26.529 (art. 14, copia en 48 h) y Decreto 1089/2012 (conservación por 10 años, firma electrónica Ley 25.506).
4. **Handoff clínico auditado — vacío parcial.** Existe derivación, pero no se evidencia trazabilidad de la derivación dentro de la historia clínica ni override humano registrado.
5. **Relleno automático integral de cancelaciones — vacío parcial.** Órbita publica lista de espera y horarios que completan el día [A]; Curve y NexHealth lo demuestran con waitlist y depósito en otros mercados. Falta la combinación waitlist + depósito + liberación automática + conciliación ARCA en un mismo producto argentino.
6. **Lo que ya NO es vacío (corrección):** seña con Mercado Pago y declaración de Ley 25.326. Deben darse por cubiertos y competirse en ejecución, no en novedad.

## C.4 Oportunidades de innovación

1. **Cumplimiento demostrable como funcionalidad de producto.** Dossier descargable con DPA responsable/encargado, cláusulas AAIP 198/2023 para subprocessors, RNBD, roles, logs inmutables, exportación en 48 h, retención 10 años, firma electrónica y procedimiento INTIMA/CONSTA del pagador. Diferencia frente a «somos seguros».
2. **Precio todo-incluido y predecible.** Un solo plan con recordatorios, seña, respuesta IA y handoff incluidos; sin packs de mensajes ni módulos sorpresa. Ataca la principal irritación observable.
3. **Motor de ocupación.** Waitlist con relleno automático, depósito configurable, regla de liberación por 48 hs hábiles, conciliación con Mercado Pago y comprobante ARCA. Integra lo que hoy está repartido entre tres proveedores.
4. **IA con seguridad clínica auditable.** Reglas explícitas de derivación ante síntomas, registro de cada handoff en la historia, override humano y métricas de precisión publicadas.
5. **Foso local de integración.** Conector con WhatsApp Business API + ARCA + obras sociales + FHIR, con exportación gratuita. Ningún competidor reúne las cuatro con evidencia pública.

---

# D. Recomendación final

## D.1 Cinco competidores prioritarios para análisis mediante demo

| Orden | Producto | Hipótesis a validar en la demo |
|---|---|---|
| 1 | **Bilog** | Si su circuito clínico-administrativo y la liquidación de obras sociales justifican el liderazgo declarado; verificar precios reales, exportación y auditoría, y la contradicción entre cifras de adopción de sus propias páginas. |
| 2 | **Órbita** | Si el chatbot con derivación clínica y el odontograma por voice efectivamente reducen trabajo de recepción; confirmar precio vigente, costos Meta y si la lista de espera rellena huecos sin intervención. |
| 3 | **Dentalink** | Si el módulo Argentina factura y liquidan realmente en ARCA/obras sociales; medir Contact Center IA (USD 50 por 80 conversaciones) y la API de pago. |
| 4 | **DentalCore** | Si el cumplimiento documentado (AAIP, FHIR, 2FA, exportación) y la facturación ARCA + 65 obras sociales son reales en la operación; probar receta electrónica en trámite y ausencia de portal de paciente. |
| 5 | **DentalTec** | Si la validación en tiempo real contra +15 obras sociales y la refacturación son verificables; confirmar agenda digital pública (hoy no evidenciada) y costo real de suscripción. |

**Suplente:** DentalSoft, por tener precios públicos en ARS y señas con Mercado Pago documentadas; útil como benchmark de transparencia.

## D.2 Tres productos de referencia para experiencia de usuario

1. **Órbita** — flujo WhatsApp → turno con audios, botones de confirmación y derivación humana. Referencia de conversación, no de facturación.
2. **Curve Dental** — *self-scheduling* 24/7 con permisos por tipo de cita y **Smart Fill**. Referencia de ocupación y de reserva sin fricción.
3. **NexHealth** — reserva 24/7 real con *write-back* al PMS, depósitos y waitlist; sin necesidad de portal. Referencia de arquitectura de agendamiento.

**Secundarias:** Doctoralia/Clinic Cloud para el patrón *marketplace de captación + PMS*, y Calendly para UX genérica de booking (sin valor clínico).

## D.3 MVP sugerido

### Imprescindible

- Agenda multiprofesional y multisillón, con duración variable, bloqueos, sobreturnos, prevención de solapamientos y bloqueo en base de datos.
- Reserva online por link/QR con confirmación, cancelación y reprogramación, con **ventana de 48 hs hábiles** y *write-back* real a la agenda.
- WhatsApp Business API con plantillas aprobadas, recordatorios y **handoff humano registrado**.
- Ficha clínica, anamnesis, odontograma FDI con dientes temporales, periodontograma, presupuestos y evolución firmada con matrícula.
- Caja, señas con Mercado Pago, facturación ARCA y conciliación.
- Roles y permisos, auditoría, exportación completa en 48 h, retención de 10 años y copia de historia conforme a la Ley 26.529.
- Cumplimiento de Ley 25.326 y Decreto 1089/2012 con DPA y registro AAIP del responsable.

### Diferenciadores

- **Plan todo-incluido** con recordatorios, seña, respuesta IA y handoff sin add-ons ni packs.
- **Motor de ocupación**: waitlist con relleno automático, depósito configurable y métricas de ocupación por sillón.
- **Dossier de cumplimiento** descargable y trazabilidad de cada derivación clínica en la historia.
- **Precio público, único y estable**, con costos de API detallados por separado.

### Etapas posteriores

- Liquidación y validación de obras sociales y prepagas (alto costo de integración; foso de los incumbentes).
- Facturación avanzada y receta electrónica con validez legal (ReNaPDiS).
- Periodontograma con curvas, radiología/PACS e imágenes 3D.
- API pública, Google Calendar bidireccional e interoperabilidad FHIR completa.
- IA conversacional amplia con reglas clínicas auditables y multisede avanzada.

---

# Fuentes consultadas

Todas las URLs fueron consultadas el **19 de noviembre de 2026**. Se agrupan por sistema; cuando una afirmación del texto tiene una fuente específica, el apartado A la identifica por su contenido.

| Sistema | Fuentes oficiales y verificables |
|---|---|
| Bilog | https://bilog.com.ar/ · https://bilog.com.ar/funcionalidades · https://docs.bilog.com.ar/schedule/online-appointment · https://bilog.com.ar/planes · https://bilog.com.ar/seguridad · https://bilog.com.ar/electronic-invoice · https://www.bilog.com.ar/payments · https://bilog.com.ar/app-mobile · https://bilog.com.ar/llms.txt · https://bilog.com.ar/terms_and_conditions/ |
| DentalTec | https://web.dentaltec.com.ar/ · https://web.dentaltec.com.ar/funcionalidades · https://web.dentaltec.com.ar/preguntas-frecuentes · https://web.dentaltec.com.ar/casos-de-exito · https://web.dentaltec.com.ar/software-odontologos-argentina · https://web.dentaltec.com.ar/mejor-software-odontologico-argentina |
| Órbita | https://hiorbita.com/ · https://hiorbita.com/erp · https://hiorbita.com/erp/funcionalidades · https://hiorbita.com/erp/agenda · https://hiorbita.com/erp/funcionalidades/la-consulta · https://hiorbita.com/precios · https://hiorbita.com/comparativa · https://hiorbita.com/consultorios · https://hiorbita.com/nosotros · https://hiorbita.com/ayuda/turnos-en-serie.pdf · https://tn.com.ar/tecno/novedades/2026/07/16/tres-jovenes-argentinos-crearon-una-ia-que-responde-consulta-y-gestiona-turnos-medicos-por-whatsapp/ · https://www.ambito.com/tecnologia/desarrollo-nacional-crearon-una-ia-argentina-que-organiza-turnos-medicos-whatsapp-y-ya-se-prueba-consultorios-n6304027 |
| Dentalink | https://www.softwaredentalink.com/ar · https://www.softwaredentalink.com/ar/planes · https://www.softwaredentalink.com/ar/experiencia-de-pacientes/agenda · https://softwaredentalink.com/blog/recordatorio-turnos-whatsapp · https://ayuda.softwaredentalink.com/es/articles/9493145-integra-dentalink-con-google-calendar · https://ayuda.softwaredentalink.com/es/articles/9493507-integracion-api · https://www.softwaredentalink.com/inteligencia-artificial/contact-center · https://www.softwaredentalink.com/ar/promociones · https://www.capterra.com/p/196361/Dentalink/ |
| DentalCore | https://dentalcore.app/ · https://www.dentalcore.app/features · https://dentalcore.app/pricing · https://dentalcore.app/software-dental/argentina · https://dentalcore.app/privacidad · https://dentalcore.app/about · https://www.capterra.com/p/10047210/Dentalcore/ · https://www.g2.com/products/dentalcore/reviews |
| AgendaPro | https://agendapro.com/ar · https://agendapro.com/ar/dental/software-odontologico · https://agendapro.com/ar/planes · https://apps.apple.com/ar/app/agendapro-business/id1218956898 |
| DentalSoft | https://www.dentalsoft.com.ar/ · https://www.dentalsoft.com.ar/funcionalidades · https://www.dentalsoft.com.ar/funcionalidades/historia-clinica-odontograma · https://www.dentalsoft.com.ar/funcionalidades/obras-sociales · https://www.dentalsoft.com.ar/funcionalidades/cobros-mercadopago · https://www.dentalsoft.com.ar/funcionalidades/mercadopago · https://www.dentalsoft.com.ar/privacidad |
| Odonthia | https://odonthia.com/ · https://odonthia.com/funcionalidades · https://odonthia.com/planes · https://odonthia.com/llms.txt · https://odonthia.com/reembolsos · https://odonthia.com/docs/conceptos · https://odonthia.com/legal/privacidad · https://odonthia.com/cambiar-de-sistema |
| DentiClick | https://denticlick.app/ · https://denticlick.app/privacidad · https://apps.apple.com/af/app/denticlick/id6809206464 |
| Odonto.ar | https://odonto.ar/ · https://odonto.ar/terminos · https://odonto.ar/privacidad |
| DenPro | https://www.denpro.ar/acerca-de/ · https://www.denpro.ar/soluciones/ · https://www.denpro.ar/caracteristicas/sistema-de-citas-para-clinicas-dentales/ · https://www.denpro.ar/caracteristicas/odontograma-digital-e-historia-dental/ · https://www.denpro.ar/caracteristicas/facturacion-para-consultorios-dentales/ · https://www.denpro.ar/precios/ |
| Dentiqa | https://dentiqa.app/ar · https://dentiqa.app/software-dental · https://dentiqa.app/gestion-clinica-dental · https://dentiqa.app/whatsapp-clinica-dental · https://dentiqa.app/guias/gestion-integral-clinica-dental · https://dentiqa.app/legal/terminos-de-servicio |
| Benty | https://benty.com.ar/ · https://benty.com.ar/terms/ · https://benty.com.ar/software-odontologico/ · https://benty.com.ar/funcionalidades/agenda-y-turnos/ · https://benty.com.ar/funcionalidades/autogestion-de-pacientes/ · https://benty.com.ar/funcionalidades/menos-ausentismo/ · https://benty.com.ar/funcionalidades/facturas-electronicas/ · https://benty.com.ar/funcionalidades/integrante-del-staff/ |
| Doctocliq | https://www.doctocliq.com/ · https://www.doctocliq.com/planes-y-precios · https://www.doctocliq.com/software-dental-gratis-doctocliq · https://www.doctocliq.com/funcionalidades · https://www.doctocliq.com/funcionalidades/marketing · https://www.doctocliq.com/pe/software-dental · https://www.doctocliq.com/preguntas-frecuentes |
| Dentidesk | https://www.dentidesk.cl/ · https://app.dentidesk.cl/terminos.php · https://www.dentidesk.cl/dentidesk/detail/4 · https://www.dentidesk.cl/dentidesk · https://www.dentidesk.com/dentidesk/feature |
| Clinic Cloud | https://clinic-cloud.com/tarifas · https://pro.doctoralia.es/precios/para-clinicas · https://clinic-cloud.com/faq/doctoralia/clinic-cloud-y-doctoralia |
| BlueDental | https://www.bluedental.com.br/ · https://valor.globo.com/patrocinado/dino/noticia/2026/07/16/software-odontologico-gratuito-atinge-mais-de-8500-usuarios-1.ghtml · https://www.capterra.com.br/software/209573/bluedental |
| Open Dental | https://www.opendental.com/ · https://opendental.com/site/order.html · https://www.opendental.com/manual/appointments.html · https://www.opendental.com/site/websched.html · https://www.opendental.com/site/eservices.html · https://opendental.com/site/fees.html · https://www.opendental.com/site/paymentportal.html |
| Curve Dental | https://www.curvedental.com/ · https://www.curvedental.com/pricing · https://www.curvedental.com/dental-scheduling-software · https://www.curvedental.com/blog/how-one-platform-powers-your-practice · https://www.curvedental.com/feature-overview · https://www.curvedental.com/curve-vs-open-dental |
| tab32 | https://tab32.com/ · https://tab32.com/pricing/ · https://tab32.com/enhance-the-patient-experience · https://tab32.com/platform · https://frontdeskreview.com/software/dental-software/tab32 |
| NexHealth | https://www.nexhealth.com/ · https://www.nexhealth.com/resources/nexhealth-fall-release-2026 · https://www.nexhealth.com/pricing · https://www.nexhealth.com/features/scheduling · https://www.nexhealth.com/frequently-asked-questions · https://www.nexhealth.com/features/nexhealth-online-booking · https://www.nexhealth.com/features/nexhealth-scheduling · https://www.nexhealth.com/features/synchronizer |
| Weave | https://www.getweave.com/dental · https://www.getweave.com/weave-scheduling · https://www.getweave.com/use-cases/patient-scheduling-software · https://www.getweave.com/weave-reminders · https://www.getweave.com/pricing |
| Calendly | https://calendly.com/scheduling · https://calendly.com/help/choose-the-right-calendly-plan-for-your-team · https://calendly.com/help/get-to-know-calendly · https://calendly.com/pricing?billing=annual · https://calendly.com/integration |
| Zocdoc / LocalMed | https://www.zocdoc.com/provider-help/en/articles/8843078-what-is-the-zocdoc-marketplace · https://www.zocdoc.com/provider-help/en/articles/10859404-understanding-zocdoc-pricing-and-billing · https://www.localmed.com/ · https://www.dentalintel.com/localmed |
| Normativa argentina | https://www.argentina.gob.ar/normativa/nacional/ley-25326-64790/texto · https://www.argentina.gob.ar/normativa/nacional/decreto-1558-2001-70368/texto · https://www.argentina.gob.ar/aaip/datospersonales · https://www.argentina.gob.ar/normativa/nacional/resolucion-198-2023-391538/texto · https://www.argentina.gob.ar/normativa/nacional/resoluci%C3%B3n-126-2024-399750/actualizacion · https://www.argentina.gob.ar/normativa/nacional/ley-26529-160432/texto · https://www.argentina.gob.ar/normativa/nacional/decreto-1089-2012-199296/texto · https://www.argentina.gob.ar/normativa/nacional/decreto-953-2024-405666 · https://www.arca.gob.ar/facturacion/mas-info/ · https://www.argentina.gob.ar/normativa/nacional/ley-23660-1988-62/texto · https://www.argentina.gob.ar/normativa/nacional/ley-23661-63/actualizacion · https://www.argentina.gob.ar/normativa/nacional/ley-26682-182180/texto · https://www.argentina.gob.ar/noticias/la-superintendencia-fortalece-el-sistema-de-denuncias-y-sanciones-para-garantizar |
| Otras fuentes de precios / señas | https://www.usarflap.com/mercadopago · https://www.dentapp.com.ar/mercadopago · https://miagendaprofesional.com.ar/mercadopago/ · https://www.softwaredoit.es/software-medico/software-clinica-dental.html · https://www.capterra.co.cr/software/196361/dentalink |
