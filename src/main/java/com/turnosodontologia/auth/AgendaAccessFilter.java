package com.turnosodontologia.auth;

import com.turnosodontologia.domain.identidad.Usuario;
import com.turnosodontologia.infrastructure.persistence.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Autentica y autoriza la superficie de agenda y catálogos.
 *
 * <ol>
 *   <li>Lee la cookie de sesión de C-04 y la verifica con {@link AccessAuthenticator}
 *       (firma, expiración, emisor, {@code jti} no revocado) → 401.</li>
 *   <li>Exige usuario existente y activo, con el mismo rol que el token → 401.</li>
 *   <li>En métodos que modifican estado exige CSRF de doble envío → 403.</li>
 *   <li>Evalúa {@link AgendaRbacPolicy} → 403.</li>
 *   <li>Publica el {@link AuthenticatedActor} (modo {@code REAL}) en el contexto de seguridad
 *       para que {@link AuthenticatedCurrentActorProvider} lo adapte a los servicios.</li>
 * </ol>
 */
public class AgendaAccessFilter extends OncePerRequestFilter {

    static final String CUERPO_TOKEN_INVALIDO = "token-invalido";
    static final String CUERPO_ACCESO_DENEGADO = "acceso-denegado";
    static final String CUERPO_CSRF_INVALIDO = "csrf-invalido";
    static final String CABECERA_CSRF = "X-CSRF-Token";

    private static final Set<String> METODOS_SEGUROS = Set.of("GET", "HEAD", "OPTIONS", "TRACE");

    private final AccessAuthenticator acceso;
    private final UsuarioRepository usuarios;
    private final AgendaRbacPolicy politica;
    private final CsrfTokenStore csrf;
    private final String sessionCookie;
    private final String csrfCookie;

    public AgendaAccessFilter(AccessAuthenticator acceso, UsuarioRepository usuarios,
                              AgendaRbacPolicy politica, CsrfTokenStore csrf,
                              String sessionCookie, String csrfCookie) {
        this.acceso = acceso;
        this.usuarios = usuarios;
        this.politica = politica;
        this.csrf = csrf;
        this.sessionCookie = sessionCookie;
        this.csrfCookie = csrfCookie;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !AgendaRbacPolicy.aplicaA(ruta(request));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        JwtService.AccessClaims claims;
        try {
            claims = acceso.verificar(cookie(request, sessionCookie).orElse(null));
        } catch (InvalidAccessTokenException e) {
            responder(response, HttpStatus.UNAUTHORIZED, CUERPO_TOKEN_INVALIDO);
            return;
        }

        Optional<Usuario> usuario = usuarios.findById(claims.usuarioId())
                .filter(Usuario::isActivo)
                .filter(u -> u.getRol() != null && claims.rol().equals(u.getRol().getCodigo()));
        if (usuario.isEmpty()) {
            responder(response, HttpStatus.UNAUTHORIZED, CUERPO_TOKEN_INVALIDO);
            return;
        }

        String metodo = request.getMethod();
        if (!METODOS_SEGUROS.contains(metodo.toUpperCase(Locale.ROOT))
                && !csrf.esValido(request.getHeader(CABECERA_CSRF), cookie(request, csrfCookie).orElse(null))) {
            responder(response, HttpStatus.FORBIDDEN, CUERPO_CSRF_INVALIDO);
            return;
        }

        if (!politica.permite(claims.rol(), metodo, ruta(request))) {
            responder(response, HttpStatus.FORBIDDEN, CUERPO_ACCESO_DENEGADO);
            return;
        }

        AuthenticatedActor actor = AuthenticatedActor.real(
                claims.usuarioId(), usuario.get().getEmail(), claims.rol(), claims.jti());
        SecurityContext contexto = SecurityContextHolder.createEmptyContext();
        contexto.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(actor, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + claims.rol().toUpperCase(Locale.ROOT)))));
        SecurityContextHolder.setContext(contexto);
        try {
            chain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private static String ruta(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String contexto = request.getContextPath();
        if (contexto != null && !contexto.isEmpty() && uri.startsWith(contexto)) {
            return uri.substring(contexto.length());
        }
        return uri;
    }

    private static Optional<String> cookie(HttpServletRequest request, String nombre) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        for (Cookie cookie : cookies) {
            if (nombre.equals(cookie.getName())) {
                return Optional.ofNullable(cookie.getValue());
            }
        }
        return Optional.empty();
    }

    private static void responder(HttpServletResponse response, HttpStatus estado, String cuerpo)
            throws IOException {
        SecurityContextHolder.clearContext();
        response.setStatus(estado.value());
        response.setContentType(MediaType.TEXT_PLAIN_VALUE);
        response.getWriter().write(cuerpo);
    }
}
