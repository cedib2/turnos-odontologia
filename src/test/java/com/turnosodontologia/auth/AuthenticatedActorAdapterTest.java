package com.turnosodontologia.auth;

import com.turnosodontologia.domain.auditoria.ActorSimulado;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthenticatedActorAdapterTest {

    @Test
    void actorRealConservaUsuarioNombreJtiYNormalizaRol() {
        ActorSimulado actor = AuthenticatedActorAdapter.adaptar(
                AuthenticatedActor.real(7L, "dra.perez@example.com", "odontologo", "jti-7"));

        assertEquals(7L, actor.usuarioId());
        assertEquals("dra.perez@example.com", actor.nombre());
        assertEquals("ODONTOLOGO", actor.rol());
        assertEquals("jti-7", actor.jti());
        assertEquals("REAL", actor.modo());
    }

    @Test
    void actorSimuladoConservaModoSimuladoSinUsuario() {
        ActorSimulado actor = AuthenticatedActorAdapter.adaptar(
                AuthenticatedActor.simulado("recepcionista", "recepcionista", null));

        assertNull(actor.usuarioId());
        assertEquals("recepcionista", actor.nombre());
        assertEquals("RECEPCIONISTA", actor.rol());
        assertEquals("SIMULADO", actor.modo());
        assertEquals(ActorSimulado.recepcionista(), actor);
    }

    @Test
    void actorSistemaEquivaleAlSystemDelProveedorSimulado() {
        ActorSimulado actor = AuthenticatedActorAdapter.adaptar(AuthenticatedActor.sistema());

        assertEquals(ActorSimulado.system(), actor);
        assertEquals("SYSTEM", actor.modo());
        assertNull(actor.usuarioId());
        assertNull(actor.jti());
    }

    @Test
    void actorPacienteConYSinUsuario() {
        ActorSimulado conUsuario = AuthenticatedActorAdapter.adaptar(
                AuthenticatedActor.paciente(88L, "Ana"));
        ActorSimulado sinUsuario = AuthenticatedActorAdapter.adaptar(
                AuthenticatedActor.paciente(null, "paciente"));

        assertEquals(ActorSimulado.paciente(88L, "Ana"), conUsuario);
        assertEquals("PACIENTE", conUsuario.modo());
        assertEquals(ActorSimulado.pacienteSinUsuario(), sinUsuario);
    }

    @Test
    void actorRealIncompletoNoSeAdapta() {
        assertThrows(IllegalArgumentException.class, () -> AuthenticatedActorAdapter.adaptar(
                AuthenticatedActor.real(null, "x", "recepcionista", "jti")));
        assertThrows(IllegalArgumentException.class, () -> AuthenticatedActorAdapter.adaptar(
                AuthenticatedActor.real(1L, "x", "recepcionista", " ")));
        assertThrows(NullPointerException.class, () -> AuthenticatedActorAdapter.adaptar(null));
    }

    @Test
    void cubreLosCuatroModos() {
        assertEquals(4, ModoActor.values().length);
        for (ModoActor modo : ModoActor.values()) {
            AuthenticatedActor actor = new AuthenticatedActor(1L, "n", "recepcionista", "jti", modo);
            assertEquals(modo.name(), AuthenticatedActorAdapter.adaptar(actor).modo());
        }
    }
}
