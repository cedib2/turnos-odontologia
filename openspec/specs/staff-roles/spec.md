# staff-roles Specification

## Purpose
Operación de los roles de staff sobre la agenda y la ficha clínica: cancelación y reprogramación por recepción, alta de pacientes, vínculo entre usuario y profesional, y aplicación del ámbito `propio` de los permisos del seed V001. Decisión: `docs/decisiones/roles-staff-ambito-propio.md`.

## Requirements

### Requirement: Cancelación y reprogramación por recepción
El sistema SHALL permitir a un rol con `agenda:actualizar` cancelar un turno activo con motivo obligatorio y reprogramarlo a un horario válido, sin aplicar la ventana de 48 hs hábiles. La cancelación SHALL liberar el hueco en la misma transacción y auditar `turno.cancelado` con el motivo e `inicioAnterior`. La reprogramación MUST respetar horarios, feriados y la restricción de solapamiento.

#### Scenario: Cancelación con motivo
- **GIVEN** un turno activo
- **WHEN** recepción lo cancela con un motivo
- **THEN** el turno queda `cancelado` e inactivo, su ocupación se libera y se audita `turno.cancelado` con `inicioAnterior`

#### Scenario: Turno ya cancelado
- **GIVEN** un turno cancelado
- **WHEN** se intenta cancelarlo de nuevo
- **THEN** se responde 409 sin cambios

### Requirement: Alta de pacientes
El sistema SHALL dar de alta un paciente con tipo y número de documento, nombre, apellido y teléfono. Un documento repetido MUST responder 409.

#### Scenario: Documento repetido
- **GIVEN** un paciente con DNI 40111222
- **WHEN** se intenta dar de alta otro con el mismo DNI
- **THEN** se responde 409 y no se crea un segundo paciente

### Requirement: Ámbito propio
Para cada permiso, el sistema SHALL usar el ámbito de la matriz `rol_permiso`: con `todos` el actor accede a todo; con `propio`, solo a lo de su profesional vinculado (`usuarios.profesional_id`). Un actor con ámbito `propio` sin profesional vinculado MUST ser rechazado.

#### Scenario: Agenda del odontólogo
- **GIVEN** un odontólogo vinculado al profesional 1
- **WHEN** consulta la agenda sin filtro o la del profesional 2
- **THEN** recibe solo la agenda del profesional 1, y el pedido de la del profesional 2 responde 403 `agenda_de_otro_profesional`

#### Scenario: Ficha de un paciente ajeno
- **GIVEN** un odontólogo vinculado al profesional 1 y un paciente sin turnos con él
- **WHEN** consulta su ficha, la tenga o no
- **THEN** responde 403 `ficha_de_paciente_no_atendido` y se audita `clinico.acceso.denegado`

#### Scenario: Administrador
- **GIVEN** el administrador, con `ficha_clinica:leer` de ámbito `todos` (V033)
- **WHEN** consulta cualquier ficha
- **THEN** la operación se permite

### Requirement: Búsqueda de ficha por paciente
El sistema SHALL devolver la ficha vigente de un paciente por su id después de verificar el permiso de lectura y el ámbito, y SHALL responder 404 si el paciente no tiene ficha. Sin permiso de lectura MUST responder 403 sin revelar si la ficha existe.

#### Scenario: Recepción sin permiso de lectura
- **GIVEN** el rol recepcionista, sin `ficha_clinica:leer`
- **WHEN** busca la ficha de un paciente, la tenga o no
- **THEN** responde 403 `sin_permiso` en ambos casos
