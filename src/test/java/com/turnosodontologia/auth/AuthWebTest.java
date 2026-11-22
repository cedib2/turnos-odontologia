package com.turnosodontologia.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.turnosodontologia.application.AuditEventPublisher;
import com.turnosodontologia.application.CurrentActorProvider;
import com.turnosodontologia.application.RolService;
import com.turnosodontologia.application.SimulatedCurrentActorProvider;
import com.turnosodontologia.application.UsuarioService;
import com.turnosodontologia.domain.identidad.Ambito;
import com.turnosodontologia.domain.identidad.ProveedorAutenticacion;
import com.turnosodontologia.domain.identidad.Rol;
import com.turnosodontologia.domain.identidad.Usuario;
import com.turnosodontologia.infrastructure.audit.InMemoryAuditEventRecorder;
import com.turnosodontologia.infrastructure.persistence.UsuarioRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Value;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Timestamp;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = AuthWebTest.Config.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "spring.flyway.enabled=false",
                "spring.datasource.url=jdbc:h2:mem:authweb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
                "AUTH_JWT_SECRET=clave-de-prueba-no-real-para-tests",
                "auth.session-cookie=auth_session",
                "auth.refresh-cookie=auth_refresh",
                "auth.csrf-cookie=auth_csrf"
        })
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthWebTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private RolService rolService;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtService jwt;

    @Autowired
    private AuthService authService;

    @Value("${AUTH_JWT_SECRET}")
    private String jwtSecret;

    @Value("${AUTH_JWT_ISSUER:turnos-odontologia}")
    private String jwtIssuer;

    @SpyBean
    private AuthPasswordHasher authPasswordHasher;

    @Autowired
    private LoginRateLimiter loginRateLimiter;

    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void limpiarSesiones() {
        loginRateLimiter.limpiar();
        jdbcTemplate.update("DELETE FROM auth_tokens_refresh");
        jdbcTemplate.update("DELETE FROM auth_jti_revocados");
        jdbcTemplate.update("DELETE FROM auth_sesiones");
    }

    @Test
    void loginValidoConCsrfEmiteAccessJwtYRefreshOpacoLigadosASesion() throws Exception {
        Usuario usuario = crearUsuario("login-valido@example.com", "ClaveSegura123!");
        String csrf = csrf();

        MvcResult resultado = mockMvc.perform(post("/auth/login")
                        .cookie(new Cookie("auth_csrf", csrf))
                        .header("X-CSRF-Token", csrf)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of(
                                "email", "login-valido@example.com",
                                "password", "ClaveSegura123!"))))
                .andExpect(status().isOk())
                .andReturn();

        Cookie sesion = resultado.getResponse().getCookie("auth_session");
        Cookie refresh = resultado.getResponse().getCookie("auth_refresh");
        assertNotNull(sesion);
        assertNotNull(refresh);
        assertEquals(3, sesion.getValue().split("\\.").length);
        assertNotEquals(sesion.getValue(), refresh.getValue());
        assertTrue(refresh.getValue().length() >= 32);
        Integer sesiones = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM auth_sesiones WHERE usuario_id = ?", Integer.class, usuario.getId());
        Integer refreshes = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM auth_tokens_refresh WHERE hash_token IS NOT NULL", Integer.class);
        assertEquals(1, sesiones);
        assertEquals(1, refreshes);
    }

    @Test
    void snapshotDelActorRealSeTomaTrasLoginConModoYCti() {
        Usuario usuario = crearUsuario("actor-real@example.com", "ClaveSegura123!");

        AuthService.LoginResult resultado = authService.login(
                usuario.getEmail(), "ClaveSegura123!", "203.0.113.50", "test-agente");
        AuthenticatedActor actor = new RealActorProvider().desdeLogin(usuario.getEmail(), resultado);

        assertEquals(usuario.getId(), actor.usuarioId());
        assertEquals(usuario.getEmail(), actor.nombre());
        assertEquals(resultado.rol(), actor.rol());
        assertEquals(resultado.jti(), actor.jti());
        assertEquals(ModoActor.REAL, actor.modo());
    }

    @Test
    void loginConUsuarioInexistenteContrascenaIncorrectaEInactivoDanLaMisma401() throws Exception {
        Usuario conClave = crearUsuario("clave-incorrecta@example.com", "ClaveSegura123!");
        Usuario inactivo = crearUsuario("inactivo@example.com", "ClaveSegura123!");
        inactivo.desactivar("test");
        usuarioRepository.saveAndFlush(inactivo);
        int sesionesIniciales = totalSesiones();

        MvcResult inexistente = login("no-existe@example.com", "ClaveSegura123!");
        MvcResult incorrecta = login("clave-incorrecta@example.com", "OtraClave123!");
        MvcResult inactivoResultado = login("inactivo@example.com", "ClaveSegura123!");

        assertEquals(401, inexistente.getResponse().getStatus());
        assertEquals(inexistente.getResponse().getContentAsString(),
                incorrecta.getResponse().getContentAsString());
        assertEquals(inexistente.getResponse().getContentAsString(),
                inactivoResultado.getResponse().getContentAsString());
        assertFalse(inexistente.getResponse().getContentAsString().toLowerCase().contains("existe"));
        assertFalse(inexistente.getResponse().getContentAsString().toLowerCase().contains("clave"));
        assertFalse(inexistente.getResponse().getContentAsString().toLowerCase().contains("inactivo"));
        for (MvcResult resultado : List.of(inexistente, incorrecta, inactivoResultado)) {
            assertEquals(null, resultado.getResponse().getCookie("auth_session"));
            assertEquals(null, resultado.getResponse().getCookie("auth_refresh"));
        }
        assertEquals(sesionesIniciales, totalSesiones());
        assertNotNull(conClave);
    }

    @Test
    void cincoFallosPorIpYUsuarioBloqueanConRetryAfterSinEvaluarCredenciales() throws Exception {
        crearUsuario("rate-limit@example.com", "ClaveSegura123!");
        String csrf = csrf();

        for (int intento = 0; intento < 5; intento++) {
            MvcResult fallo = loginConIp("rate-limit@example.com", "ClaveIncorrecta123!", csrf, "203.0.113.7");
            assertEquals(401, fallo.getResponse().getStatus());
        }

        clearInvocations(authPasswordHasher);
        MvcResult bloqueado = loginConIp("rate-limit@example.com", "ClaveSegura123!", csrf, "203.0.113.7");

        assertEquals(429, bloqueado.getResponse().getStatus());
        String retryAfter = bloqueado.getResponse().getHeader("Retry-After");
        assertNotNull(retryAfter);
        assertTrue(Integer.parseInt(retryAfter) > 0);
        verify(authPasswordHasher, times(0)).matches(anyString(), anyString());
        assertNullCookie(bloqueado, "auth_session");
        assertNullCookie(bloqueado, "auth_refresh");
    }

    @Test
    void csrfAnonimoEsLegibleDistintoDeLaCookieDeSesionYSinValorAutorizativo() throws Exception {
        MvcResult resultado = mockMvc.perform(get("/auth/csrf"))
                .andExpect(status().isOk())
                .andReturn();

        Cookie csrf = resultado.getResponse().getCookie("auth_csrf");
        assertNotNull(csrf);
        assertFalse(csrf.isHttpOnly());
        assertTrue(csrf.getValue().length() >= 32);
        assertNullCookie(resultado, "auth_session");
        assertNullCookie(resultado, "auth_refresh");
    }

    @Test
    void cadenaDeSeguridadSoloCoincideConAuth() {
        FilterChainProxy proxy = applicationContext.getBean("springSecurityFilterChain", FilterChainProxy.class);
        List<SecurityFilterChain> cadenas = proxy.getFilterChains();
        assertEquals(1, cadenas.size());

        DefaultSecurityFilterChain cadena = (DefaultSecurityFilterChain) cadenas.get(0);
        assertTrue(cadena.getRequestMatcher().matches(new MockHttpServletRequest("GET", "/auth/me")));
        assertFalse(cadena.getRequestMatcher().matches(new MockHttpServletRequest("GET", "/agenda")));
    }

    @Test
    void loginSinCsrfYConCsrfIncorrectoResponde403SinTokensNiSesion() throws Exception {
        crearUsuario("csrf-faltante@example.com", "ClaveSegura123!");
        int sesionesIniciales = totalSesiones();

        MvcResult sinHeader = mockMvc.perform(post("/auth/login")
                        .cookie(new Cookie("auth_csrf", csrf()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credenciales("csrf-faltante@example.com", "ClaveSegura123!")))
                .andReturn();
        String csrf = csrf();
        MvcResult incorrecto = mockMvc.perform(post("/auth/login")
                        .cookie(new Cookie("auth_csrf", csrf))
                        .header("X-CSRF-Token", "valor-distinto")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credenciales("csrf-faltante@example.com", "ClaveSegura123!")))
                .andReturn();

        assertEquals(403, sinHeader.getResponse().getStatus());
        assertEquals(403, incorrecto.getResponse().getStatus());
        assertNullCookie(sinHeader, "auth_session");
        assertNullCookie(incorrecto, "auth_refresh");
        assertEquals(sesionesIniciales, totalSesiones());
    }

    @Test
    void loginValidoRotaCsrfYRechazaElValorAnterior() throws Exception {
        crearUsuario("csrf-rotado@example.com", "ClaveSegura123!");
        String anterior = csrf();

        MvcResult primero = login("csrf-rotado@example.com", "ClaveSegura123!", anterior);
        assertEquals(200, primero.getResponse().getStatus());
        Cookie rotado = primero.getResponse().getCookie("auth_csrf");
        assertNotNull(rotado);
        assertNotEquals(anterior, rotado.getValue());

        MvcResult rechazado = mockMvc.perform(post("/auth/login")
                        .cookie(new Cookie("auth_csrf", anterior))
                        .header("X-CSRF-Token", anterior)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credenciales("csrf-rotado@example.com", "ClaveSegura123!")))
                .andReturn();
        assertEquals(403, rechazado.getResponse().getStatus());
        assertNullCookie(rechazado, "auth_session");

        MvcResult segundo = login("csrf-rotado@example.com", "ClaveSegura123!", rotado.getValue());
        assertEquals(200, segundo.getResponse().getStatus());
    }

    @Test
    void cookiesDeSesionYRefreshSonHttpOnlySecureYSameSite() throws Exception {
        crearUsuario("cookies-seguras@example.com", "ClaveSegura123!");

        MvcResult resultado = login("cookies-seguras@example.com", "ClaveSegura123!");
        assertEquals(200, resultado.getResponse().getStatus());

        for (String nombre : List.of("auth_session", "auth_refresh")) {
            List<String> cabeceras = resultado.getResponse().getHeaders("Set-Cookie").stream()
                    .filter(cabecera -> cabecera.startsWith(nombre + "="))
                    .toList();
            assertEquals(1, cabeceras.size(), nombre);
            assertTrue(cabeceras.get(0).contains("HttpOnly"), cabeceras.get(0));
            assertTrue(cabeceras.get(0).contains("Secure"), cabeceras.get(0));
            assertTrue(cabeceras.get(0).contains("SameSite"), cabeceras.get(0));
        }
    }

    @Test
    void refreshSinCsrfResponde403SinRotar() throws Exception {
        crearUsuario("refresh-sin-csrf@example.com", "ClaveSegura123!");
        MvcResult login = login("refresh-sin-csrf@example.com", "ClaveSegura123!");
        assertEquals(200, login.getResponse().getStatus());
        String hashAntes = unicoHashRefresh();

        MvcResult resultado = mockMvc.perform(post("/auth/refresh")
                        .cookie(login.getResponse().getCookie("auth_session")))
                .andReturn();

        assertEquals(403, resultado.getResponse().getStatus());
        assertEquals(hashAntes, unicoHashRefresh());
    }

    @Test
    void rotacionExitosaEmiteNuevoParMarcaUsedAtYConservaFamilia() throws Exception {
        crearUsuario("rotacion-exitosa@example.com", "ClaveSegura123!");
        MvcResult login = login("rotacion-exitosa@example.com", "ClaveSegura123!");
        String refrescoViejo = login.getResponse().getCookie("auth_refresh").getValue();
        String familia = jdbcTemplate.queryForObject(
                "SELECT familia_id FROM auth_tokens_refresh WHERE hash_token = ?",
                String.class, hash(refrescoViejo));

        MvcResult resultado = refrescar(login, refrescoViejo);
        assertEquals(200, resultado.getResponse().getStatus());
        Cookie nuevoAccess = resultado.getResponse().getCookie("auth_session");
        Cookie nuevoRefresh = resultado.getResponse().getCookie("auth_refresh");
        assertNotNull(nuevoAccess);
        assertNotNull(nuevoRefresh);
        assertEquals(200, mockMvc.perform(get("/auth/me").cookie(nuevoAccess))
                .andReturn().getResponse().getStatus());
        assertNotEquals(refrescoViejo, nuevoRefresh.getValue());
        assertNotNull(jdbcTemplate.queryForObject(
                "SELECT used_at FROM auth_tokens_refresh WHERE hash_token = ?",
                Object.class, hash(refrescoViejo)));
        assertEquals(familia, jdbcTemplate.queryForObject(
                "SELECT familia_id FROM auth_tokens_refresh WHERE hash_token = ?",
                String.class, hash(nuevoRefresh.getValue())));
        Integer totalFamilia = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM auth_tokens_refresh WHERE familia_id = ?", Integer.class, familia);
        assertEquals(2, totalFamilia);
    }

    @Test
    void refreshVencidoResponde401SinEmitirTokens() throws Exception {
        crearUsuario("refresh-vencido@example.com", "ClaveSegura123!");
        MvcResult login = login("refresh-vencido@example.com", "ClaveSegura123!");
        String refresco = login.getResponse().getCookie("auth_refresh").getValue();
        jdbcTemplate.update("UPDATE auth_tokens_refresh SET expires_at = ? WHERE hash_token = ?",
                Timestamp.from(Instant.now().minusSeconds(60)), hash(refresco));
        int totalAntes = totalRefreshes();

        MvcResult resultado = refrescar(login, refresco);

        assertEquals(401, resultado.getResponse().getStatus());
        assertNullCookie(resultado, "auth_session");
        assertNullCookie(resultado, "auth_refresh");
        assertEquals(totalAntes, totalRefreshes());
    }

    @Test
    void reutilizacionDeRefreshRotadoRevocaFamiliaYResponde401() throws Exception {
        crearUsuario("reuso-refresh@example.com", "ClaveSegura123!");
        MvcResult login = login("reuso-refresh@example.com", "ClaveSegura123!");
        String refrescoViejo = login.getResponse().getCookie("auth_refresh").getValue();
        String familia = jdbcTemplate.queryForObject(
                "SELECT familia_id FROM auth_tokens_refresh WHERE hash_token = ?",
                String.class, hash(refrescoViejo));
        MvcResult primera = refrescar(login, refrescoViejo);
        assertEquals(200, primera.getResponse().getStatus());

        MvcResult reuso = refrescar(primera.getResponse().getCookie("auth_csrf"), refrescoViejo);

        assertEquals(401, reuso.getResponse().getStatus());
        assertNullCookie(reuso, "auth_session");
        assertNullCookie(reuso, "auth_refresh");
        Integer vigentes = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM auth_tokens_refresh WHERE familia_id = ? AND revoked_at IS NULL",
                Integer.class, familia);
        assertEquals(0, vigentes);
        Integer sesionesRevocadas = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM auth_sesiones s JOIN auth_tokens_refresh r ON r.sesion_id = s.id "
                        + "WHERE r.familia_id = ? AND s.estado = 'REVOCADA'", Integer.class, familia);
        assertTrue(sesionesRevocadas > 0);
    }

    @Test
    void accessConExpVencidoResponde401SinEfectos() throws Exception {
        String vencido = jwt.emitir(1L, "recepcionista", "jti-vencido",
                Instant.now().minusSeconds(16 * 60));
        int sesionesIniciales = totalSesiones();

        MvcResult resultado = mockMvc.perform(get("/auth/me")
                        .cookie(new Cookie("auth_session", vencido)))
                .andReturn();

        assertEquals(401, resultado.getResponse().getStatus());
        assertEquals(sesionesIniciales, totalSesiones());
        assertNullCookie(resultado, "auth_refresh");
    }

    @Test
    void refreshOpacoPresentadoComoAccessResponde401() throws Exception {
        crearUsuario("refresh-como-access@example.com", "ClaveSegura123!");
        MvcResult login = login("refresh-como-access@example.com", "ClaveSegura123!");
        assertEquals(200, login.getResponse().getStatus());
        String refresh = login.getResponse().getCookie("auth_refresh").getValue();

        MvcResult resultado = mockMvc.perform(get("/auth/me")
                        .cookie(new Cookie("auth_session", refresh)))
                .andReturn();

        assertEquals(401, resultado.getResponse().getStatus());
        assertNullCookie(resultado, "auth_session");
        assertNullCookie(resultado, "auth_refresh");
    }

    @Test
    void accessConFirmaInvalidaResponde401SinEfectos() throws Exception {
        String forjado = tokenConPayload(payloadAccess("jti-firma-invalida", true), "clave-ajena-a-la-de-prueba");
        int sesionesIniciales = totalSesiones();

        MvcResult resultado = mockMvc.perform(get("/auth/me")
                        .cookie(new Cookie("auth_session", forjado)))
                .andReturn();

        assertEquals(401, resultado.getResponse().getStatus());
        assertEquals(sesionesIniciales, totalSesiones());
        assertNullCookie(resultado, "auth_refresh");
    }

    @Test
    void accessSinJtiResponde401SinEfectos() throws Exception {
        String sinJti = tokenConPayload(payloadAccess(null, false), jwtSecret);
        int sesionesIniciales = totalSesiones();

        MvcResult resultado = mockMvc.perform(get("/auth/me")
                        .cookie(new Cookie("auth_session", sinJti)))
                .andReturn();

        assertEquals(401, resultado.getResponse().getStatus());
        assertEquals(sesionesIniciales, totalSesiones());
        assertNullCookie(resultado, "auth_refresh");
    }

    @Test
    void noExisteEndpointDeRegistroDeUsuarios() throws Exception {
        mockMvc.perform(post("/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credenciales("nuevo@example.com", "ClaveSegura123!")))
                .andExpect(status().isNotFound());
    }

    @Test
    void altaDeStaffPersisteHashBCryptNoTextoPlano() {
        Rol rol = rolService.registrar("administrador", "Administrador");

        Usuario usuario = usuarioService.registrar(
                rol, "hash-bcrypt@example.com", "ClaveSegura123!", ProveedorAutenticacion.LOCAL);

        assertNotEquals("ClaveSegura123!", usuario.getHashPassword());
        assertTrue(usuario.getHashPassword().matches("^\\$2[aby]\\$\\d{2}\\$.{53}$"),
                usuario.getHashPassword());
    }

    @Test
    void mismaCredencialProduceHashesBCryptDistintos() {
        Rol rol = rolService.registrar("odontologo", "Odontólogo");

        Usuario primero = usuarioService.registrar(
                rol, "hash-distinto-1@example.com", "ClaveSegura123!", ProveedorAutenticacion.LOCAL);
        Usuario segundo = usuarioService.registrar(
                rol, "hash-distinto-2@example.com", "ClaveSegura123!", ProveedorAutenticacion.LOCAL);

        assertNotEquals(primero.getHashPassword(), segundo.getHashPassword());
        assertTrue(primero.getHashPassword().startsWith("$2"));
        assertTrue(segundo.getHashPassword().startsWith("$2"));
    }

    @Test
    void tresCausasInvalidasEjecutanVerificacionBCryptConHashDummy() throws Exception {
        crearUsuario("timing-valido@example.com", "ClaveSegura123!");
        Usuario inactivo = crearUsuario("timing-inactivo@example.com", "ClaveSegura123!");
        inactivo.desactivar("test");
        usuarioRepository.saveAndFlush(inactivo);
        clearInvocations(authPasswordHasher);

        login("no-existe-timing@example.com", "ClaveSegura123!");
        login("timing-valido@example.com", "ClaveIncorrecta123!");
        login("timing-inactivo@example.com", "ClaveSegura123!");

        ArgumentCaptor<String> hashes = ArgumentCaptor.forClass(String.class);
        verify(authPasswordHasher, times(3)).matches(anyString(), hashes.capture());
        String dummy = authPasswordHasher.dummyVerificationHash();
        assertEquals(2, hashes.getAllValues().stream().filter(dummy::equals).count());
    }

    @Test
    void fixturesDeAuthCreanStaffConUsuarioServiceRegistrar() throws IOException {
        String raiz = "src/test/java/com/turnosodontologia/auth";
        String codigo;
        try (var paths = Files.walk(Path.of(raiz))) {
            codigo = paths.filter(Files::isRegularFile)
                    .filter(ruta -> ruta.toString().endsWith(".java"))
                    .map(ruta -> {
                        try {
                            return Files.readString(ruta);
                        } catch (IOException e) {
                            throw new IllegalStateException(e);
                        }
                    })
                    .reduce("", String::concat);
        }

        assertFalse(codigo.contains("new " + "Usuario("));
        assertTrue(codigo.contains("usuarioService.registrar("));
    }

    @Test
    void logoutConCsrfValidoRevocaSesionFamiliaYJtiYAccessPosteriorResponde401() throws Exception {
        crearUsuario("logout-valido@example.com", "ClaveSegura123!");
        MvcResult inicio = login("logout-valido@example.com", "ClaveSegura123!");
        Cookie acceso = inicio.getResponse().getCookie("auth_session");
        Cookie csrf = inicio.getResponse().getCookie("auth_csrf");
        String jti = jwt.verificar(acceso.getValue()).jti();

        mockMvc.perform(post("/auth/logout")
                        .cookie(acceso)
                        .cookie(csrf)
                        .header("X-CSRF-Token", csrf.getValue()))
                .andExpect(status().isNoContent());

        assertEquals("REVOCADA", jdbcTemplate.queryForObject(
                "SELECT estado FROM auth_sesiones WHERE jti = ?", String.class, jti));
        assertNotNull(jdbcTemplate.queryForObject(
                "SELECT revoked_at FROM auth_sesiones WHERE jti = ?", Timestamp.class, jti));
        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM auth_tokens_refresh WHERE revoked_at IS NULL", Integer.class));
        assertTrue(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM auth_jti_revocados WHERE jti = ?", Integer.class, jti) > 0);

        mockMvc.perform(get("/auth/me").cookie(acceso))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutSinCsrfNoRevocaNada() throws Exception {
        crearUsuario("logout-sin-csrf@example.com", "ClaveSegura123!");
        MvcResult inicio = login("logout-sin-csrf@example.com", "ClaveSegura123!");
        Cookie acceso = inicio.getResponse().getCookie("auth_session");
        Cookie csrf = inicio.getResponse().getCookie("auth_csrf");
        String jti = jwt.verificar(acceso.getValue()).jti();

        mockMvc.perform(post("/auth/logout")
                        .cookie(acceso)
                        .cookie(csrf))
                .andExpect(status().isForbidden());

        assertEquals("ACTIVA", jdbcTemplate.queryForObject(
                "SELECT estado FROM auth_sesiones WHERE jti = ?", String.class, jti));
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM auth_tokens_refresh WHERE revoked_at IS NULL", Integer.class));
        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM auth_jti_revocados WHERE jti = ?", Integer.class, jti));
        mockMvc.perform(get("/auth/me").cookie(acceso))
                .andExpect(status().isOk());
    }

    @Test
    void logoutDeUnaSesionNoAfectaOtraFamilia() throws Exception {
        crearUsuario("logout-uno@example.com", "ClaveSegura123!");
        crearUsuario("logout-dos@example.com", "ClaveSegura123!");
        MvcResult uno = login("logout-uno@example.com", "ClaveSegura123!");
        MvcResult dos = login("logout-dos@example.com", "ClaveSegura123!");
        Cookie csrfUno = uno.getResponse().getCookie("auth_csrf");
        String jtiDos = jwt.verificar(dos.getResponse().getCookie("auth_session").getValue()).jti();

        mockMvc.perform(post("/auth/logout")
                        .cookie(uno.getResponse().getCookie("auth_session"))
                        .cookie(csrfUno)
                        .header("X-CSRF-Token", csrfUno.getValue()))
                .andExpect(status().isNoContent());

        assertEquals("ACTIVA", jdbcTemplate.queryForObject(
                "SELECT estado FROM auth_sesiones WHERE jti = ?", String.class, jtiDos));
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM auth_tokens_refresh r JOIN auth_sesiones s ON s.id = r.sesion_id "
                        + "WHERE s.jti = ? AND r.revoked_at IS NULL", Integer.class, jtiDos));
        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM auth_jti_revocados WHERE jti = ?", Integer.class, jtiDos));
        mockMvc.perform(get("/auth/me").cookie(dos.getResponse().getCookie("auth_session")))
                .andExpect(status().isOk());
    }

    @Test
    void rolSinPermisoNoPuedeConsultarMe() throws Exception {
        crearUsuarioSinPermiso("sin-permiso-me@example.com");
        MvcResult inicio = login("sin-permiso-me@example.com", "ClaveSegura123!");

        mockMvc.perform(get("/auth/me").cookie(inicio.getResponse().getCookie("auth_session")))
                .andExpect(status().isForbidden());
    }

    @Test
    void meConPermisoPropioCoincideConElSubDelToken() throws Exception {
        Usuario usuario = crearUsuario("me-propio@example.com", "ClaveSegura123!");
        MvcResult inicio = login("me-propio@example.com", "ClaveSegura123!");

        MvcResult respuesta = mockMvc.perform(
                        get("/auth/me").cookie(inicio.getResponse().getCookie("auth_session")))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode cuerpo = mapper.readTree(respuesta.getResponse().getContentAsString());

        assertEquals(usuario.getId(), cuerpo.get("usuario_id").asLong());
        assertFalse(cuerpo.toString().contains("hash"));
    }

    @Test
    void ambitoPropioNoAutorizaIdentidadDeOtroTitular() throws Exception {
        crearUsuarioConAmbito("actor-propio@example.com", Ambito.PROPIO);
        Usuario consultado = crearUsuarioConAmbito("consultado-propio@example.com", Ambito.PROPIO);
        MvcResult inicio = login("actor-propio@example.com", "ClaveSegura123!");

        MvcResult respuesta = mockMvc.perform(get("/auth/usuarios/{id}", consultado.getId())
                        .cookie(inicio.getResponse().getCookie("auth_session")))
                .andExpect(status().isForbidden())
                .andReturn();

        assertFalse(respuesta.getResponse().getContentAsString().contains(consultado.getEmail()));
    }

    @Test
    void ambitoTodosAutorizaIdentidadDeStaffSinDatosClinicos() throws Exception {
        crearUsuarioConAmbito("actor-todos@example.com", Ambito.TODOS);
        Usuario consultado = crearUsuarioConAmbito("consultado-todos@example.com", Ambito.PROPIO);
        MvcResult inicio = login("actor-todos@example.com", "ClaveSegura123!");

        MvcResult respuesta = mockMvc.perform(get("/auth/usuarios/{id}", consultado.getId())
                        .cookie(inicio.getResponse().getCookie("auth_session")))
                .andExpect(status().isOk())
                .andReturn();
        String cuerpo = respuesta.getResponse().getContentAsString();

        assertTrue(cuerpo.contains(consultado.getEmail()));
        assertTrue(cuerpo.contains(consultado.getRol().getCodigo()));
        assertFalse(cuerpo.contains("hash"));
        assertFalse(cuerpo.contains("clinic"));
        assertFalse(cuerpo.contains("password"));
    }

    @Test
    void noHayHerenciaDePermisosEntreRoles() throws Exception {
        crearUsuarioConAmbito("con-todos@example.com", Ambito.TODOS);
        crearUsuarioSinPermiso("sin-herencia@example.com");
        Usuario consultado = crearUsuarioConAmbito("consultado-herencia@example.com", Ambito.PROPIO);
        MvcResult inicio = login("sin-herencia@example.com", "ClaveSegura123!");

        mockMvc.perform(get("/auth/usuarios/{id}", consultado.getId())
                        .cookie(inicio.getResponse().getCookie("auth_session")))
                .andExpect(status().isForbidden());
    }

    @Test
    void rolPacienteSinUsuarioNoPuedeHacerLoginDeStaff() throws Exception {
        rolService.registrar("paciente", "Paciente");
        MvcResult resultado = login("paciente@example.com", "ClaveSegura123!");

        assertEquals(401, resultado.getResponse().getStatus());
        assertEquals(0, usuarioRepository.findAll().stream()
                .filter(usuario -> "paciente".equals(usuario.getRol().getCodigo()))
                .count());
    }

    @Test
    void permisoConfigNoAutorizaIdentidadAjena() throws Exception {
        crearUsuarioConAmbito("actor-config@example.com", Ambito.CONFIG);
        Usuario consultado = crearUsuarioConAmbito("consultado-config@example.com", Ambito.PROPIO);
        MvcResult inicio = login("actor-config@example.com", "ClaveSegura123!");

        mockMvc.perform(get("/auth/usuarios/{id}", consultado.getId())
                        .cookie(inicio.getResponse().getCookie("auth_session")))
                .andExpect(status().isForbidden());
    }

    private MvcResult refrescar(MvcResult login, String refresco) throws Exception {
        return refrescar(login.getResponse().getCookie("auth_csrf"), refresco);
    }

    private MvcResult refrescar(Cookie csrf, String refresco) throws Exception {
        return mockMvc.perform(post("/auth/refresh")
                        .cookie(new Cookie("auth_refresh", refresco))
                        .cookie(csrf)
                        .header("X-CSRF-Token", csrf.getValue()))
                .andReturn();
    }

    private String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private int totalRefreshes() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM auth_tokens_refresh", Integer.class);
    }

    private String payloadAccess(String jti, boolean incluirJti) throws Exception {
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("sub", "1");
        claims.put("rol", "recepcionista");
        if (incluirJti) {
            claims.put("jti", jti);
        }
        claims.put("iss", jwtIssuer);
        claims.put("iat", Instant.now().getEpochSecond());
        claims.put("exp", Instant.now().plusSeconds(900).getEpochSecond());
        return mapper.writeValueAsString(claims);
    }

    private String tokenConPayload(String payloadJson, String secret) throws Exception {
        String unsigned = base64(mapper.writeValueAsString(Map.of(
                "alg", "HS256", "typ", "JWT"))) + "." + base64(payloadJson);
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return unsigned + "." + base64(mac.doFinal(unsigned.getBytes(StandardCharsets.UTF_8)));
    }

    private String base64(String value) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private String base64(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    private String csrf() throws Exception {
        MvcResult resultado = mockMvc.perform(get("/auth/csrf"))
                .andExpect(status().isOk())
                .andReturn();
        Cookie cookie = resultado.getResponse().getCookie("auth_csrf");
        assertNotNull(cookie);
        return cookie.getValue();
    }

    private MvcResult login(String email, String password) throws Exception {
        return login(email, password, csrf());
    }

    private MvcResult login(String email, String password, String csrf) throws Exception {
        return loginConIp(email, password, csrf, "198.51.100.1");
    }

    private MvcResult loginConIp(String email, String password, String csrf, String ip) throws Exception {
        return mockMvc.perform(post("/auth/login")
                        .cookie(new Cookie("auth_csrf", csrf))
                        .header("X-CSRF-Token", csrf)
                        .header("X-Forwarded-For", ip)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credenciales(email, password)))
                .andReturn();
    }

    private String credenciales(String email, String password) throws Exception {
        return mapper.writeValueAsString(Map.of("email", email, "password", password));
    }

    private Usuario crearUsuario(String email, String password) {
        return crearUsuarioConAmbito(email, password, Ambito.PROPIO);
    }

    private Usuario crearUsuarioConAmbito(String email, Ambito ambito) {
        return crearUsuarioConAmbito(email, "ClaveSegura123!", ambito);
    }

    private Usuario crearUsuarioConAmbito(String email, String password, Ambito ambito) {
        Rol rol = rolService.registrar("rol-" + email.hashCode() + "-" + ambito, ambito.name());
        jdbcTemplate.update("INSERT INTO permisos (id, recurso, accion, ambito, created_at, activo) "
                        + "SELECT NEXT VALUE FOR permisos_id_seq, 'auth', 'leer', ?, CURRENT_TIMESTAMP, TRUE "
                        + "WHERE NOT EXISTS (SELECT 1 FROM permisos "
                        + "WHERE recurso = 'auth' AND accion = 'leer' AND ambito = ?)",
                ambito.name(), ambito.name());
        Long permiso = jdbcTemplate.queryForObject("SELECT id FROM permisos "
                + "WHERE recurso = 'auth' AND accion = 'leer' AND ambito = ?", Long.class, ambito.name());
        jdbcTemplate.update("INSERT INTO rol_permiso (rol_id, permiso_id, created_at, activo) "
                + "VALUES (?, ?, CURRENT_TIMESTAMP, TRUE)", rol.getId(), permiso);
        return usuarioService.registrar(rol, email, password, ProveedorAutenticacion.LOCAL);
    }

    private Usuario crearUsuarioSinPermiso(String email) {
        Rol rol = rolService.registrar("sin-permiso-" + email.hashCode(), "Sin permiso");
        return usuarioService.registrar(rol, email, "ClaveSegura123!", ProveedorAutenticacion.LOCAL);
    }

    private int totalSesiones() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM auth_sesiones", Integer.class);
    }

    private String unicoHashRefresh() {
        return jdbcTemplate.queryForObject("SELECT hash_token FROM auth_tokens_refresh", String.class);
    }

    private void assertNullCookie(MvcResult resultado, String nombre) {
        assertEquals(null, resultado.getResponse().getCookie(nombre));
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackages = {"com.turnosodontologia.domain", "com.turnosodontologia.auth"})
    @ComponentScan(
            basePackages = {"com.turnosodontologia.application",
                    "com.turnosodontologia.infrastructure.audit", "com.turnosodontologia.auth"},
            excludeFilters = @ComponentScan.Filter(type = FilterType.ANNOTATION, classes = SpringBootConfiguration.class)
    )
    @EnableJpaRepositories(basePackages = {"com.turnosodontologia.infrastructure.persistence",
            "com.turnosodontologia.auth"})
    static class Config {

        @Bean
        @ConditionalOnMissingBean(CurrentActorProvider.class)
        CurrentActorProvider currentActorProvider() {
            return new SimulatedCurrentActorProvider();
        }

        @Bean
        @ConditionalOnMissingBean(AuditEventPublisher.class)
        AuditEventPublisher auditEventPublisher() {
            return new InMemoryAuditEventRecorder();
        }
    }
}
