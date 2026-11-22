# clinical-signature Specification

## Purpose
Firma de evoluciones con matrícula del profesional y firma electrónica de consentimientos conforme Ley 25.506, con integridad verificable, sin sobrescritura y con un evento de auditoría por firma (C-12; RN-CL-02, RN-CL-03, RN-SE-02).

## Requirements

### Requirement: Firma de evolución con matrícula
El sistema SHALL firmar una evolución solo con la matrícula no vacía del profesional activo que es su autor, una única vez y sobre la versión vigente. La firma MUST guardar una copia de la matrícula y el SHA-256 del contenido firmado, y es append-only.

#### Scenario: Firma sin matrícula
- **GIVEN** un profesional con matrícula vacía
- **WHEN** intenta firmar su evolución
- **THEN** no se firma y se audita `clinico.evolucion.firma_rechazada` con motivo `matricula-faltante`

#### Scenario: Firma correcta
- **WHEN** el autor activo con matrícula firma
- **THEN** queda la firma con matrícula y hash, la integridad verifica y se emite `clinico.evolucion.firmada`

### Requirement: Evolución firmada no se modifica
Ante un cambio sobre una evolución firmada el sistema SHALL crear una versión nueva sin firma; la versión firmada, su texto y su firma MUST permanecer intactos.

#### Scenario: Nueva versión
- **GIVEN** una evolución firmada
- **WHEN** se registra una corrección
- **THEN** existe la versión 2 sin firma y la versión firmada conserva integridad

### Requirement: Consentimiento con firma electrónica
El consentimiento SHALL firmarse a través del puerto de firma electrónica (proveedor `(TBD)`), una sola vez, guardando proveedor, referencia y hash del documento. Si el proveedor no está disponible MUST quedar sin firmar y auditarse `clinico.consentimiento.firma_fallida`; la política de contingencia es `(TBD)`. Fuera de `dev`/`test`, sin proveedor configurado, ninguna firma se da por válida.

#### Scenario: Proveedor no disponible
- **WHEN** el proveedor no responde
- **THEN** no hay fila de firma y el fallo queda auditado
