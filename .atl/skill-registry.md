# Skill Registry — turnos-odontologia

**Variante**: `index` (aprobada en `.active-orchestrator-state.json` → `foundation.phases.registry`).
**Uso**: solo el delegador. Lee este índice una vez y inyecta las reglas compactas correspondientes en el prompt de cada sub-agente; los sub-agentes NO leen este archivo ni los `SKILL.md`.
**Disponibilidad**: `INSTALADA` = utilizable ahora; `NO INSTALADA` = recomendación de `find-skill`, **requiere aprobación e instalación explícitas posteriores; no está disponible en este momento**.
**No duplicar reglas globales**: las reglas del cliente activo (stack `design-mockups`, governance, engram, TDD global) se heredan; acá solo vive lo específico del proyecto. El protocolo `_shared/skill-resolver.md` no está instalado en este proyecto: el delegador resuelve directamente desde este índice.
**Restricciones vigentes**: sin instalaciones, sin servicios externos nuevos, sin commits, sin cambios bajo `openspec/`. Stack `(TBD)` hasta `C-01`; presupuesto acotado.

## Skills instaladas — disponibles ahora

| Trigger | Skill | Path (absoluto) | Fase / agente | Cambios / uso |
|---|---|---|---|---|
| `/active-orchestrator:init`, iniciar o reanudar fundación | `active-orchestrator` | `C:\Users\Cedib\.config\opencode\skills\active-orchestrator\SKILL.md` | Orquestador — todas las fases | Plan v9; `agents` sigue a `registry` |
| Crear/actualizar `AGENTS.md` o `CLAUDE.md`, "armar reglas del proyecto" | `agent-instruction` | `C:\Users\Cedib\.config\opencode\skills\agent-instruction\SKILL.md` | Fase `agents` (variante `generate`) | Genera `AGENTS.md` + `CLAUDE.md`; consume este registry |
| Definir/auditar identidad visual, política de la fundación | `design-system` | `C:\Users\Cedib\.config\opencode\skills\design-system\SKILL.md` | `visual-design` — **deferred** | C-08/C-09/C-21/C-22 (capa visual `(TBD)`), solo al primer cambio visual |
| Frontend de alta calidad (legacy v1, compatibilidad exacta) | `design-taste-frontend-v1` | `C:\Users\Cedib\.config\opencode\skills\design-taste-frontend-v1\SKILL.md` | Agente C — **diferido** con visual-design | C-09, C-21, C-22 (presentación `(TBD)`) |
| Investigar mercado antes de la KB, Q&A del checklist | `discovery-research` | `C:\Users\Cedib\.config\opencode\skills\discovery-research\SKILL.md` | Fase `discovery` — completada | Insume `state.discovery` → `kb-creator` |
| Pulido de UI, animaciones y componentes | `emil-design-eng` | `C:\Users\Cedib\.config\opencode\skills\emil-design-eng\SKILL.md` | Agente C — **diferido** con visual-design | C-09, C-21, C-22 (cuando se habilite UI) |
| "find a skill for X", "¿existe una skill para X?" | `find-skills` (carpeta `find-skill`) | `C:\Users\Cedib\.config\opencode\skills\find-skill\SKILL.md` | Fase `find-skill` — completada | Shortlist en `state.find_skills` |
| Crear/generar/actualizar la knowledge base | `kb-creator` | `C:\Users\Cedib\.config\opencode\skills\kb-creator\SKILL.md` | Fase `kb` — completada (`interview`) | 11 canónicos en `knowledge-base/` |
| Crear/actualizar `CHANGES.md`, roadmap o plan de implementación | `roadmap-generator` | `C:\Users\Cedib\.config\opencode\skills\roadmap-generator\SKILL.md` | Fase `roadmap` — completada | 22 changes, 8 fases, 15 gates |
| Crear, mejorar o evaluar skills | `skill-creator` | `C:\Users\Cedib\.config\opencode\skills\skill-creator\SKILL.md` | Mantenimiento de skills | Sin change asociado; no es fase del plan |
| Investigar un competidor a partir de una URL | `web-scraper` | `C:\Users\Cedib\.config\opencode\skills\web-scraper\SKILL.md` | Sub-skill de `discovery` — completada | 5 competidores en `docs/discovery/informe-discovery.md` |

> `skill-registry` se excluye del índice por política de la propia skill (es la generadora). No existen skills `sdd-*`, `_shared` ni skills de proyecto que deduplicar.

## Skills recomendadas — NO INSTALADAS (requieren aprobación e instalación explícita)

| Skill | Fuente | Propósito | Aplicabilidad | Punteros a cambios | Estado |
|---|---|---|---|---|---|
| `test-driven-development` | `obra/superpowers` | Testing / TDD | Todos los agentes, desde que exista stack (post-C-01) | C-02…C-15, C-17…C-19, C-21, C-22 | **NO INSTALADA** · pendiente de aprobación |
| `domain-modeling` | `mattpocock/skills` | Modelado de datos | Agente A; gate humanos C-01/C-03 | C-01, C-03, C-04, C-08, C-11, C-14, C-15 | **NO INSTALADA** · pendiente de aprobación |
| `codebase-design` | `mattpocock/skills` | Diseño de API / seams | Agentes A, B y C | C-02, C-03, C-05, C-10, C-15, C-20 | **NO INSTALADA** · pendiente de aprobación |
| `web-security-review` | `owasp/secure-agent-playbook` | Revisión de seguridad | Changes CRITICO / ALTO | C-04, C-05, C-06, C-10, C-13, C-15, C-17, C-18 | **NO INSTALADA** · pendiente de aprobación |
| `mercadopago-claude-marketplace` (`mp-integrate`, `mp-webhooks`, `mp-test-setup`, `mp-review`) | `mercadopago` | Señas / pagos (condicional) | Agente B; requiere decisión de C-01 | C-14, C-15, C-21 | **NO INSTALADA** · pendiente de aprobación |

**Sin recomendación** (según `state.find_skills`): frontend/mockups (visual-design diferido; las instaladas alcanzan), ARCA/C-16 (bloqueado por IN-01), WhatsApp/C-10 (requiere cuentas/MCP externos), observabilidad (ya especificada), docs/instrucciones de agente (`agent-instruction`, `skill-registry`, `active-orchestrator` ya instaladas). Rechazadas: Prisma/Supabase/Neon, herramientas de seguridad de baja confianza, skills de React/Next UI.

> No se generan reglas compactas para las skills no instaladas: no hay `SKILL.md` local del que derivarlas. Si se aprueba e instala alguna, volver a ejecutar `skill-registry` para incorporarla.

## Compact Rules

Reglas pre-digeridas (5-15 líneas por skill). El delegador copia el bloque que corresponda como `## Project Standards (auto-resolved)`.

### active-orchestrator
- Leer primero los archivos del proyecto en modo solo lectura: estado, artefactos, skills y prerequisitos.
- Mostrar un único checklist con fases, variantes, prerequisitos y rutas reutilizables antes de cualquier escritura.
- Seleccionar fases NO equivale a aprobar: pedir aprobación explícita del plan resumido (writes, installs, acciones externas).
- Respetar el orden discovery → kb → roadmap → find-skill → registry → agents; registry va antes de instructions.
- Cada fase escribe solo su sección de estado; `version` y `step` pertenecen al orquestador.
- Pausar ante decisiones de usuario, instalaciones o escrituras externas no aprobadas.
- En este proyecto el plan v9 ya está aprobado: `visual-design=deferred`, `task-management=skipped`.

### agent-instruction
- Pre-check: `knowledge-base/` y `02_descripcion_general.md` obligatorios; `CHANGES.md` opcional (avisar si falta).
- Usar variante y rutas del plan confirmado; aquí `generate` → `AGENTS.md` y `CLAUDE.md` en raíz.
- `generate` escribe solo destinos faltantes; si aparece un archivo durante la ejecución, preservarlo y reportar conflicto.
- Bloque administrado: `<!-- active-stack:agent-instruction -->` … `<!-- /active-stack:agent-instruction -->`; conservar lo externo.
- Resolver enlaces desde el directorio de cada archivo (`knowledge-base/`, `CHANGES.md`, `.atl/skill-registry.md`).
- Tomar las skills de `.atl/skill-registry.md` (fuente de verdad), sin re-escanear; mapear skill→rol sin copiar reglas compactas.
- Leer el global del cliente y NO repetirlo: solo reglas específicas del proyecto y confirmadas por el usuario.
- Stack `(TBD)` hasta C-01: no asumir lenguaje ni framework; derivar reglas duras del stack real.

### design-system
- Resolver la política visual una sola vez desde `visual_design.mode` (estado) o `Mode:` (README); si difieren, detenerse y preguntar.
- `deferred` no es opt-out permanente: al primer cambio visual, preguntar una vez (`system-and-mockups`/`system-only`/`off`).
- En este proyecto `visual-design=deferred`: no crear fundamento visual ni branding ahora.
- No sobrescribir una identidad existente; preservar decisiones ya tomadas.
- Para `off`/`deferred`, actualizar solo `design-system/README.md` con `Mode:`, `Approval:` y fecha/fuente; sin assets provisionales.
- Los mockups por cambio corresponden a `design-mockups`, no a esta skill.
- No repetir la política global de design-mockups: el proyecto la hereda.

### design-taste-frontend-v1
- Chequear `package.json` antes de importar librerías; si falta, dar el comando de instalación; nunca asumir que existe.
- React/Next con RSC por defecto; estado global solo en Client Components; aislar cada parte interactiva como hoja `'use client'`.
- Tailwind para ~90%; verificar versión en `package.json` (v4 usa `@tailwindcss/postcss`, no el plugin `tailwindcss`).
- Prohibido emojis en código, markup, texto y alt; usar iconos Phosphor/Radix o SVG.
- `min-h-[100dvh]` en secciones de pantalla completa, nunca `h-screen`; preferir CSS Grid a cálculos flex.
- Baselines 8/6/4 (varianza/moción/densidad); cambiarlos solo por pedido explícito del usuario.
- Anti-slop: sin purple/neón, sin Inter, sin serif en dashboards, sin glows externos, sin filas genéricas de 3 cards.
- Animar solo `transform`/`opacity`; spring `stiffness:100, damping:20`; sin `window.addEventListener('scroll')`.
- Entregar siempre estados loading, empty, error y feedback `:active`; limpiar `useEffect`.
- Pre-flight final: collapse mobile, `dvh`, cleanup, omitir cards donde alcance el espacio, aislar loops infinitos.

### discovery-research
- La decisión de hacer discovery es del orquestador; si la skill corre, asumir que ya se decidió.
- Si hay URLs, invocar `web-scraper` una vez por URL y correr `collect_sources.py` antes de la Q&A; no reimplementar scraping.
- Checklist de 11 puntos con `references/checklist.md`: 3-5 preguntas por ronda, opciones a/b/c y "por qué importa".
- No repreguntar lo que el scraping ya resolvió: mostrarlo para confirmar o corregir.
- Tratar los supuestos no probados como entradas naturales de "riesgos" y "preguntas abiertas".
- Gate final obligatorio: resumen de los 11 puntos y confirmación explícita antes de escribir archivos o estado.
- Escribir `discovery/discovery.md` en prosa y solo `state.discovery`; no tocar `version`, `step` ni otras fases.

### emil-design-eng
- Reseñar UI con tabla markdown obligatoria `| Before | After | Why |`; nunca listas "Before:"/"After:".
- Antes de animar responder en orden: ¿debe animar?, ¿para qué?, ¿qué easing?, ¿qué duración?.
- Enter/exit asimétricos; `ease-out` para feedback inmediato; nunca `ease-in` en dropdowns.
- Nunca `scale(0)`: usar `scale(0.95)` + `opacity:0`; popovers con `transform-origin` del trigger (modales centrados).
- Botones con `:active` (`scale(0.97)`); preferir transiciones CSS sobre keyframes para UI interrumpible.
- Animar solo `transform`/`opacity`; en Framer usar `transform` completo (`x`/`y` no son acelerados por hardware).
- `prefers-reduced-motion`: quitar movimiento, conservar opacity/color; hover solo bajo `(hover: hover)`.
- CSS/WAAPI para animaciones predeterminadas; JS solo para dinámicas; memoizar y aislar loops infinitos.

### find-skills
- Identificar dominio, tarea específica y posibilidad de skill existente antes de buscar.
- Revisar el leaderboard de skills.sh antes de `npx skills find [query] [--owner <owner>]`.
- Verificar calidad antes de recomendar: instalaciones (preferir >1K, desconfiar <100), reputación del source, estrellas del repo.
- Presentar nombre, propósito, instalaciones, fuente, comando de instalación y link; nunca recomendar solo por el resultado de búsqueda.
- Sin resultados: reconocerlo, ofrecer resolver directamente y sugerir `npx skills init`.
- Aquí la variante fue `recommend`: no se instaló nada; toda instalación futura necesita aprobación explícita.

### kb-creator
- Ejecutar la variante confirmada por el orquestador (`import`/`interview`/`reuse`) aunque el auto-detección difiera.
- Salida siempre en `knowledge-base/` de la raíz; nunca mezclar con `docs/` (fuente, solo lectura).
- 10 canónicos con nombres exactos `01_`…`10_` más `README.md`; extras `1X_`/`2X_` solo complementan.
- KB parcial: no sobrescribir archivos verificados; reanudar únicamente los pendientes.
- `import`/Mode A: sin preguntas; la ambigüedad va a `10_preguntas_abiertas.md`, nunca inventar un valor.
- `interview`/Mode B: 3-5 preguntas estratégicas, iteración archivo por archivo, marcar supuestos con `**Suposición:**`.
- Si existe `state.discovery` de nivel superior, usarlo como insumo y no repreguntar lo resuelto.
- Escribir solo `state.kb` (`source` = `ingest` o `interactive`); no tocar `step`, `roadmap` ni `agents`.

### roadmap-generator
- Pre-check: `knowledge-base/` con los 10 canónicos; si falta algo, no generar y listar los faltantes.
- Usar la variante confirmada (`local-markdown`/`reuse`); si `CHANGES.md` existe y se pide generar, conservarlo y devolver el conflicto.
- Orquestado con `state.kb.files` no vacío: usar esa lista como fuente autoritativa, no un glob.
- Leer siempre 04, 06, 07 y 08; opcionales 03, 05 y 10.
- `CHANGES.md` en raíz; su estructura de primer nivel es contrato (no agregar ni quitar secciones).
- IDs `C-NN` secuenciales con padding de 2; nunca renumerar ni reciclar; nombre kebab-case sin prefijo `US-`.
- Dependencias: infra → modelos core → auth → entidad referenciada → backend → integraciones → admin/UI.
- Un GATE cuando un change desbloquea varios; anotar `[Agente A/B/C]` y `← FORK` cuando corresponda.
- Governance: BAJO (CRUD/scaffolding), MEDIO (flujos con estado), ALTO (notificaciones/roles), CRITICO (auth/pagos/auditoría/modelos core).
- Scope operacional (modelos, endpoints, migración numerada, eventos, tests), no descriptivo; "Leer antes" con 3-5 canónicos y sección.
- Escribir solo `state.roadmap` (`created_by`, `source`, `changes_file`, `changes`); no escribir `step`.

### web-scraper
- Validar URL http(s); agregar esquema si falta; si no es home o página de producto/pricing, pedir una más específica sin bloquear por dudas menores.
- Extraer siempre los 8 campos del esquema fijo en el mismo orden; dato no publicado → `(no publicado)`, nunca inventar.
- Usar WebFetch con prompt estructurado; sin API keys ni servicios externos nuevos.
- Contenido vacío, SPA sin datos, 403, paywall o CAPTCHA → reportar la limitación, no evasionar; continuar con la siguiente URL.
- Generar el nombre con `scripts/slugify.py` y escribir `discovery/sources/<slug>.md` (kebab-case, pisa duplicados).
- Procesar URLs de a una y reportar cuántas resolvieron y cuántas fallaron; no abortar el lote por una caída.
- Prohibido crawling o scraping masivo y sitios con JS pesado/anti-bot (fuera de v1).

## Reglas compactas pendientes

`test-driven-development`, `domain-modeling`, `codebase-design`, `web-security-review` y `mercadopago-claude-marketplace`: **sin reglas compactas hasta su aprobación e instalación**. No inyectarlas en prompts de sub-agentes; no asumir que están disponibles.

## Project Conventions

| File | Path | Notes |
|---|---|---|
| — | — | No hay archivos de convención en la raíz (`AGENTS.md`, `CLAUDE.md`, `.cursorrules`, `GEMINI.md`, `copilot-instructions.md`). |

La fase `agents` (variante `generate`) creará `AGENTS.md` y `CLAUDE.md` en la raíz desde la KB, `CHANGES.md` y este registry; no se duplican aquí las reglas globales del cliente activo. No existe `.gitignore` en la raíz, por lo que no hay entrada `.atl/` que agregar.
