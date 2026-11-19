# turnos-odontologia — Base de Conocimiento

Base de conocimiento generada con `kb-creator` (variante `interview`) el 2026-11-19. El «registro de entrevista» son los outputs ya corregidos de la fase `discovery`; no se realizaron preguntas nuevas al usuario. Todo punto no respondido se marca `(TBD)` y se consolida en [10_preguntas_abiertas.md](10_preguntas_abiertas.md).

**Fuentes**: `docs/discovery/discovery.md`, `docs/discovery/informe-discovery.md` y el PDF original `C:\Users\Cedib\Downloads\InvMercadoOdont.pdf` (texto extraído en el entorno temporal). Estado: `kb` en curso; la KB no modifica `.active-orchestrator-state.json` ni `openspec/`.

## Índice de Archivos

| Archivo | Contenido |
|---|---|
| [01_vision_y_objetivos.md](01_vision_y_objetivos.md) | Propósito, actores, alcance MVP, fuera de alcance y métricas de éxito |
| [02_descripcion_general.md](02_descripcion_general.md) | Stack `(TBD)`, arquitectura lógica, integraciones y superficie API indicativa |
| [03_actores_y_roles.md](03_actores_y_roles.md) | Actores, matriz RBAC, rutas públicas y datos sensibles |
| [04_modelo_de_datos.md](04_modelo_de_datos.md) | Dominios, ERD, entidades, constraints y seed data |
| [05_reglas_de_negocio.md](05_reglas_de_negocio.md) | Reglas `RN-XX-NN` por dominio y excepciones globales |
| [06_funcionalidades.md](06_funcionalidades.md) | Épicas e historias `US-NNN` con criterios de aceptación |
| [07_flujos_principales.md](07_flujos_principales.md) | Siete flujos extremo a extremo con errores |
| [08_arquitectura_propuesta.md](08_arquitectura_propuesta.md) | Patrones, estructura conceptual, seguridad y variables de entorno |
| [09_decisiones_y_supuestos.md](09_decisiones_y_supuestos.md) | Decisiones `DD-01..11` y supuestos `SU-01..07` |
| [10_preguntas_abiertas.md](10_preguntas_abiertas.md) | Inconsistencias `IN-01..06`, preguntas priorizadas y lista de `(TBD)` |

## Quick Start para Desarrolladores

1. Entender el dominio → [01](01_vision_y_objetivos.md), [03](03_actores_y_roles.md).
2. Entender los datos → [04](04_modelo_de_datos.md).
3. Entender las reglas → [05](05_reglas_de_negocio.md).
4. Entender la arquitectura → [02](02_descripcion_general.md), [08](08_arquitectura_propuesta.md).
5. Implementar → [07](07_flujos_principales.md), [06](06_funcionalidades.md).
6. **Antes de codificar** → [10](10_preguntas_abiertas.md) y [09](09_decisiones_y_supuestos.md).

## Resumen Ejecutivo

El sistema debe permitir reservar turnos odontológicos por link/QR con confirmación inmediata, cancelación y reprogramación dentro de 48 hs hábiles, sobre una agenda multiprofesional y multisillón sin solapamientos, con WhatsApp Business API obligatoria, historia clínica FDI y señas con Mercado Pago por paridad.

La tesis de oportunidad fue **corregida**: no es cierto que nadie cobre seña con Mercado Pago ni que ningún proveedor declare Ley 25.326. Los diferenciadores reales son precio predecible y todo incluido, métricas verificables de ocupación y ausentismo, relleno automático de lista de espera con depósito y cumplimiento **demostrable** con evidencia.

El stack tecnológico, la escala y varios parámetros de negocio están deliberadamente `(TBD)`: la entrevista no los resolvió y no corresponde inventarlos.
