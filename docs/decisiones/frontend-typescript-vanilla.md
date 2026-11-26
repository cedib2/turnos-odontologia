# Frontend en TypeScript vanilla

- **Fecha**: 2026-11-25
- **Decisor**: José Donato Fornasari Donadel
- **Alcance**: primer cambio visual del proyecto (`visual-design` estaba diferido)

## Decisiones

| Tema | Decisión |
|---|---|
| Política visual (`CLAUDE.md`, se pregunta una vez) | **`system-only`**: sistema de diseño sin mockups previos, en `frontend/public/css/sistema.css` y documentado en `frontend/DISENO.md`. No se crea `design-system/` en la raíz. |
| Pantallas | Reserva del paciente, gestión del turno, ingreso del staff, agenda (día y semana) y cumplimiento (métricas, dossier, exportación y respaldo). |
| Tooling | Solo `tsc`: única dependencia nueva `typescript@5.9.3` (devDependency, versión exacta). Compila a módulos ES nativos, sin bundler ni framework. |
| Servido | Desde Spring Boot: `npm run build` deja HTML, CSS y JS en `src/main/resources/static` (ignorado por git). Mismo origen que la API: las cookies de sesión y el CSRF de C-04 funcionan sin CORS. |

## Cambios de backend necesarios

Aprobados junto con el frontend, todos en la superficie indicativa de `dev`/`test`:

- `DirectorioController`: `GET /pacientes`, `/profesionales` y `/sillones` (DTOs, no entidades) y `POST /pacientes/{id}/enlace-reserva`, que emite el token `RESERVAR` de C-06 (1 hora) y devuelve el link. Exige profesional, **sillón** y tratamiento: la reserva pública rechaza crear un turno sin sillón (`sillonId_requerido`), así que un link sin sillón mostraría horarios imposibles de reservar.
- `ErroresStaffAdvice` (antes `ConflictoDeDatosAdvice`): en los controladores de staff, una violación de integridad responde 409 en lugar de 500, y las restricciones de exclusión de la agenda (RN-AG-01) se informan como `solapamiento`. Después se amplió con 400, 403 y 409 para validaciones, permisos y estados.

Ajustes de tests de guarda:

- `AgendaSurfaceTest` prohíbe exponer `/pacientes` en `interfaces/web`. Se agrega una excepción explícita solo para `DirectorioController` (la superficie aprobada) y se lo suma a la verificación de que los controladores sean solo `dev`/`test`. El resto de la guarda queda igual.
- `CurrentActorProviderTest` excluye `src/main/resources/static`: es el JS compilado del frontend (código de cliente que llama a `/auth`), no código del servidor.

## Hallazgos para resolver

1. **Sentido de la ventana de 48 hs hábiles (C-09 contra C-22).** Resuelto el mismo día: ver `docs/decisiones/C-09-sentido-ventana-48hs.md`. El frontend vuelve a indicar el plazo.
2. **Horarios pasados.** La disponibilidad pública incluye los horarios de hoy que ya pasaron. El frontend los filtra; el backend debería excluirlos.
3. **Spring devuelve texto con `Content-Type: application/json`** cuando el pedido acepta JSON (p. ej. `credenciales-invalidas`). El cliente lo tolera leyendo el texto y parseándolo solo si es JSON válido.

## Verificación

Recorrido de punta a punta con Playwright contra la app en el perfil `dev` y PostgreSQL 16 (`docker compose`), en escritorio (1280×860) y en celular (iPhone 13):

- Agenda: alta de turno, rechazo por solapamiento con mensaje claro, bloqueo, vista semanal y emisión del link de reserva.
- Paciente: reserva desde el link, confirmación de asistencia y link reusado rechazado con su mensaje.
- Cumplimiento: tablero de métricas, dossier y respaldo (falla como corresponde sin `BACKUP_ENCRYPTION_KEY`).
- Ingreso: credenciales inválidas con su mensaje.
- Sin errores de consola inesperados y sin desborde horizontal en celular.
