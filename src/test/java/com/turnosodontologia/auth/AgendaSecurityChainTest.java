package com.turnosodontologia.auth;

import com.turnosodontologia.application.AuditEventPublisher;
import com.turnosodontologia.application.CurrentActorProvider;
import com.turnosodontologia.application.RolService;
import com.turnosodontologia.application.UsuarioService;
import com.turnosodontologia.domain.auditoria.ActorSimulado;
import com.turnosodontologia.domain.identidad.ProveedorAutenticacion;
import com.turnosodontologia.domain.identidad.Rol;
import com.turnosodontologia.domain.identidad.Usuario;
import com.turnosodontologia.infrastructure.audit.InMemoryAuditEventRecorder;
import com.turnosodontologia.infrastructure.persistence.RolRepository;
import com.turnosodontologia.infrastructure.persistence.UsuarioRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cadena real de agenda en un perfil distinto de dev/test: sesión de C-04 + RBAC + CSRF y
 * actor REAL entregado a los servicios por {@link AuthenticatedCurrentActorProvider}.
 */
@SpringBootTest(classes = AgendaSecurityChainTest.Config.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "spring.flyway.enabled=false",
                "spring.datasource.url=jdbc:h2:mem:agendachain;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
                "AUTH_JWT_SECRET=clave-de-prueba-no-real-para-tests",
                "auth.session-cookie=auth_session",
                "auth.csrf-cookie=auth_csrf"
        })
@AutoConfigureMockMvc
@ActiveProfiles("agenda-protegida")
class AgendaSecurityChainTest {

    private static final String CLAVE = "ClaveSegura123!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ApplicationContext context;

    @Autowired
    private RolService rolService;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private AuthService authService;

    @Autowired
    private CsrfTokenStore csrf;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void limpiar() {
        jdbc.update("DELETE FROM auth_tokens_refresh");
        jdbc.update("DELETE FROM auth_jti_revocados");
        jdbc.update("DELETE FROM auth_sesiones");
    }

    @Test
    void fueraDeDevTestHayDosCadenasYLaDeAgendaNoTocaAuth() {
        List<SecurityFilterChain> cadenas = context.getBean("springSecurityFilterChain", FilterChainProxy.class)
                .getFilterChains();
        assertEquals(2, cadenas.size());

        DefaultSecurityFilterChain agenda = cadenas.stream().map(DefaultSecurityFilterChain.class::cast)
                .filter(c -> c.getRequestMatcher().matches(new MockHttpServletRequest("GET", "/agenda")))
                .findFirst().orElseThrow();
        for (String ruta : List.of("/agenda/dia", "/turnos", "/bloqueos", "/sobreturnos",
                "/tratamientos/1", "/horarios", "/feriados")) {
            assertTrue(agenda.getRequestMatcher().matches(new MockHttpServletRequest("GET", ruta)), ruta);
        }
        assertFalse(agenda.getRequestMatcher().matches(new MockHttpServletRequest("GET", "/auth/me")));
    }

    @Test
    void elProveedorDeActorEsElRealYNoElSimulado() {
        assertInstanceOf(AuthenticatedCurrentActorProvider.class, context.getBean(CurrentActorProvider.class));
    }

    @Test
    void sinSesionResponde401() throws Exception {
        mockMvc.perform(get("/agenda/actor"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("token-invalido"));
    }

    @Test
    void tokenAdulteradoResponde401() throws Exception {
        mockMvc.perform(get("/agenda/actor").cookie(new Cookie("auth_session", "a.b.c")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rolSinPermisoResponde403() throws Exception {
        String access = login("odontologo-" + System.nanoTime() + "@example.com", "odontologo").access();

        mockMvc.perform(post("/turnos").cookie(sesion(access), csrfCookie(csrf.emitir()))
                        .header("X-CSRF-Token", ultimoCsrf))
                .andExpect(status().isForbidden())
                .andExpect(content().string("acceso-denegado"));
    }

    @Test
    void escrituraSinCsrfResponde403AunConPermiso() throws Exception {
        String access = login("recep-csrf-" + System.nanoTime() + "@example.com", "recepcionista").access();

        mockMvc.perform(post("/turnos").cookie(sesion(access)))
                .andExpect(status().isForbidden())
                .andExpect(content().string("csrf-invalido"));
    }

    @Test
    void recepcionistaAutenticadoLlegaAlServicioConActorReal() throws Exception {
        String email = "recep-" + System.nanoTime() + "@example.com";
        AuthService.LoginResult resultado = login(email, "recepcionista");

        mockMvc.perform(get("/agenda/actor").cookie(sesion(resultado.access())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modo").value("REAL"))
                .andExpect(jsonPath("$.usuarioId").value(resultado.usuarioId()))
                .andExpect(jsonPath("$.nombre").value(email))
                .andExpect(jsonPath("$.rol").value("RECEPCIONISTA"))
                .andExpect(jsonPath("$.jti").value(resultado.jti()));

        mockMvc.perform(post("/turnos").cookie(sesion(resultado.access()), csrfCookie(csrf.emitir()))
                        .header("X-CSRF-Token", ultimoCsrf))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modo").value("REAL"));

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void jtiRevocadoPorLogoutResponde401() throws Exception {
        AuthService.LoginResult resultado = login("recep-logout-" + System.nanoTime() + "@example.com",
                "recepcionista");
        authService.logout(resultado.jti());

        mockMvc.perform(get("/agenda/actor").cookie(sesion(resultado.access())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void usuarioDesactivadoResponde401() throws Exception {
        String email = "recep-inactivo-" + System.nanoTime() + "@example.com";
        AuthService.LoginResult resultado = login(email, "recepcionista");
        Usuario usuario = usuarioRepository.findByEmail(email).orElseThrow();
        usuario.desactivar("test");
        usuarioRepository.saveAndFlush(usuario);

        mockMvc.perform(get("/agenda/actor").cookie(sesion(resultado.access())))
                .andExpect(status().isUnauthorized());
    }

    private String ultimoCsrf;

    private Cookie csrfCookie(String valor) {
        ultimoCsrf = valor;
        return new Cookie("auth_csrf", valor);
    }

    private static Cookie sesion(String access) {
        return new Cookie("auth_session", access);
    }

    private AuthService.LoginResult login(String email, String codigoRol) {
        Rol rol = rol(codigoRol);
        usuarioService.registrar(rol, email, CLAVE, ProveedorAutenticacion.LOCAL);
        return authService.login(email, CLAVE, "203.0.113.10", "test");
    }

    private Rol rol(String codigo) {
        Rol rol = rolRepository.findByCodigo(codigo).orElseGet(() -> rolService.registrar(codigo, codigo));
        Map<String, List<String>> matriz = Map.of(
                "recepcionista", List.of("agenda:crear", "agenda:actualizar", "turno:leer"),
                "odontologo", List.of("agenda:leer"));
        for (String permiso : matriz.getOrDefault(codigo, List.of())) {
            String[] partes = permiso.split(":");
            jdbc.update("INSERT INTO permisos (id, recurso, accion, ambito, created_at, activo) "
                            + "SELECT NEXT VALUE FOR permisos_id_seq, ?, ?, 'TODOS', CURRENT_TIMESTAMP, TRUE "
                            + "WHERE NOT EXISTS (SELECT 1 FROM permisos WHERE recurso = ? AND accion = ?)",
                    partes[0], partes[1], partes[0], partes[1]);
            Long id = jdbc.queryForObject("SELECT id FROM permisos WHERE recurso = ? AND accion = ?",
                    Long.class, partes[0], partes[1]);
            jdbc.update("INSERT INTO rol_permiso (rol_id, permiso_id, created_at, activo) "
                    + "SELECT ?, ?, CURRENT_TIMESTAMP, TRUE WHERE NOT EXISTS "
                    + "(SELECT 1 FROM rol_permiso WHERE rol_id = ? AND permiso_id = ?)",
                    rol.getId(), id, rol.getId(), id);
        }
        return rol;
    }

    /** Controlador de prueba: expone el actor que recibiría un servicio de agenda. */
    @RestController
    static class ActorProbeController {

        private final CurrentActorProvider actores;

        ActorProbeController(CurrentActorProvider actores) {
            this.actores = actores;
        }

        @GetMapping("/agenda/actor")
        ActorSimulado actual() {
            return actores.current();
        }

        @PostMapping("/turnos")
        ActorSimulado crear() {
            return actores.current();
        }
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackages = {"com.turnosodontologia.domain", "com.turnosodontologia.auth"})
    @ComponentScan(
            basePackages = {"com.turnosodontologia.application", "com.turnosodontologia.auth"},
            excludeFilters = @ComponentScan.Filter(type = FilterType.ANNOTATION, classes = SpringBootConfiguration.class)
    )
    @EnableJpaRepositories(basePackages = {"com.turnosodontologia.infrastructure.persistence",
            "com.turnosodontologia.auth"})
    @Import(ActorProbeController.class)
    static class Config {

        @Bean
        AuditEventPublisher auditEventPublisher() {
            return new InMemoryAuditEventRecorder();
        }
    }
}
