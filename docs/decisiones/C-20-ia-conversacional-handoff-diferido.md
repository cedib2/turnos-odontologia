# C-20 — `ia-conversacional-handoff`: cerrado como diferido, sin código

- **Fecha**: 2026-11-22
- **Solicitante**: dueño de la rama `Parte-2-Donato` (Parte 2), pedido explícito de ejecutar C-20.
- **Base de la decisión**: `docs/decisiones/C-01-decisiones-habilitantes.md` §A3, aprobada por el PO el 2026-11-20: *«solo plantillas WhatsApp aprobadas, sin asistente conversacional; `IA_HANDOFF_ENABLED=false`, `C-20` diferido»*. Alternativa descartada: asistente con handoff clínico registrado, por la evidencia de precisión y las obligaciones de RN-CL-06 que implicaría en el MVP.
- **Regla del propio change** (`CHANGES.md` §C-20): *«Inclusión en MVP `(TBD)`; si no se habilita, cerrar ficha como diferida sin código.»*

## Qué se hizo

- **Sin código de producto**: no hay adaptador conversacional, detección de urgencia ni handoff. Implementarlo contradiría A3 y la regla dura del proyecto de no decidir por convención (RN-GL-03).
- **Guardia** `IaHandoffDiferidoTest`: verifica que A3 siga documentada, que `IA_HANDOFF_ENABLED` exista solo como nombre en `.env.example` y que `src/main` no contenga código de asistente conversacional, handoff ni proveedores de IA. Si alguien agrega ese código sin una decisión nueva, el CI falla.
- **Coherencia con C-10**: el webhook de WhatsApp solo procesa estados de entrega y respuestas de botón de plantillas; los mensajes de texto libre del paciente se ignoran, sin respuesta automática ni interpretación.

## Para retomarlo (fuera del MVP actual)

1. Decisión nueva del PO que reemplace A3 (DD-11), con la evidencia de precisión exigida.
2. Proveedor/API del asistente `(TBD)`, reglas explícitas de derivación humana ante dolor, sangrado o postoperatorio (RN-CL-06) y métricas de handoff `(TBD)`.
3. Registrar cada handoff en la historia clínica (C-11/C-12) con override humano auditable (C-05), sin diagnósticos automatizados.
4. Retirar `IaHandoffDiferidoTest` en el mismo change que implemente lo anterior.

## Evidencia

- `mvn test` completo en verde (269 tests: 267 previos + 2 de la guardia).
