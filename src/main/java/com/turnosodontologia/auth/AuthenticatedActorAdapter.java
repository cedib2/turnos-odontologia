package com.turnosodontologia.auth;

import com.turnosodontologia.domain.auditoria.ActorSimulado;

import java.util.Locale;
import java.util.Objects;

/**
 * Convierte el {@link AuthenticatedActor} de C-04 en la instantánea {@link ActorSimulado}
 * que consumen los servicios de agenda a través de
 * {@link com.turnosodontologia.application.CurrentActorProvider}.
 *
 * <p>Conserva el modo de origen ({@code REAL}, {@code SIMULADO}, {@code SYSTEM},
 * {@code PACIENTE}), el {@code usuarioId}, el nombre y el {@code jti}. El rol se normaliza
 * a mayúsculas para coincidir con la convención de la instantánea de auditoría de C-05.
 */
public final class AuthenticatedActorAdapter {

    private AuthenticatedActorAdapter() {
    }

    public static ActorSimulado adaptar(AuthenticatedActor actor) {
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(actor.modo(), "modo");
        return switch (actor.modo()) {
            case REAL -> ActorSimulado.real(requerirUsuario(actor), actor.nombre(),
                    normalizarRol(actor.rol()), requerirJti(actor));
            case SIMULADO -> new ActorSimulado(null, actor.nombre(), normalizarRol(actor.rol()),
                    actor.jti(), ModoActor.SIMULADO.name());
            case SYSTEM -> ActorSimulado.system();
            case PACIENTE -> actor.usuarioId() == null
                    ? ActorSimulado.pacienteSinUsuario()
                    : ActorSimulado.paciente(actor.usuarioId(), actor.nombre());
        };
    }

    private static Long requerirUsuario(AuthenticatedActor actor) {
        if (actor.usuarioId() == null) {
            throw new IllegalArgumentException("Actor REAL sin usuarioId");
        }
        return actor.usuarioId();
    }

    private static String requerirJti(AuthenticatedActor actor) {
        if (actor.jti() == null || actor.jti().isBlank()) {
            throw new IllegalArgumentException("Actor REAL sin jti");
        }
        return actor.jti();
    }

    private static String normalizarRol(String rol) {
        return rol == null ? null : rol.toUpperCase(Locale.ROOT);
    }
}
