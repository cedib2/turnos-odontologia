# Fichas clínicas: rol real en dev, acceso del titular por link y confirmación de turno

- **Fecha**: 2026-11-26
- **Pedido**: el administrador y el odontólogo recibían «Tu rol no tiene permiso para esta operación.» al abrir una ficha; el paciente no tenía forma de ver su propia historia; mostrar una confirmación al crear un turno.

## Diagnóstico del error de permisos

En el perfil `dev` todas las pantallas de staff usaban `SimulatedCurrentActorProvider`, que actúa siempre como **recepcionista** aunque se haya iniciado sesión con otro usuario. La recepcionista tiene `ficha_clinica:crear` pero no `ficha_clinica:leer`, así que `AutorizacionClinica` rechazaba la lectura con `sin_permiso`. El proveedor que lee el login (`AuthenticatedCurrentActorProvider` + `AgendaSecurityConfig`) existe solo fuera de dev/test, donde la superficie de staff no está expuesta.

No había discrepancias de nombres de rol (sin prefijo `ROLE_`; el token lleva `odontologo` y la autorización compara en minúsculas). El desajuste de mayúsculas del ámbito (`PROPIO`/`propio`) ya se había corregido con `AmbitoConverter`.

## Cambios

| Pieza | Comportamiento |
|---|---|
| `auth/SesionDevCurrentActorProvider` (`@Profile("dev")`, `@Primary`) | Con una cookie de sesión válida actúa el usuario logueado con su rol real y su profesional vinculado (`usuarios.profesional_id`). Sin sesión, sesión vencida o usuario inactivo: el actor simulado de siempre. `test` y producción sin cambios. |
| `historiapaciente/HistoriaPublicaController` | `POST /publico/v1/historia/consulta?token=` y `/copia?token=`, con la evidencia de identidad en el cuerpo (no en la URL). Usa `AccesoHistoriaPacienteService` (C-13): el token `HISTORIA` está atado a un paciente y no hay parámetro de paciente. Denegaciones: 403 `identidad_no_verificada`, 401 para token inválido, vencido o alterado; todo auditado. |
| `DirectorioController` `POST /pacientes/{id}/enlace-historia` | El staff emite el link (`/mi-historia.html?token=…`, vence en 24 h); la emisión se audita como `historia.paciente.enlace_emitido`. |
| `VerificadorIdentidadDocumentoDev` (`@Profile("dev")`) | Solo para probar en dev: el titular se identifica con su número de documento. **No es el mecanismo real** (sigue `(TBD)`, RN-CL-04); en `test` sigue `SimulatedVerificadorIdentidad` y fuera de dev/test la verificación falla cerrada. |
| `CrearTurnoPublicoResponse` | Suma (aditivo) `tratamiento`, `profesional`, `sillon` y `consultorio` para la confirmación del paciente. |
| Frontend | Botón «Link para el paciente» en Fichas; página `mi-historia.html` de solo lectura con «Descargar copia»; componente `confirmacionTurno` (ui.ts + sistema.css) usado al crear un turno en la agenda y en la reserva pública. El campo «Autoriza» del sobreturno se precarga con el usuario logueado. |

## Permisos resultantes (verificados en el navegador, dev)

| Rol | Fichas clínicas |
|---|---|
| Administrador | Todas |
| Odontólogo | Solo pacientes con turnos con él; otro paciente → «Solo podés ver fichas de pacientes que atendés.» |
| Recepcionista | No lee fichas (RBAC de V001: solo `ficha_clinica:crear`) |
| Paciente | Solo la suya, con su link y su documento; un documento ajeno o un token alterado se rechazan |

## Confirmación de turno

- La insignia muestra el **estado real**: «✓ Confirmado» solo si el turno está confirmado; la reserva pública crea turnos `reservado`.
- El aviso habla de WhatsApp (el sistema no envía correos).
- En la reserva pública, «Reservar otro turno» explica que cada link sirve para una sola reserva (el token `RESERVAR` se consume al reservar).
