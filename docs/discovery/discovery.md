# Discovery — turnos-odontologia

**Fecha**: 2026-11-19
**Fuentes investigadas**: 1 informe de mercado local (`C:\Users\Cedib\Downloads\InvMercadoOdont.pdf`), con 17 sistemas comparados, matriz de puntuación, análisis competitivo y MVP sugerido. No se invocó `web-scraper`: la fuente es un documento provisto por el usuario, no URLs.

## 1. Problema que resuelve

Es un problema dual. Por un lado, los pacientes deben llamar o mandar un mensaje para obtener, modificar o cancelar un turno odontológico, sin poder gestionarlo por sí mismos. Por el otro, los consultorios quieren centralizar su agenda y dejar de sufrir reservas dobles o pisadas entre profesionales y sillones.

## 2. Usuarios / roles

- **Paciente**: reserva, cancela y reprograma su turno.
- **Recepcionista**: gestiona la agenda del consultorio.
- **Odontólogo**: ve su agenda y la ficha clínica del paciente.

## 3. Casos de uso

Casos provisionales ("por ahora"): el MVP del informe, con permiso para crecer después.

1. Como paciente, quiero ver la disponibilidad para elegir un horario sin llamar.
2. Como paciente, quiero reservar por link o QR para confirmar mi turno al instante.
3. Como paciente, quiero cancelar o reprogramar para liberar el horario y recolocarme.
4. Como recepcionista, quiero ver la agenda por sillón para detectar y evitar solapamientos.
5. Como paciente, quiero recibir un recordatorio por WhatsApp para no olvidar el turno.

## 4. Competidores / soluciones existentes

El informe compara 17 sistemas, ordenados por relevancia para Argentina. Los cinco que recomienda analizar en demo:

| Sistema | Origen | Agenda y turnos | Diferenciador / foso | Precio |
|---|---|---|---|---|
| Bilog | AR | Vista diaria/semanal por profesional, arrastre de turnos, sobreturnos, reserva y cancelación online, alta por QR, asistente IA | Odontograma, historia clínica, firma electrónica y liquidación de obras sociales. +1.500 clínicas (autopublicado) | No publicado; freemium según fuente |
| Órbita | AR (La Plata) | Agenda por sillón; asistente de IA que atiende WhatsApp, da y confirma turnos, reprograma y deriva a humano | Odontograma dictado por voz, evolución firmada con matrícula. API oficial de WhatsApp | Sin precio publicado; demo |
| Dentalink | CL (Healthatom) | Confirmaciones y recordatorios automáticos | Módulo Argentina: obras sociales, recaudación, conciliaciones y receta electrónica | Desde US$ 29 (Capterra) |
| DentalTec | AR (San Juan) | Reserva online: no encontrado; recordatorios por WhatsApp | Validación de prácticas en tiempo real contra 15+ obras sociales, facturación automática ARCA | Pack de WhatsApp ~USD 9 |
| DentalCore | AR (La Plata) | Agenda; WhatsApp online: no encontrado | Odontograma con nomenclatura, obras sociales, facturación electrónica | USD 50/mes (2 usuarios) |

**Lectura del informe**: la validación y liquidación de obras sociales es el principal foso competitivo local (Bilog, DentalTec y, en menor grado, Dentalink). La IA conversacional en WhatsApp con derivación a humano ante urgencias (Órbita) es lo más novedoso del mercado argentino. Según el informe ampliado, los vacíos reales son: relleno automático de lista de espera con depósito, métricas verificables de ocupación y ausentismo, recuperación de pacientes, precios predecibles con recordatorios incluidos y cumplimiento demostrable (no solo declarado). La seña con Mercado Pago y la declaración de adecuación a la Ley 25.326 **ya están presentes** en competidores (DentalSoft, AgendaPro, Bilog, Odonthia, DentalCore), por lo que no deben figurar como vacíos. **Corrección vigente:** ver `docs/discovery/informe-discovery.md`, sección «Nota preliminar: correcciones al informe previo».

## 5. Funcionalidades necesarias

MVP sugerido por el informe, validado por el usuario:

- Agenda multiprofesional y multisillón, con duración variable, bloqueos, sobreturnos y prevención de solapamientos.
- Reserva online por enlace y QR, con confirmación, cancelación y reprogramación.
- WhatsApp Business API con plantillas para confirmación y recordatorio.
- Ficha clínica, anamnesis y odontograma FDI con dientes temporales.
- Presupuesto, caja y señas.
- Roles, auditoría, exportación completa de datos y respaldo.

## 6. Funcionalidades opcionales

Diferenciadores (no bloqueantes para el MVP):

- Relleno automático de lista de espera con depósito y métricas de ocupación por sillón (la seña con Mercado Pago ya es estándar y no es diferenciador).
- Recupero de ausentes y controles periódicos.
- Precio público, con recordatorios incluidos (sin packs aparte).

Etapas posteriores: liquidación y validación de obras sociales, facturación ARCA, periodontograma, radiología, API pública, Google Calendar e IA conversacional con derivación humana obligatoria ante síntomas.

## 7. Reglas de negocio

- No se pueden reservar turnos superpuestos en un mismo profesional o sillón.
- Duración variable por tipo de tratamiento, con bloqueos y sobreturnos permitidos.
- Cancelación configurable con **ventana de 48 hs hábiles**.
- Lista de espera para rellenar cancelaciones.
- Consentimientos con firma electrónica y evolución firmada con matrícula.
- Horarios laborales y feriados del consultorio.

## 8. Integraciones

- WhatsApp Business API (confirmación y recordatorio) — obligatoria en el MVP.
- Mercado Pago para señas — ya ofrecido por varios competidores; incluir en el MVP por paridad, no como diferenciador.
- Google Calendar — etapa posterior.
- ARCA y validación/liquidación de obras sociales — etapa posterior, alto costo de integración.

## 9. Restricciones

- Cumplimiento de la Ley 25.326 y facturación ARCA.
- Presupuesto acotado.
- Sin stack tecnológico impuesto.

## 10. Riesgos

- **Riesgo principal**: que los pacientes no usen la reserva online y sigan llamando por costumbre (supuesto sin probar).
- Cifras de adopción de los competidores autopublicadas y no verificables de forma independiente.
- Diferenciadores del informe todavía por validar en demo.
- Precios opacos en los líderes con liquidación de obras sociales.
- Declaraciones de Ley 25.326, ISO 27001 e HIPAA sin auditoría pública, inscripción RNBD por proveedor ni cláusulas AAIP 198/2023 publicadas para subprocessors.

## 11. Preguntas abiertas

- ¿Incluir seña con Mercado Pago desde el MVP por paridad competitiva (no como diferenciador)?
- ¿Liquidación y validación de obras sociales dentro del MVP o después?
- ¿IA conversacional con derivación humana obligatoria ante urgencias?
- ¿Soporte multisede desde el día 1 o consultorio único?
