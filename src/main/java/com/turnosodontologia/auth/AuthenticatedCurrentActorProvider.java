package com.turnosodontologia.auth;

import com.turnosodontologia.application.CurrentActorProvider;
import com.turnosodontologia.domain.auditoria.ActorSimulado;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Implementación real de {@link CurrentActorProvider} fuera de dev/test.
 *
 * <p>Toma el {@link AuthenticatedActor} que {@link AgendaAccessFilter} dejó en el contexto
 * de seguridad y lo adapta con {@link AuthenticatedActorAdapter}. Sin actor autenticado
 * falla cerrado ({@link AccesoDenegadoException}); nunca cae al actor simulado. El actor de
 * sistema conserva el modo {@code SYSTEM} para tareas internas sin request.
 */
@Component
@Profile("!dev & !test")
public class AuthenticatedCurrentActorProvider implements CurrentActorProvider {

    @Override
    public ActorSimulado current() {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion == null || !autenticacion.isAuthenticated()
                || !(autenticacion.getPrincipal() instanceof AuthenticatedActor actor)) {
            throw new AccesoDenegadoException();
        }
        return AuthenticatedActorAdapter.adaptar(actor);
    }

    @Override
    public ActorSimulado system() {
        return AuthenticatedActorAdapter.adaptar(AuthenticatedActor.sistema());
    }
}
