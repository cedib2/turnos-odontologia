# C-11 — `clinical-record-odontogram`

- **Fecha**: 2026-11-22
- **Solicitante / autorización**: dueño de la rama `Parte-2-Donato` (Parte 2), pedido explícito. Governance **MEDIO**.
- **Migración**: `V011__clinical_record_odontogram.sql`.
- **Spec**: `openspec/specs/clinical-record/spec.md`.

## Qué se implementó

| Pieza | Detalle |
|---|---|
| V011 | `piezas_fdi` con seed de 52 piezas (32 permanentes + 20 temporales) y checks de cuadrante/posición; `fichas_clinicas` (una por paciente, `historia_version`, turno de apertura); `anamnesis` (respuestas JSONB + texto libre, C-01 C4), `odontograma_registros` (pieza FDI, cara, estado, versión), `evoluciones` (versión y `reemplaza_id` único) y `consentimientos` (versión por tipo). Triggers append-only y sin borrado de fichas. |
| `HistoriaClinicaService` | Abrir ficha (desde turno o paciente, idempotente), registrar anamnesis, odontograma, evolución y consentimiento como versión nueva con bloqueo de ficha; consulta de la vista vigente. |
| `AutorizacionClinica` | Permisos por rol con `rol_permiso` (`ficha_clinica:crear`/`leer`); solo `REAL`/`SIMULADO`; denegaciones auditadas sin revertir el evento. |
| `FichaClinicaController` | Endpoints indicativos `/fichas` solo en dev/test, como la agenda de C-08. |
| Auditoría | `clinico.{ficha.abierta,anamnesis.registrada,odontograma.registrado,evolucion.registrada,consentimiento.registrado,acceso.denegado}` con autor y `historiaVersion`, sin contenido clínico. |

## Supuestos y `(TBD)` abiertos

- **Estructura de anamnesis**: campos mínimos como objeto JSON libre + texto libre (C-01 C4); el set cerrado de campos sigue `(TBD)`.
- **Valores de `cara` y `estado` del odontograma**: texto no vacío; catálogo cerrado `(TBD)`.
- **Borrado físico (RN-CL-05)**: `(TBD)`; mientras tanto la base lo prohíbe.
- **Acceso de recepción**: se respeta el seed V001 — la recepcionista tiene `ficha_clinica:crear` pero no `leer`. El auditor no tiene permisos clínicos en el seed aunque la KB lo muestre con lectura; cambiarlo exige migración de la Parte 1.
- **Ámbito `propio`** del odontólogo: sin vínculo usuario → profesional en el modelo, no se filtra por profesional (igual que en C-08).
- **Firma de evolución con matrícula y firma electrónica de consentimientos**: fuera de alcance, van en C-12; aquí los registros quedan sin firmar.
- **Retención de 10 años y copia en 48 h**: `(TBD)`, C-13/C-17.

## Evidencia

- `mvn test` completo en verde (251 tests: 240 previos + 11 nuevos) contra Postgres 17.
- `HistoriaClinicaPostgresTest` (9): seed FDI, codificación temporal/permanente, versión nueva ante sobrescritura, append-only en la base, reemplazo de evolución, ficha única desde el turno, permisos por rol y trazabilidad en auditoría. `FichaClinicaControllerTest` (2).
