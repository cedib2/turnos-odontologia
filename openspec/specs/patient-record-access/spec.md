# patient-record-access Specification

## Purpose
Acceso y copia de la propia historia clínica por el paciente, protegidos por el token `HISTORIA` de `public-access-tokens` y por verificación de identidad del titular, con registro append-only de cada acceso, exportación e intento denegado (C-13; Ley 26.529 art. 14, RN-CL-04, RN-SE-02). Sin diagnósticos automatizados ni interpretación clínica.

## Requirements

### Requirement: Token HISTORIA más identidad verificada
El sistema SHALL permitir la consulta o exportación solo con un token `HISTORIA` válido (firma, emisor, vigencia, `jti` disponible, paciente existente) **y** la identidad del titular verificada por el verificador configurado. El mecanismo de verificación es `(TBD)`; fuera de `dev`/`test` MUST denegar siempre. La validación genérica de C-06 (`validar`) no cambia y sigue denegando `HISTORIA`.

#### Scenario: Identidad no verificada
- **GIVEN** un token `HISTORIA` vigente
- **WHEN** se consulta o exporta sin evidencia válida del titular
- **THEN** se deniega, no se entrega nada y se registra `historia.paciente.denegada` con la operación

#### Scenario: Token de otro scope, vencido o adulterado
- **WHEN** se usa un token que no es `HISTORIA`, está vencido o no verifica
- **THEN** se deniega y queda registrado

### Requirement: Solo la propia historia
El token liga el acceso a un único `pacienteId`; el sistema MUST NOT devolver registros de otro paciente.

#### Scenario: Paciente sin ficha
- **WHEN** un titular verificado sin ficha consulta
- **THEN** recibe una respuesta vacía, nunca la historia de otro

### Requirement: Copia con plazo y hash
La exportación SHALL entregar un documento con los registros vigentes, su SHA-256 y un registro append-only en `historia_copias` con solicitud, vencimiento (`historia.copia.plazo-horas`, por defecto 48 h corridas; parámetros operativos `(TBD)`) y entrega; el evento `historia.paciente.exportada` indica si la entrega cayó dentro del plazo.

#### Scenario: Copia dentro del plazo
- **WHEN** el titular verificado exporta
- **THEN** recibe el documento con hash y queda registrada la copia dentro del plazo
