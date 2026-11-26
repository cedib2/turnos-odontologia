package com.turnosodontologia.auth;

import com.turnosodontologia.application.CurrentActorProvider;
import com.turnosodontologia.application.SimulatedCurrentActorProvider;
import com.turnosodontologia.domain.auditoria.ActorSimulado;
import com.turnosodontologia.domain.identidad.Usuario;
import com.turnosodontologia.infrastructure.persistence.UsuarioRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

/**
 * Actor de dev: si la request trae una sesión válida (cookie de C-04), actúa el usuario que inició sesión con su
 * rol real (administrador, odontólogo vinculado a su profesional, etc.); si no hay sesión, se usa el actor
 * simulado de siempre ({@link SimulatedCurrentActorProvider}, recepcionista por defecto). Sin esto, en dev todas
 * las pantallas de staff actuaban como recepcionista aunque se hubiera ingresado con otro rol.
 *
 * <p>Solo en el perfil {@code dev}: en {@code test} sigue el actor simulado fijo y fuera de dev/test actúa
 * {@link AuthenticatedCurrentActorProvider} con la cadena de seguridad completa.
 */
@Component
@Primary
@Profile("dev")
public class SesionDevCurrentActorProvider implements CurrentActorProvider {

    private final AccessAuthenticator acceso;
    private final UsuarioRepository usuarios;
    private final SimulatedCurrentActorProvider simulado;
    private final String sessionCookie;

    public SesionDevCurrentActorProvider(AccessAuthenticator acceso, UsuarioRepository usuarios,
                                         SimulatedCurrentActorProvider simulado,
                                         @Value("${auth.session-cookie:auth_session}") String sessionCookie) {
        this.acceso = acceso;
        this.usuarios = usuarios;
        this.simulado = simulado;
        this.sessionCookie = sessionCookie;
    }

    @Override
    public ActorSimulado current() {
        return actorDeSesion().orElseGet(simulado::current);
    }

    @Override
    public ActorSimulado system() {
        return simulado.system();
    }

    private Optional<ActorSimulado> actorDeSesion() {
        Optional<String> valor = cookieDeSesion();
        if (valor.isEmpty()) {
            return Optional.empty();
        }
        try {
            JwtService.AccessClaims claims = acceso.verificar(valor.get());
            return usuarios.findById(claims.usuarioId())
                    .filter(Usuario::isActivo)
                    .map(usuario -> AuthenticatedActorAdapter.adaptar(AuthenticatedActor.real(
                            claims.usuarioId(), usuario.getEmail(), claims.rol(), claims.jti())));
        } catch (InvalidAccessTokenException e) {
            return Optional.empty();
        }
    }

    private Optional<String> cookieDeSesion() {
        RequestAttributes atributos = RequestContextHolder.getRequestAttributes();
        if (!(atributos instanceof ServletRequestAttributes servlet)) {
            return Optional.empty();
        }
        HttpServletRequest request = servlet.getRequest();
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        for (Cookie cookie : cookies) {
            if (sessionCookie.equals(cookie.getName()) && cookie.getValue() != null && !cookie.getValue().isBlank()) {
                return Optional.of(cookie.getValue());
            }
        }
        return Optional.empty();
    }
}
