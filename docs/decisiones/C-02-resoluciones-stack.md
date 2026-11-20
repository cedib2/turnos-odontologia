# C-02 — Resoluciones del stack y convenciones base

- **Change**: `C-02 foundation-scaffolding`
- **Governance**: BAJO
- **Estado**: implementado
- **Fecha**: 2026-11-20
- **Fuentes**: `CHANGES.md` §`[C-02]`, `docs/decisiones/C-01-decisiones-habilitantes.md` (B1/B2/B3/B4/B5/B6, A3), `knowledge-base/08_arquitectura_propuesta.md` §Estructura de directorios y §Variables de entorno.

## Resoluciones de entorno verificado

| Ítem | Resolución | Evidencia |
|---|---|---|
| JDK | Java 17.0.20.1 (Temurin) | `java -version` → `openjdk version "17.0.20.1"`, `Eclipse Adoptium` |
| Build tool | Maven 3.9.16 | `mvn -version` → `Apache Maven 3.9.16` |
| Gradle | No instalado → descartado | No disponible en el entorno verificado |
| Spring Boot | 3.5.16 | Último estable de la serie 3.x; publicado 2026-06-25 |
| Paquete Java base | `com.turnosodontologia` | Convención nueva de C-02; DDD bajo `domain`, `application`, `infrastructure`, `interfaces` |
| Motor de base de datos | `(TBD)` | Se cierra en `C-03`; no se fija driver ni URL concreta |

> Nota de ciclo de vida: Spring Boot 3.5 finalizó el soporte de código abierto el 2026-06-30 y 3.5.16 es el último parche 3.x. Se usa 3.5.16 porque el alcance de C-02 exige expresamente Spring Boot 3.x; una eventual migración a 4.x se evalúa fuera de este change.

## Contrato del health check

- **Decisión C-02**: `GET /health` → `200` con cuerpo `{"status":"UP"}`.
- La KB marca el path/verbo como `(TBD)`; C-02 lo fija con este contrato mínimo e indicativo, sin control de dependencias (DB, WhatsApp, Mercado Pago).
- Tipo de respuesta: `com.turnosodontologia.interfaces.HealthCheckResponse`, un `record` con `String status`; el valor `UP` se produce en el controlador cuando se implemente la capa REST.

## Desviaciones deliberadas

- **Lombok diferido**: se declara en `pom.xml` (`provided`) pero el código de C-02 usa `record`s de Java 17 sin anotaciones Lombok, para que el harness compile solo con `javac`/JDK. Lombok y los DTO se aplican en `C-03`, cuando se resuelvan las dependencias.
- **JUnit no ejecutable aún**: `src/test/java/README.md` es placeholder; las dependencias de test se declaran pero no se descargan ni ejecutan en C-02. Las pruebas ejecutables viven en `tests/SmokeTest.java` y usan solo `java.base`.
- **Maven declarado, no ejecutado**: `pom.xml` y `.github/workflows/ci.yml` se escriben sin ejecutar `mvn`, sin instalar dependencias y sin servicios externos.
- **API REST solo en C-02**: la UI pública de reserva por link/QR sigue en el MVP y se implementa en changes posteriores (aclaración final de C-01).
- **Gestor de secretos**: `(TBD)`; `.env.example` usa marcadores `(TBD)` y `IA_HANDOFF_ENABLED=false` (decisión A3).
- **Herramienta de lint**: `(TBD)`; el job `lint` del CI compila con Maven como esqueleto hasta fijar la herramienta.
