package com.turnosodontologia.auth;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!dev & !test")
public class RealActorProvider {

    public AuthenticatedActor desdeLogin(String nombre, AuthService.LoginResult resultado) {
        return AuthenticatedActor.real(
                resultado.usuarioId(), nombre, resultado.rol(), resultado.jti());
    }

    public AuthenticatedActor sistema() {
        return AuthenticatedActor.sistema();
    }

    public AuthenticatedActor paciente(Long usuarioId, String nombre) {
        return AuthenticatedActor.paciente(usuarioId, nombre);
    }
}
