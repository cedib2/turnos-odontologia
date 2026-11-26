package com.turnosodontologia.auth;

import com.turnosodontologia.domain.identidad.Usuario;
import com.turnosodontologia.infrastructure.persistence.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final String CUERPO_CREDENCIALES_INVALIDAS = "credenciales-invalidas";
    private static final String CUERPO_CSRF_INVALIDO = "csrf-invalido";
    private static final String CUERPO_TOKEN_INVALIDO = "token-invalido";
    private static final String CUERPO_REFRESH_INVALIDO = "refresh-invalido";
    private static final String CUERPO_ACCESO_DENEGADO = "acceso-denegado";
    private static final String CUERPO_DEMASIADOS_INTENTOS = "demasiados-intentos";

    private final AuthService servicio;
    private final CsrfTokenStore csrf;
    private final AccessAuthenticator acceso;
    private final AuthorizationService autorizaciones;
    private final UsuarioRepository usuarios;
    private final LoginRateLimiter limite;

    @Value("${auth.session-cookie:auth_session}")
    private String sessionCookie;

    @Value("${auth.refresh-cookie:auth_refresh}")
    private String refreshCookie;

    @Value("${auth.csrf-cookie:auth_csrf}")
    private String csrfCookie;

    @Value("${auth.refresh-days:7}")
    private long refreshDays;

    public AuthController(AuthService servicio, CsrfTokenStore csrf, AccessAuthenticator acceso,
                          AuthorizationService autorizaciones, UsuarioRepository usuarios,
                          LoginRateLimiter limite) {
        this.servicio = servicio;
        this.csrf = csrf;
        this.acceso = acceso;
        this.autorizaciones = autorizaciones;
        this.usuarios = usuarios;
        this.limite = limite;
    }

    @GetMapping("/csrf")
    public ResponseEntity<Void> csrf() {
        String valor = csrf.emitir();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookieCSRF(valor).toString())
                .build();
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(
            @CookieValue(name = "${auth.session-cookie:auth_session}", required = false) String access) {
        try {
            JwtService.AccessClaims claims = acceso.verificar(access);
            autorizaciones.exigirPropio(claims, claims.usuarioId());
            return ResponseEntity.ok(Map.of(
                    "usuario_id", claims.usuarioId(),
                    "rol", claims.rol()));
        } catch (InvalidAccessTokenException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(CUERPO_TOKEN_INVALIDO);
        } catch (AccesoDenegadoException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(CUERPO_ACCESO_DENEGADO);
        }
    }

    @GetMapping("/usuarios/{id}")
    public ResponseEntity<?> usuario(
            @PathVariable Long id,
            @CookieValue(name = "${auth.session-cookie:auth_session}", required = false) String access) {
        try {
            JwtService.AccessClaims claims = acceso.verificar(access);
            autorizaciones.exigirTodos(claims);
            return usuarios.findById(id)
                    .map(this::identidadStaff)
                    .map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (InvalidAccessTokenException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(CUERPO_TOKEN_INVALIDO);
        } catch (AccesoDenegadoException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(CUERPO_ACCESO_DENEGADO);
        }
    }

    private Map<String, Object> identidadStaff(Usuario usuario) {
        return Map.of(
                "usuario_id", usuario.getId(),
                "email", usuario.getEmail(),
                "rol", usuario.getRol().getCodigo());
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody(required = false) LoginRequest request,
            @RequestHeader(value = "X-CSRF-Token", required = false) String cabeceraCSRF,
            @CookieValue(name = "${auth.csrf-cookie:auth_csrf}", required = false) String cookieCSRF,
            @RequestHeader(value = "X-Forwarded-For", required = false) String ip,
            @RequestHeader(value = "User-Agent", required = false) String userAgent) {
        String ipEfectiva = ip == null ? "desconocida" : ip;
        String usuario = request == null || request.email() == null ? "desconocido" : request.email();
        LoginRateLimiter.Decision decision = limite.evaluar(ipEfectiva, usuario);
        if (decision.bloqueado()) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .header(HttpHeaders.RETRY_AFTER, Long.toString(decision.reintentarEnSegundos()))
                    .body(CUERPO_DEMASIADOS_INTENTOS);
        }
        if (!csrf.esValido(cabeceraCSRF, cookieCSRF)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(CUERPO_CSRF_INVALIDO);
        }
        if (request == null || request.email() == null || request.password() == null) {
            limite.registrarFallo(ipEfectiva, usuario);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(CUERPO_CREDENCIALES_INVALIDAS);
        }
        try {
            AuthService.LoginResult resultado = servicio.login(
                    request.email(), request.password(), ipEfectiva,
                    userAgent == null ? "desconocido" : userAgent);
            limite.registrarExito(ipEfectiva, usuario);
            String csrfRotado = csrf.rotar(cabeceraCSRF);
            return ResponseEntity.ok()
                    .headers(cookies(resultado, csrfRotado))
                    .body(Map.of("token_type", "Bearer"));
        } catch (InvalidCredentialsException e) {
            limite.registrarFallo(ipEfectiva, usuario);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(CUERPO_CREDENCIALES_INVALIDAS);
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(
            @RequestHeader(value = "X-CSRF-Token", required = false) String cabeceraCSRF,
            @CookieValue(name = "${auth.csrf-cookie:auth_csrf}", required = false) String cookieCSRF,
            @CookieValue(name = "${auth.refresh-cookie:auth_refresh}", required = false) String refresh) {
        if (!csrf.esValido(cabeceraCSRF, cookieCSRF)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        AuthService.RotationResult rotacion = servicio.rotar(refresh);
        if (!rotacion.esValido()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(CUERPO_REFRESH_INVALIDO);
        }
        return ResponseEntity.ok()
                .headers(cookies(rotacion.credenciales(), csrf.rotar(cabeceraCSRF)))
                .body(Map.of("token_type", "Bearer"));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = "${auth.session-cookie:auth_session}", required = false) String access,
            @RequestHeader(value = "X-CSRF-Token", required = false) String cabeceraCSRF,
            @CookieValue(name = "${auth.csrf-cookie:auth_csrf}", required = false) String cookieCSRF) {
        if (!csrf.esValido(cabeceraCSRF, cookieCSRF)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        try {
            JwtService.AccessClaims claims = acceso.verificar(access);
            servicio.logout(claims.jti());
            csrf.rotar(cabeceraCSRF);
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.SET_COOKIE, cookieExpirada(sessionCookie, true).toString());
            headers.add(HttpHeaders.SET_COOKIE, cookieExpirada(refreshCookie, true).toString());
            headers.add(HttpHeaders.SET_COOKIE, cookieExpirada(csrfCookie, false).toString());
            return ResponseEntity.noContent().headers(headers).build();
        } catch (InvalidAccessTokenException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    private HttpHeaders cookies(AuthService.LoginResult resultado, String csrfRotado) {
        HttpHeaders headers = new HttpHeaders();
        // Con Max-Age las cookies sobreviven al cierre del navegador: la sesión termina con el logout
        // o cuando vence el refresh (auth.refresh-days, que se renueva con cada rotación).
        headers.add(HttpHeaders.SET_COOKIE, ResponseCookie.from(sessionCookie, resultado.access())
                .httpOnly(true).secure(true).sameSite("Strict").path("/")
                .maxAge(AuthService.ACCESS_SECONDS).build().toString());
        headers.add(HttpHeaders.SET_COOKIE, ResponseCookie.from(refreshCookie, resultado.refresh())
                .httpOnly(true).secure(true).sameSite("Strict").path("/")
                .maxAge(refreshSegundos()).build().toString());
        headers.add(HttpHeaders.SET_COOKIE, cookieCSRF(csrfRotado).toString());
        return headers;
    }

    private ResponseCookie cookieExpirada(String nombre, boolean httpOnly) {
        return ResponseCookie.from(nombre, "")
                .httpOnly(httpOnly).secure(true).sameSite("Strict").path("/").maxAge(0).build();
    }

    private ResponseCookie cookieCSRF(String valor) {
        return ResponseCookie.from(csrfCookie, valor)
                .httpOnly(false).secure(true).sameSite("Strict").path("/")
                .maxAge(refreshSegundos()).build();
    }

    private long refreshSegundos() {
        return refreshDays * 24 * 60 * 60;
    }

    public record LoginRequest(String email, String password) {
    }
}
