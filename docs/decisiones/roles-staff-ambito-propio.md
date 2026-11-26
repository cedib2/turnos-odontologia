# Roles de staff: gestión de agenda, ámbito propio y ficha clínica

- **Fecha**: 2026-11-26
- **Decisor**: José Donato Fornasari Donadel (aprobación explícita; toca RBAC, governance CRITICO)
- **Changes afectados**: C-04 (RBAC), C-08 (agenda), C-11 (ficha clínica)
- **Origen**: revisión de los roles de discovery §2 (paciente, recepcionista y odontólogo)

## Contexto

La revisión encontró estos huecos:

- La recepcionista no podía cancelar ni reprogramar un turno desde el staff, ni dar de alta pacientes.
- No había vínculo entre usuario y profesional, así que el ámbito `propio` del seed V001 no se aplicaba.
- No había pantalla de ficha clínica.

## Decisiones

1. **Cancelación y reprogramación por recepción**: `POST /turnos/{id}/cancelacion` (motivo obligatorio) y `POST /turnos/{id}/reprogramacion` (`nuevoInicio`). No aplican la ventana de 48 hs: recepción es quien atiende los cambios que el paciente ya no puede hacer por link. La cancelación libera el hueco y audita `turno.cancelado` con `inicioAnterior` (contrato con C-22).
2. **Alta de pacientes**: `POST /pacientes` (tipo y número de documento, nombre, apellido y teléfono). Un documento repetido responde 409.
3. **Vínculo usuario-profesional**: columna `usuarios.profesional_id` (V033, un profesional por usuario) y `PUT /profesionales/{id}/usuario`, que solo puede usar el administrador.
4. **Ámbito `propio`** (`AlcanceProfesional`): con ámbito `todos` el actor ve todo; con `propio`, solo lo de su profesional vinculado. Si no tiene vínculo, se rechaza (`usuario_sin_profesional_vinculado`).
   - **Agenda** (`agenda:leer` y `turno:leer`): el odontólogo ve solo su agenda, y pedir la de otro responde 403. Recepción y administración ven todo.
   - **Ficha clínica** (`ficha_clinica:leer`): el odontólogo ve solo fichas de pacientes con algún turno con él. El ámbito se evalúa por paciente, así que no se revela si un paciente ajeno tiene ficha. Cada rechazo se audita como `clinico.acceso.denegado`.
5. **Administrador con acceso completo a fichas**: el seed solo tenía `ficha_clinica:leer` con ámbito `propio`, y aplicarlo dejaba al administrador sin acceso. V033 agrega ese permiso con ámbito `todos` solo para el administrador, como indica la KB (03_actores_y_roles, Ficha clínica: Administrador C L A E).
6. **Búsqueda de ficha por paciente**: `GET /fichas?pacienteId=`. Verifica el permiso de lectura y el ámbito antes de buscar; responde 404 si el paciente todavía no tiene ficha.
7. **RBAC fuera de dev/test** (`AgendaRbacPolicy`):

| Ruta | Lectura | Escritura |
|---|---|---|
| `/pacientes`, `/profesionales`, `/sillones` | lectura de agenda | alta de paciente y link de reserva: `agenda:crear`; vínculo de profesional: administrador |
| `/fichas` | `ficha_clinica:leer` | `ficha_clinica:crear` |
| `/turnos/{id}/...` | — | `agenda:actualizar` |

8. **Actor simulado configurable en dev**: `turnos.simulado.rol` (por defecto `recepcionista`) y `turnos.simulado.profesional-id`, para probar los permisos de cada rol sin login.
9. **Errores de staff legibles** (`ErroresStaffAdvice`): 400 para validaciones, 403 para permiso y ámbito, 409 para solapamiento, duplicados y estados inválidos, en lugar de un 500 genérico.

## Defectos previos encontrados y corregidos

- `HistoriaClinicaService.consultar` corría en una transacción de solo lectura y, al denegar, escribía en la auditoría. Con el publicador JDBC real, cualquier lectura denegada daba 500. Los tests no lo veían porque usan un registrador en memoria.
- La columna `permisos.ambito` guarda `propio`/`todos` en minúsculas (CHECK de V001), pero el enum JPA `Ambito` está en mayúsculas, así que `RolPermisoRepository.existsByRol_CodigoAndPermiso_Ambito` no encuentra nada. `AlcanceProfesional` consulta por SQL con `lower()`. El método del repositorio quedó sin corregir: hoy no lo usa nadie.

## Pendiente

- La KB marca para el auditor «L» en ficha clínica, pero el seed no le da `ficha_clinica:leer`. No se cambió.
- Los controladores siguen siendo superficie indicativa de `dev`/`test`, como el resto del proyecto.
