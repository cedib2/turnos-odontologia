# turnos-odontologia

Sistema de gestión de turnos para consultorios odontológicos: reserva pública por link/QR, agenda multiprofesional y multisillón sin solapamientos, historia clínica con odontograma FDI, cobro de señas, facturación y cumplimiento normativo (Ley 25.326, Ley 26.529).

Los diferenciadores del producto son el **precio predecible y todo incluido**, las **métricas verificables de ocupación y ausentismo**, el **relleno automático de la lista de espera con depósito** y el **cumplimiento demostrable con evidencia**. La seña con Mercado Pago y la declaración de Ley 25.326 se incluyen por paridad competitiva, no como innovación.

## Stack

| Capa | Tecnología |
|---|---|
| Lenguaje | Java 17 |
| Framework | Spring Boot 3.5 (Web, Security, Data JPA) |
| Base de datos | PostgreSQL |
| Migraciones | Flyway (`src/main/resources/db/migration`, V001–V033) |
| Tests | JUnit 5 + Testcontainers (PostgreSQL) |
| Frontend | TypeScript vanilla (solo `tsc`), sin framework; servido por Spring Boot |
| Build | Maven (backend) y npm (frontend) |
| CI | GitHub Actions (`.github/workflows/ci.yml`): compilación y `mvn test` en cada push y PR |

Integraciones del MVP: **WhatsApp Business API** (notificaciones con outbox y webhooks idempotentes), **Mercado Pago** (señas) y **ARCA** (facturación electrónica). En los perfiles `dev` y `test` se usan adaptadores simulados.

## Requisitos

- JDK 17
- Maven 3.9 o superior
- Docker en ejecución (los tests de integración levantan PostgreSQL con Testcontainers)
- Node.js 20 o superior, para compilar el frontend

## Cómo compilar y probar

```bash
mvn -B -DskipTests compile
```

```bash
mvn -B test
```

La suite completa tiene 498 tests. No requiere variables de entorno: el perfil `test` usa valores no productivos.

## Cómo ejecutar la aplicación

El punto de entrada es `com.turnosodontologia.TurnosOdontologiaApplication`. Para correrla en local hace falta un PostgreSQL. El repo incluye un [`docker-compose.yml`](docker-compose.yml) con PostgreSQL 16 (base, usuario y contraseña `turnos`, puerto 5432 y datos persistidos en un volumen):

```bash
docker compose up -d
```

Para detenerla, `docker compose down`; para borrar también los datos, `docker compose down -v`.

Con la base levantada, se ejecuta con el perfil `dev`. Flyway aplica las migraciones al arrancar.

```bash
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/turnos
export SPRING_DATASOURCE_USERNAME=turnos
export SPRING_DATASOURCE_PASSWORD=turnos
export AUTH_JWT_SECRET=un-secreto-de-desarrollo-de-al-menos-32-caracteres
export PUBLIC_TOKEN_SECRET=otro-secreto-de-desarrollo
```

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

También se puede empaquetar y correr el jar:

```bash
mvn -B -DskipTests package
```

```bash
java -jar target/turnos-odontologia-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
```

Con cualquier otro perfil (por ejemplo `--spring.profiles.active=prod`) se activan la seguridad de C-04 y los adaptadores no configurados: WhatsApp, firma electrónica y aviso de lista de espera fallan de forma explícita en lugar de simular envíos, hasta que se definan sus proveedores reales.

La aplicación queda en `http://localhost:8080`. `AUTH_JWT_SECRET` es obligatoria; el resto de las variables tiene valores por defecto no productivos en el perfil `dev`.

## Frontend

El frontend vive en [`frontend/`](frontend/): TypeScript vanilla compilado con `tsc`, sin framework ni bundler. Tiene seis pantallas:

| Pantalla | Ruta | Para quién |
|---|---|---|
| Inicio | `/` | Todos |
| Reserva por link/QR | `/reservar.html` | Paciente (link con token `RESERVAR`) |
| Gestión del turno | `/turno.html` | Paciente (link con token `GESTIONAR`) |
| Ingreso | `/ingresar.html` | Staff |
| Agenda | `/agenda.html` | Recepción y odontólogos |
| Fichas clínicas | `/fichas.html` | Odontólogos y administración (búsqueda por paciente, odontograma FDI, evoluciones) |
| Cumplimiento | `/cumplimiento.html` | Administración y auditoría |

Se compila a `src/main/resources/static`, desde donde lo sirve Spring Boot en el mismo origen que la API. Hay que compilarlo antes de `mvn package` o `mvn spring-boot:run`:

```bash
cd frontend && npm ci && npm run build
```

En el perfil `dev` el actor simulado es un recepcionista. Para probar otro rol sin login, por ejemplo un odontólogo vinculado al profesional 1, agregá `--turnos.simulado.rol=odontologo --turnos.simulado.profesional-id=1` al arrancar. El odontólogo ve solo su agenda y las fichas de sus pacientes; ver [`docs/decisiones/roles-staff-ambito-propio.md`](docs/decisiones/roles-staff-ambito-propio.md).

Para desarrollar, `npm run watch` recompila al guardar. El sistema de diseño (tokens, componentes y puntos de quiebre) está documentado en [`frontend/DISENO.md`](frontend/DISENO.md), y las decisiones en [`docs/decisiones/frontend-typescript-vanilla.md`](docs/decisiones/frontend-typescript-vanilla.md).

Las pantallas de staff y las rutas que usan (agenda, directorio, cumplimiento) existen solo en los perfiles `dev`/`test`, como el resto de la superficie HTTP del proyecto. Para probar la reserva, generá un link desde **Agenda → Link de reserva**.

## Configuración

Las variables de entorno están listadas, sin valores, en [`.env.example`](.env.example). Los valores reales nunca se versionan (`.env` está en `.gitignore`).

| Grupo | Variables |
|---|---|
| General | `APP_BASE_URL`, `DATABASE_URL`, `TIMEZONE`, `CANCELLATION_WINDOW_HOURS` |
| Autenticación | `AUTH_JWT_SECRET`, `AUTH_JWT_ISSUER`, `AUTH_SESSION_COOKIE`, `AUTH_CSRF_COOKIE` |
| Acceso público | `PUBLIC_TOKEN_SECRET` |
| WhatsApp | `WHATSAPP_ACCESS_TOKEN`, `WHATSAPP_PHONE_NUMBER_ID`, `WHATSAPP_WEBHOOK_VERIFY_TOKEN`, `WHATSAPP_APP_SECRET` |
| Mercado Pago | `MERCADOPAGO_ACCESS_TOKEN`, `MERCADOPAGO_WEBHOOK_SECRET` |
| ARCA | `ARCA_BASE_URL`, `ARCA_ACCESS_TOKEN` |
| Respaldo y exportación | `EXPORT_RETENTION_YEARS`, `BACKUP_CRON`, `BACKUP_ENCRYPTION_KEY`, `BACKUP_DIR` |
| Restore aislado | `RESTORE_SANDBOX_URL`, `RESTORE_SANDBOX_USER`, `RESTORE_SANDBOX_PASSWORD` |
| IA conversacional | `IA_HANDOFF_ENABLED` (diferida, `false`) |

## Estructura del repositorio

```
src/main/java/com/turnosodontologia/
  domain/            Modelo de dominio (agenda, clínico, etc.)
  application/       Casos de uso; cumplimiento/ = exportación, respaldo y restore (C-17)
  infrastructure/    Persistencia y adaptadores
  interfaces/        Controladores HTTP
  auth/              Autenticación y RBAC
  audit/             Log de auditoría append-only
  publictoken/       Tokens de acceso público (link/QR)
  publicbooking/     Reserva y reprogramación pública
  historiapaciente/  Acceso del paciente a su historia clínica
  depositpayments/   Señas con Mercado Pago
  invoicing/         Facturación con ARCA
  waitlist/          Lista de espera y métricas de ocupación
src/main/resources/db/migration/   Migraciones Flyway
frontend/                          Frontend en TypeScript (src/, public/, DISENO.md)
src/test/                          Tests unitarios y de integración
knowledge-base/                    Base de conocimiento del dominio (fuente de verdad)
docs/decisiones/                   Registro de decisiones por change
docs/discovery/                    Investigación de mercado (solo lectura)
openspec/                          Especificaciones y changes (activos y archivados)
CHANGES.md                         Roadmap operativo: alcance, dependencias y estado
```

## Documentación

- **Dominio:** empezar por [`knowledge-base/README.md`](knowledge-base/README.md). Contiene visión, actores y RBAC, modelo de datos, reglas de negocio (`RN-XX-NN`), historias de usuario, flujos, arquitectura, decisiones y preguntas abiertas.
- **Plan y estado:** [`CHANGES.md`](CHANGES.md) es la única fuente operativa de alcance, dependencias y verificación.
- **Decisiones:** [`docs/decisiones/`](docs/decisiones/) registra, por change, cada decisión con su decisor y fecha.

## Estado de los changes

| Change | Descripción | Estado |
|---|---|---|
| C-01 | Decisiones habilitantes | Completado |
| C-02 | Scaffolding base | Completado |
| C-03 | Modelos core y migraciones | Completado |
| C-04 | Autenticación y RBAC | Completado |
| C-05 | Log de auditoría append-only | Completado |
| C-06 | Tokens de acceso público | Completado |
| C-07 | Catálogos de agenda | Completado |
| C-08 | Agenda y prevención de solapamientos | Completado |
| C-09 | Reserva y reprogramación pública | Completado |
| C-10 | Notificaciones WhatsApp (outbox) | Completado |
| C-11 | Historia clínica y odontograma FDI | Completado |
| C-12 | Firma clínica y consentimiento | Completado |
| C-13 | Acceso del paciente a su historia | Completado |
| C-14 | Presupuestos y caja | Completado |
| C-15 | Señas con Mercado Pago y conciliación | Completado |
| C-16 | Facturación ARCA y conciliación | Completado |
| C-17 | Exportación, respaldo y restore | Completado (unificado) |
| C-18 | Dossier de evidencia de cumplimiento | Implementado, pendiente de revisión |
| C-19 | Relleno automático de waitlist y métricas | Completado |
| C-20 | IA conversacional con handoff | Diferido sin código |
| C-21 | Precio predecible y costos | Implementado, pendiente de revisión |
| C-22 | Dashboard de métricas de adopción | Completado |

El detalle y la evidencia de cada change están en [`CHANGES.md`](CHANGES.md).

## Equipo

| Parte | Integrante | Changes |
|---|---|---|
| Parte 1: tronco base (PO) | Lautaro Ellerbach | C-01 a C-08 |
| Parte 2: notificaciones y clínica | José Donato Fornasari Donadel | C-10 a C-13, cierre de C-08, C-20 |
| Parte 3: reserva y finanzas | Augusto De Giuseppe | C-09, C-14 a C-16, C-19 |
| Parte 4: cumplimiento y métricas | Joaquín Adrián Velázquez Carrera | C-17, C-18, C-21, C-22 |
