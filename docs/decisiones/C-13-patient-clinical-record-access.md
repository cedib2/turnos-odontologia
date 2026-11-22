# C-13 — `patient-clinical-record-access`

- **Fecha**: 2026-11-22
- **Solicitante / autorización**: dueño de la rama `Parte-2-Donato` (Parte 2), pedido explícito. Governance **ALTO** (acceso a datos sensibles): se recomienda revisión posterior del PO y del responsable legal.
- **Migración**: `V013__patient_record_access.sql`.
- **Spec**: `openspec/specs/patient-record-access/spec.md`.

## Qué se implementó (`historiapaciente`)

| Pieza | Detalle |
|---|---|
| `PublicTokenService.validarParaHistoria` (C-06, aditivo) | Valida firma, emisor, vigencia, `jti`, scope `HISTORIA` y paciente, sin otorgar acceso. `validar` **no cambia**: sigue denegando `HISTORIA`, y C-06 sigue sin emitir `identidadVerificada=true`. |
| `VerificadorIdentidadTitular` | Puerto. `SimulatedVerificadorIdentidad` (dev/test, solo la evidencia que registra el test) y `VerificadorIdentidadNoConfigurado` (resto: nunca verifica). |
| `AccesoHistoriaPacienteService` | Consulta y exportación de la propia historia; cada acceso, exportación y denegación se audita en el log append-only (actor `PACIENTE`, con `jti` y operación). |
| V013 `historia_copias` | Copia entregada: solicitud, vencimiento del plazo, entrega, hash del documento y `jti`. Append-only. |
| `HistoriaClinicaService.historiaDelTitular` | Lectura de la historia vigente del titular para C-13, que autoriza antes de llamar. |
| `AccesoHistoriaConfig` | Cablea `PublicTokenService` con `PUBLIC_TOKEN_SECRET` (y `PUBLIC_TOKEN_ISSUER`, por defecto `turnos-odontologia-public`), el verificador según perfil y el reloj. |

## Supuestos y `(TBD)` abiertos

- **Mecanismo de verificación de identidad**: `(TBD)` (KB §Preguntas abiertas). El simulado no representa ninguna decisión; fuera de dev/test no se verifica a nadie, así que el acceso queda cerrado hasta decidirlo.
- **Plazo de copia**: 48 h corridas configurables (`historia.copia.plazo-horas`); hábiles vs. corridas y demás parámetros operativos `(TBD)`. La copia digital se entrega en el acto y se registra si cayó dentro del plazo.
- **Alcance del acceso del paciente**: se entrega la vista vigente (anamnesis, odontograma, evoluciones vigentes y consentimientos); incluir el historial completo de versiones queda `(TBD)`.
- **Uso único del token**: no se marca el `jti` como usado al consultar, para permitir reintentos dentro de la vigencia de 24 h de C-06; uso único `(TBD)`.
- **Exposición HTTP**: igual que en C-10, la ruta pública queda `(TBD)`; el token viaja por path/query según el contrato de C-06.
- **Eventos**: C-06 emitía la denegación de `HISTORIA` como evento `SIMULADO` sin persistir; C-13 registra sus accesos y denegaciones en el log persistente con modo `PACIENTE`, como pide su alcance.

## Corrección de C-10

`.env.example` no incluía `WHATSAPP_APP_SECRET`, que usa el verificador de firma del webhook de C-10; se agrega el nombre, sin valor.

## Ajuste de infraestructura de tests

Con los contextos de Spring que suman C-10 a C-13, los pools Hikari cacheados (10 conexiones cada uno) llegaban a 97 conexiones simultáneas y superaban `max_connections` de Postgres (`FATAL: sorry, too many clients already`). `src/test/resources/application-test.properties` acota el pool de test a 4 (mínimo 1 ociosa): el pico medido baja a 24. El test de concurrencia de C-08 necesita 2. No afecta producción.

**`target/` fuera del repositorio**: `target/` estaba versionado en git. El checkout traía `target/test-classes/application-test.yml` viejo con fecha más reciente que el fuente y Maven no lo reemplazaba, así que el límite del pool no llegaba al CI. Este change saca `target/` del índice (`git rm -r --cached target`) y lo agrega a `.gitignore`; el límite queda en `application-test.properties`. Las otras ramas que todavía versionen `target/` deben hacer lo mismo al integrarse.

## Evidencia

- `mvn test` completo en verde (269 tests al cierre de la Parte 2; 8 propios de C-13) contra Postgres 17.
- `AccesoHistoriaPacientePostgresTest` (8): titular verificado con acceso auditado, identidad no verificada → denegación registrada, tokens de otro scope/vencidos/adulterados/de paciente inexistente, solo la propia historia, copia dentro del plazo con hash y registro append-only, entrega fuera de plazo registrada, `validar` de C-06 sin cambios y verificador fuera de dev/test.
