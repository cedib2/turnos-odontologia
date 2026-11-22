package com.turnosodontologia.auth;

import com.turnosodontologia.application.CurrentActorProvider;
import com.turnosodontologia.application.SimulatedCurrentActorProvider;
import com.turnosodontologia.domain.auditoria.ActorSimulado;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthenticatedCurrentActorProviderTest {

    private final CurrentActorProvider provider = new AuthenticatedCurrentActorProvider();

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void sinActorAutenticadoFallaCerradoYNoCaeAlSimulado() {
        assertThrows(AccesoDenegadoException.class, provider::current);
    }

    @Test
    void principalQueNoEsAuthenticatedActorSeRechaza() {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated("alguien", null, List.of()));

        assertThrows(AccesoDenegadoException.class, provider::current);
    }

    @Test
    void autenticacionNoConfirmadaSeRechaza() {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.unauthenticated(
                        AuthenticatedActor.real(1L, "x", "recepcionista", "j"), null));

        assertThrows(AccesoDenegadoException.class, provider::current);
    }

    @Test
    void adaptaElActorRealDelContexto() {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        AuthenticatedActor.real(5L, "recepcion@example.com", "recepcionista", "jti-5"),
                        null, List.of()));

        ActorSimulado actor = provider.current();

        assertEquals(ActorSimulado.real(5L, "recepcion@example.com", "RECEPCIONISTA", "jti-5"), actor);
    }

    @Test
    void systemConservaModoSystemSinRequest() {
        assertEquals(ActorSimulado.system(), provider.system());
    }

    @Test
    void proveedoresRealYSimuladoSonMutuamenteExcluyentesPorPerfil() {
        Profile real = AuthenticatedCurrentActorProvider.class.getAnnotation(Profile.class);
        Profile simulado = SimulatedCurrentActorProvider.class.getAnnotation(Profile.class);
        Profile cadena = AgendaSecurityConfig.class.getAnnotation(Profile.class);

        assertEquals(List.of("!dev & !test"), List.of(real.value()));
        assertEquals(List.of("!dev & !test"), List.of(cadena.value()));
        assertEquals(List.of("dev", "test"), List.of(simulado.value()));
    }
}
