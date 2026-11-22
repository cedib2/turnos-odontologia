# C-12 — `clinical-signature-consent`

- **Fecha**: 2026-11-22
- **Solicitante / autorización**: dueño de la rama `Parte-2-Donato` (Parte 2), pedido explícito. Governance **ALTO** (validez legal de datos de usuario): se recomienda revisión posterior del PO y del responsable legal (owner legal a designar, C-01 D).
- **Migración**: `V012__clinical_signature_consent.sql`.
- **Spec**: `openspec/specs/clinical-signature/spec.md`.

## Qué se implementó

| Pieza | Detalle |
|---|---|
| V012 | `evolucion_firmas` (una por evolución, matrícula no vacía, SHA-256 del contenido, firmante) y `consentimiento_firmas` (una por consentimiento, proveedor, referencia, hash). Ambas append-only con el trigger de C-11. |
| `FirmaClinicaService.firmarEvolucion` | Exige permiso clínico, que firme el profesional autor, activo y con matrícula; rechaza firma doble y firma de versiones reemplazadas. Cada rechazo se audita (`clinico.evolucion.firma_rechazada`) sin revertir el evento. |
| `FirmaClinicaService.firmarConsentimiento` | Firma por el puerto `FirmaElectronicaGateway`; si no está disponible no firma y audita `clinico.consentimiento.firma_fallida`. |
| `evolucionIntegra` | Recalcula el hash y lo compara con el firmado. |
| Puerto de firma electrónica | `SimulatedFirmaElectronicaGateway` (dev/test) y `FirmaElectronicaNoConfiguradaGateway` (resto: nunca firma). |
| Versionado | Modificar una evolución firmada usa `HistoriaClinicaService.registrarEvolucion(..., reemplazaId)` de C-11: crea la versión siguiente sin firma y la firmada queda intacta (RN-CL-02). |

## Supuestos y `(TBD)` abiertos

- **Proveedor de firma electrónica (Ley 25.506)** e implementación: `(TBD)`; sin dependencias ni servicios externos nuevos.
- **Política de contingencia** si el proveedor no está disponible: `(TBD)` (KB §Preguntas abiertas). No se inventa una alternativa (p. ej. firma en papel): el consentimiento queda sin firmar y el fallo queda auditado.
- **Vínculo actor ↔ profesional**: no existe en el modelo; la firma recibe el `profesionalId`, que debe ser el autor de la evolución. Exigir que el usuario autenticado *sea* ese profesional requiere ese vínculo `(TBD)`.
- **Hash de contenido**: SHA-256 de `id|ficha|profesional|versión|texto`; es evidencia de integridad interna, no una firma digital con certificado.
- **Validez legal** de la firma con matrícula y del consentimiento electrónico: a confirmar por el responsable legal.

## Evidencia

- `mvn test` completo en verde (259 tests: 251 previos + 8 nuevos) contra Postgres 17.
- `FirmaClinicaPostgresTest` (8): firma correcta con evento de auditoría, firma sin matrícula, autor/activo/una vez/versión vigente, nueva versión ante modificación de evolución firmada, append-only de firmas, consentimiento firmado, proveedor no disponible (contingencia `(TBD)`), permiso clínico y perfiles del puerto.
