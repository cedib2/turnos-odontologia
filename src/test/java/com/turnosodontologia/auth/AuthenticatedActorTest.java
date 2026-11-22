package com.turnosodontologia.auth;

import com.turnosodontologia.application.SimulatedCurrentActorProvider;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Profile;

import java.lang.reflect.Modifier;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthenticatedActorTest {

    @Test
    void snapshotRealEsInmutableYCapturaLaIdentidadDelLogin() throws Exception {
        AuthService.LoginResult resultado = new AuthService.LoginResult(
                "access", "refresh", "jti-real", "recepcionista", 42L);

        AuthenticatedActor actor = new RealActorProvider().desdeLogin("staff@example.com", resultado);

        assertEquals(42L, actor.usuarioId());
        assertEquals("staff@example.com", actor.nombre());
        assertEquals("recepcionista", actor.rol());
        assertEquals("jti-real", actor.jti());
        assertEquals(ModoActor.REAL, actor.modo());
        assertTrue(Modifier.isFinal(AuthenticatedActor.class.getModifiers()));
        assertThrows(NoSuchMethodException.class,
                () -> AuthenticatedActor.class.getMethod("setRol", String.class));
    }

    @Test
    void losModosDelContratoIncluyenRealSimuladoSystemYPaciente() {
        assertArrayEquals(new ModoActor[]{ModoActor.REAL, ModoActor.SIMULADO,
                ModoActor.SYSTEM, ModoActor.PACIENTE}, ModoActor.values());
    }

    @Test
    void proveedorRealEsSeparadoYNoDebilitaAlProveedorSimulado() {
        SimulatedCurrentActorProvider simulado = new SimulatedCurrentActorProvider();
        RealActorProvider real = new RealActorProvider();

        assertEquals("recepcionista", simulado.current().nombre());
        assertEquals("SIMULADO", simulado.current().modo());
        assertEquals("system", simulado.system().nombre());
        assertEquals("SYSTEM", simulado.system().modo());
        assertNull(simulado.system().usuarioId());
        assertEquals(ModoActor.SYSTEM, real.sistema().modo());
        assertEquals(ModoActor.PACIENTE, real.paciente(7L, "paciente@example.com").modo());
        assertEquals(7L, real.paciente(7L, "paciente@example.com").usuarioId());

        Profile perfilSimulado = SimulatedCurrentActorProvider.class.getAnnotation(Profile.class);
        Profile perfilReal = RealActorProvider.class.getAnnotation(Profile.class);
        assertTrue(Arrays.asList(perfilSimulado.value()).containsAll(Arrays.asList("dev", "test")));
        assertTrue(Arrays.asList(perfilReal.value()).stream().anyMatch(valor -> valor.contains("!dev")));
        assertTrue(Arrays.asList(perfilReal.value()).stream().anyMatch(valor -> valor.contains("!test")));
    }
}
