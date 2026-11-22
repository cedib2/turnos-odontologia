# auth-rbac Specification

## Purpose

Define el contrato observable de autenticación y autorización de staff: emisión, rotación y revocación de credenciales, identidad del actor, denegación por rol y ámbito, límite de intentos y aislamiento de la superficie `/auth/**` frente a la agenda sin autenticación. C-04 entrega la infraestructura RBAC pero aún NO protege la agenda: `/agenda`, `/turnos`, `/bloqueos`, `/sobreturnos` y catálogos siguen sin autenticación en dev/test y su protección queda para un change posterior.

## Requirements

### Requirement: Login de staff con emisión de par de tokens
El sistema SHALL autenticar en `POST /auth/login` a usuarios de staff y emitir un access JWT de 15 minutos y un refresh opaco de 7 días ligados a una sesión persistida. Usuario inexistente, contraseña incorrecta e inactivo MUST dar la misma 401 en código, cuerpo y trabajo de verificación (BCrypt con hash dummy para inexistente/inactivo), sin tokens, sesión ni pista de la causa.

#### Scenario: Login válido
- **GIVEN** un usuario activo de staff con credenciales válidas y un rol sembrado
- **WHEN** se envía `POST /auth/login` con esas credenciales y el CSRF correspondiente
- **THEN** el sistema emite un access JWT de 15 minutos y un refresh opaco de 7 días ligados a una sesión persistida

#### Scenario: Usuario inexistente
- **GIVEN** un identificador de usuario que no existe en el sistema
- **WHEN** se intenta `POST /auth/login` con ese identificador y cualquier contraseña
- **THEN** el sistema responde la misma 401 que para las demás causas, sin emitir tokens, sin crear sesión y sin revelar que el usuario no existe

#### Scenario: Contraseña incorrecta
- **GIVEN** un usuario activo de staff
- **WHEN** se intenta `POST /auth/login` con una contraseña incorrecta
- **THEN** el sistema responde la misma 401 que para las demás causas, sin emitir tokens, sin crear sesión y sin revelar la causa

#### Scenario: Usuario inactivo
- **GIVEN** un usuario de staff inactivo con contraseña correcta
- **WHEN** se intenta `POST /auth/login`
- **THEN** el sistema responde la misma 401 que para las demás causas, sin emitir tokens, sin crear sesión y sin revelar la causa

#### Scenario: Trabajo de verificación uniforme
- **GIVEN** tres intentos de login con usuario inexistente, contraseña incorrecta y usuario inactivo
- **WHEN** se instrumenta la verificación de credenciales
- **THEN** los tres ejecutan la verificación BCrypt (el inexistente y el inactivo contra un hash dummy), sin retorno anticipado que permita distinguir la causa por tiempo de respuesta

### Requirement: Transporte seguro de credenciales y protección CSRF
El sistema SHALL entregar los tokens en cookie con atributos `HttpOnly`, `Secure` y `SameSite`, y MUST exigir CSRF válido en toda operación de `/auth/*` que cambie estado. Antes del login, `GET /auth/csrf` emite una cookie CSRF anónima de doble envío, distinta de la de sesión, y `POST /auth/login` exige su valor en `X-CSRF-Token`. La cookie CSRF no es credencial ni autoriza nada; la de sesión sigue siendo `HttpOnly`/`Secure`/`SameSite` y MUST NOT ser legible desde JS.

#### Scenario: Cookie CSRF anónima
- **GIVEN** un cliente sin sesión ni token
- **WHEN** solicita `GET /auth/csrf`
- **THEN** el sistema emite una cookie CSRF anónima de doble envío, distinta de la cookie de sesión, legible por el cliente para reflejarla y sin valor autorizativo

#### Scenario: Atributos de la cookie de sesión
- **GIVEN** un login válido con CSRF
- **WHEN** se inspecciona la respuesta HTTP y se intenta leer la cookie de sesión desde código cliente
- **THEN** la cookie de sesión declara `HttpOnly`, `Secure` y `SameSite` y no es accesible desde JavaScript, a diferencia de la cookie CSRF

#### Scenario: Login sin CSRF o con CSRF incorrecto
- **GIVEN** credenciales válidas y un cliente que omite `X-CSRF-Token` o envía un valor que no coincide con la cookie CSRF
- **WHEN** envía `POST /auth/login`
- **THEN** el sistema responde 403 y no emite tokens ni crea sesión

#### Scenario: Login válido con CSRF y rotación
- **GIVEN** credenciales válidas y el valor de la cookie CSRF anónima reflejado en `X-CSRF-Token`
- **WHEN** envía `POST /auth/login`
- **THEN** el sistema emite el par de tokens, rota/reemplaza la cookie CSRF y deja de aceptar el valor anterior

#### Scenario: Mutación sin CSRF
- **GIVEN** una cookie de sesión válida
- **WHEN** se envía `POST /auth/refresh` sin token CSRF
- **THEN** el sistema responde 403 y no rota ni emite tokens

### Requirement: Hash adaptativo de contraseñas
El sistema SHALL almacenar las contraseñas de usuarios de staff con hash adaptativo **BCrypt**, confirmado por el PO el 2026-11-21 y registrado en `design.md` (RN-GL-03). El alta de usuarios de staff SHALL hacerse con el servicio interno existente `UsuarioService.registrar(...)`, con `hash_password` generado por BCrypt, sin endpoint de registro. El sistema MUST NOT almacenar contraseñas en texto plano ni escribir contraseñas o hashes de contraseña en logs.

#### Scenario: Registro con credencial válida
- **GIVEN** un usuario de staff con credencial válida y hash BCrypt
- **WHEN** se registra al usuario mediante `UsuarioService.registrar(...)` y se inspecciona el valor persistido
- **THEN** el valor persistido no es texto plano y coincide con el formato de hash BCrypt

#### Scenario: Login con credencial válida
- **GIVEN** un usuario de staff con contraseña ya almacenada como hash BCrypt y credencial válida
- **WHEN** inicia sesión y se inspecciona el valor persistido
- **THEN** el valor persistido sigue siendo un hash BCrypt, nunca la contraseña en texto plano, y la autenticación coincide con ese hash

#### Scenario: Logs sin contraseñas ni hashes
- **GIVEN** operaciones de login, refresh y logout con credenciales reales
- **WHEN** se inspeccionan los logs producidos
- **THEN** no aparece la contraseña en claro ni el hash de la contraseña

### Requirement: Rechazo de access token expirado
El sistema MUST rechazar con 401 un access token JWT cuyo tiempo de expiración haya vencido y MUST NOT aceptar un refresh token en lugar del access token.

#### Scenario: Access vencido
- **GIVEN** un access token emitido con vigencia de 15 minutos y un reloj posterior a su `exp`
- **WHEN** se presenta en una operación autenticada
- **THEN** el sistema responde 401 y exige renovación mediante refresh

#### Scenario: Refresh usado como access
- **GIVEN** un refresh token opaco vigente
- **WHEN** se presenta donde el sistema espera el access token
- **THEN** el sistema lo rechaza con 401 y no lo interpreta como identidad

### Requirement: Renovación con rotación de refresh
El sistema SHALL canjear en `POST /auth/refresh` un refresh opaco vigente por un nuevo par access/refresh, marcando el token presentado como usado y conservando la familia de la sesión.

#### Scenario: Rotación exitosa
- **GIVEN** un refresh token vigente, no usado y no revocado
- **WHEN** se solicita `POST /auth/refresh` con CSRF válido
- **THEN** el sistema emite un nuevo access y un nuevo refresh, marca el anterior como usado y mantiene la misma familia

#### Scenario: Refresh vencido
- **GIVEN** un refresh token cuya vigencia de 7 días haya expirado
- **WHEN** se solicita renovación
- **THEN** el sistema responde 401 y no emite tokens nuevos

### Requirement: Detección de reutilización de refresh
El sistema MUST detectar la presentación de un refresh ya rotado o revocado, revocar la familia completa de tokens y rechazar el intento.

#### Scenario: Reuso tras rotación
- **GIVEN** un refresh token ya marcado como usado por una rotación previa
- **WHEN** vuelve a presentarse en `/auth/refresh`
- **THEN** el sistema responde 401 y revoca todos los tokens de esa familia

#### Scenario: Reuso de familia revocada
- **GIVEN** una familia de refresh revocada por logout
- **WHEN** se presenta cualquiera de sus refresh tokens
- **THEN** el sistema responde 401 y no reemite credenciales

### Requirement: Logout con revocación de sesión y familia
El sistema SHALL revocar en `POST /auth/logout` la sesión activa y la familia de refresh asociada, e invalidar el `jti` del access token vigente.

#### Scenario: Logout válido
- **GIVEN** una sesión activa con access y refresh vigentes
- **WHEN** se envía `POST /auth/logout` con CSRF válido
- **THEN** la sesión queda revocada, la familia de refresh se revoca y el `jti` del access queda registrado como revocado

#### Scenario: Uso de access después del logout
- **GIVEN** un access token emitido antes de un logout completado
- **WHEN** se presenta nuevamente
- **THEN** el sistema responde 401 porque su `jti` está revocado

### Requirement: Denegación por rol sin herencia
El sistema MUST denegar con 403 una operación protegida cuando el rol del actor no tenga el permiso `recurso`/`accion` en `rol_permiso`, sin herencia implícita entre roles. La operación protegida de referencia para los 403 es `GET /auth/me` (`propio`) o `GET /auth/usuarios/{id}` (identidad de otro usuario de staff, sin datos clínicos) para `todos`/administrador. C-04 entrega infraestructura RBAC de `/auth/**` pero aún NO protege la agenda.

#### Scenario: Definición de ámbitos y operación de referencia
- **GIVEN** la matriz de permisos planos sembrada en C-03
- **WHEN** se evalúan los ámbitos `propio`, `todos` y `config`
- **THEN** `propio` exige que el identificador del recurso coincida con el `sub` del access token del actor (referencia `GET /auth/me`), `todos` autoriza el mismo `recurso`/`accion` sobre cualquier titular (referencia `GET /auth/usuarios/{id}`, sin datos clínicos) y `config` son permisos de configuración

#### Scenario: Rol sin permiso
- **GIVEN** un usuario autenticado con un rol que no posee el permiso requerido sobre `GET /auth/me` en ámbito `propio`
- **WHEN** solicita `GET /auth/me`
- **THEN** el sistema responde 403 y la operación no produce efectos

#### Scenario: Permiso explícito de otro rol
- **GIVEN** dos roles distintos sin herencia entre sí y un actor con un rol sin el permiso de consulta en ámbito `todos`
- **WHEN** ese rol intenta `GET /auth/usuarios/{id}` para consultar la identidad de otro usuario de staff sin datos clínicos
- **THEN** el sistema no concede acceso por pertenencia a otro rol y responde 403

### Requirement: Aplicación del ámbito de permisos
El sistema MUST evaluar los ámbitos `propio`, `todos` y `config` de los permisos planos y denegar la operación cuando el ámbito del actor sea insuficiente. `propio` = coincide con el `sub` del access token; `todos` = mismo `recurso`/`accion` sobre cualquier titular; `config` = permisos de configuración. C-04 entrega infraestructura RBAC de `/auth/**` pero aún NO protege la agenda; su protección queda para un change posterior.

#### Scenario: Ámbito propio sobre GET /auth/me
- **GIVEN** un actor autenticado con un access token y un `sub`
- **WHEN** solicita `GET /auth/me`
- **THEN** el sistema autoriza la operación porque el identificador del recurso coincide con el `sub` del token

#### Scenario: Ámbito propio insuficiente
- **GIVEN** un actor con permiso de ámbito `propio` sobre un recurso de otro titular
- **WHEN** solicita `GET /auth/usuarios/{id}` para un titular cuyo identificador no coincide con el `sub` de su token
- **THEN** el sistema responde 403, no revela la identidad consultada y no produce efectos

#### Scenario: Ámbito todos suficiente
- **GIVEN** un actor con permiso de ámbito `todos` para la misma operación
- **WHEN** solicita `GET /auth/usuarios/{id}` sobre cualquier titular
- **THEN** el sistema autoriza la operación y devuelve solo la identidad del usuario de staff consultado, sin datos clínicos

### Requirement: Límite de intentos fallidos por IP y usuario
El sistema SHALL bloquear con 429 y `Retry-After` los intentos de login que superen 5 fallos en 15 minutos. El conteo SHALL ser por combinación IP + usuario, sin contadores que se suman ni la regla "cualquiera por separado"; el login exitoso lo reinicia y la ventana parte del primer fallo. El límite SHALL ser en memoria y no distribuido: se esquiva rotando de IP (cada IP nueva con el mismo usuario tiene contador propio), no se comparte entre réplicas y se pierde al reiniciar el proceso.

#### Scenario: Bloqueo tras cinco fallos
- **GIVEN** 5 intentos fallidos de login para la misma combinación IP + usuario dentro de una ventana de 15 minutos medida desde el primer fallo
- **WHEN** se realiza un nuevo intento
- **THEN** el sistema responde 429 con `Retry-After` y no evalúa credenciales

#### Scenario: Fin de la ventana
- **GIVEN** una combinación IP + usuario bloqueada hace más de 15 minutos desde su primer fallo
- **WHEN** se intenta login nuevamente
- **THEN** el sistema vuelve a evaluar credenciales en lugar de responder 429

#### Scenario: Login exitoso reinicia el contador
- **GIVEN** 4 fallos acumulados para una combinación IP + usuario
- **WHEN** esa misma combinación inicia sesión con credenciales válidas
- **THEN** el contador de esa combinación se reinicia y 4 nuevos fallos no producen 429

#### Scenario: Contadores separados por combinación IP + usuario
- **GIVEN** fallos acumulados para una combinación IP + usuario
- **WHEN** falla un usuario distinto desde la misma IP y el mismo usuario desde otra IP
- **THEN** ambas combinaciones mantienen contadores separados y ninguna hereda los fallos de la otra

#### Scenario: Límite no distribuido documentado
- **GIVEN** el documento de diseño y decisiones de C-04
- **WHEN** se inspecciona su declaración sobre el límite
- **THEN** declara explícitamente que el límite es en memoria y no distribuido, que se esquiva rotando de IP (cada IP nueva con el mismo usuario tiene contador propio), que no se comparte entre réplicas y que se pierde al reiniciar el proceso

### Requirement: Secretos fuera de logs y repositorio
El sistema MUST tomar claves de firma y credenciales desde variables de entorno, MUST NOT registrarlas en logs ni versionarlas, y SHALL publicar en `.env.example` solo nombres de variables sin valores.

#### Scenario: Logs sin secretos
- **GIVEN** operaciones de login, refresh y logout con credenciales reales
- **WHEN** se inspeccionan los logs producidos
- **THEN** no aparecen contraseñas, hashes de contraseña, tokens, claves de firma ni hashes de refresh

#### Scenario: Repositorio sin valores
- **GIVEN** el repositorio con `.gitignore` y `.env.example`
- **WHEN** se inspeccionan ambos archivos
- **THEN** `.env` está ignorado y `.env.example` contiene solo nombres de variables sin valores, sin claves en el código

### Requirement: Contrato de actor con modo y snapshot inmutable
El sistema SHALL representar al actor con `modo ∈ {REAL, SIMULADO, SYSTEM, PACIENTE}` y un snapshot inmutable tomado al autenticar, y SHALL mantener un proveedor real separado del `CurrentActorProvider` simulado existente.

#### Scenario: Actor real autenticado
- **GIVEN** un login de staff válido
- **WHEN** se construye el actor de la operación
- **THEN** el snapshot tiene `modo=REAL`, identificador, nombre, rol y `jti`, y no puede modificarse después

#### Scenario: Proveedor simulado conservado
- **GIVEN** el perfil dev/test y el proveedor simulado vigente
- **WHEN** el sistema consulta el actor para operaciones sin autenticación
- **THEN** sigue devolviendo `recepcionista`/`system` con `modo=SIMULADO` y el emisor de auditoría sigue en memoria

### Requirement: Persistencia de sesiones y tokens en V002
El sistema SHALL persistir en V002 sesiones con `jti` único, refresh tokens con hash único, familia y rotación, y revocaciones por sesión y por `jti`, con índices y checks coherentes con V001/V006 y sin borrado físico.

#### Scenario: Unicidad de jti y hash
- **GIVEN** la migración V002 aplicada en PostgreSQL
- **WHEN** se intenta insertar un `jti` o un hash de refresh duplicado
- **THEN** la base rechaza la escritura por constraint de unicidad

#### Scenario: Revocación por sesión y por jti
- **GIVEN** una sesión con un access `jti` y refresh tokens asociados
- **WHEN** se revoca la sesión
- **THEN** quedan registradas la revocación de la sesión y del `jti`, sin eliminar filas

### Requirement: Migración V002 con Flyway out-of-order en dev/test
El sistema SHALL habilitar `spring.flyway.out-of-order=true` en dev/test para que V002 se aplique en bases donde V005/V006 ya están registradas, sin reescribir migraciones aplicadas.

#### Scenario: Base con V006 registrada
- **GIVEN** una base dev/test con V005 y V006 aplicadas
- **WHEN** arranca la aplicación con la opción out-of-order habilitada
- **THEN** Flyway aplica V002 y la registra como migración válida

#### Scenario: Base nueva
- **GIVEN** una base sin migraciones aplicadas
- **WHEN** arranca la aplicación
- **THEN** V002 se aplica en orden junto a las demás migraciones

### Requirement: Aislamiento de la superficie autenticada
El sistema SHALL confinar `/auth/**` al paquete de autenticación con una cadena de seguridad restringida mediante `securityMatcher("/auth/**")`, y MUST NOT instalar una cadena global que proteja `/agenda`, `/turnos`, `/bloqueos`, `/sobreturnos` ni los catálogos de dev/test. C-04 entrega la infraestructura RBAC pero aún NO protege la agenda: esas rutas y los catálogos siguen sin autenticación en dev/test y su protección queda para un change posterior.

#### Scenario: Cadena limitada a /auth
- **GIVEN** la configuración de seguridad de la aplicación
- **WHEN** se inspeccionan las cadenas registradas
- **THEN** solo la cadena con `securityMatcher("/auth/**")` exige autenticación y no existe cadena global que cubra agenda o catálogos

#### Scenario: Rutas de agenda sin autenticación
- **GIVEN** el perfil dev/test activo
- **WHEN** se invoca una ruta de agenda o catálogo
- **THEN** responde sin credenciales y sin leer cabeceras ni cookies de identidad

#### Scenario: Confinamiento de mappings de /auth
- **GIVEN** el código fuente de la aplicación
- **WHEN** se inspeccionan los controladores con mappings `/auth`
- **THEN** todos pertenecen al paquete de autenticación y ningún controlador de agenda o catálogo expone rutas `/auth`

#### Scenario: Protección de agenda diferida
- **GIVEN** el alcance declarado de C-04 en este spec y en el documento de decisiones
- **WHEN** se inspecciona la declaración de alcance
- **THEN** declara que C-04 entrega infraestructura RBAC pero aún no protege la agenda, quedando esa protección para un change posterior
