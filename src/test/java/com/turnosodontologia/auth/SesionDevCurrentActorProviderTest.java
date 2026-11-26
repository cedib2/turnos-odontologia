package com.turnosodontologia.auth;

import com.turnosodontologia.application.SimulatedCurrentActorProvider;
import com.turnosodontologia.domain.auditoria.ActorSimulado;
import com.turnosodontologia.domain.identidad.Usuario;
import com.turnosodontologia.infrastructure.persistence.UsuarioRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SesionDevCurrentActorProviderTest {

    private final AccessAuthenticator acceso = mock(AccessAuthenticator.class);
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final SesionDevCurrentActorProvider provider =
            new SesionDevCurrentActorProvider(acceso, usuarios, new SimulatedCurrentActorProvider(), "auth_session");

    @AfterEach
    void limpiar() {
        RequestContextHolder.resetRequestAttributes();
    }

    private static Usuario usuario(String email, boolean activo) {
        Usuario usuario = mock(Usuario.class);
        when(usuario.getEmail()).thenReturn(email);
        when(usuario.isActivo()).thenReturn(activo);
        return usuario;
    }

    private void requestCon(Cookie... cookies) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (cookies.length > 0) {
            request.setCookies(cookies);
        }
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @Test
    void conSesionValidaActuaElUsuarioLogueadoConSuRolReal() {
        requestCon(new Cookie("auth_session", "access-valido"));
        when(acceso.verificar("access-valido"))
                .thenReturn(new JwtService.AccessClaims(4L, "odontologo", "jti-4", Instant.now().plusSeconds(600)));
        Usuario odontologa = usuario("lgimenez@turnos-odontologia.test", true);
        when(usuarios.findById(4L)).thenReturn(Optional.of(odontologa));

        ActorSimulado actor = provider.current();

        assertEquals(ActorSimulado.real(4L, "lgimenez@turnos-odontologia.test", "ODONTOLOGO", "jti-4"), actor);
    }

    @Test
    void sinSesionSigueElActorSimulado() {
        requestCon();

        assertEquals(ActorSimulado.recepcionista(), provider.current());
    }

    @Test
    void sesionInvalidaOVencidaVuelveAlActorSimulado() {
        requestCon(new Cookie("auth_session", "vencido"));
        when(acceso.verificar("vencido")).thenThrow(new InvalidAccessTokenException());

        assertEquals(ActorSimulado.recepcionista(), provider.current());
    }

    @Test
    void usuarioDadoDeBajaNoActuaConSuSesion() {
        requestCon(new Cookie("auth_session", "access-valido"));
        when(acceso.verificar("access-valido"))
                .thenReturn(new JwtService.AccessClaims(10L, "recepcionista", "jti-10", Instant.now().plusSeconds(600)));
        Usuario baja = usuario("baja@turnos-odontologia.test", false);
        when(usuarios.findById(10L)).thenReturn(Optional.of(baja));

        assertEquals(ActorSimulado.recepcionista(), provider.current());
    }

    @Test
    void fueraDeUnaRequestUsaElActorSimuladoYSystemNoCambia() {
        assertEquals(ActorSimulado.recepcionista(), provider.current());
        assertEquals(ActorSimulado.system(), provider.system());
    }

    @Test
    void soloSeActivaEnDevYTienePrioridadSobreElSimulado() {
        Profile perfil = SesionDevCurrentActorProvider.class.getAnnotation(Profile.class);
        assertEquals(List.of("dev"), List.of(perfil.value()));
        assertNotNull(SesionDevCurrentActorProvider.class.getAnnotation(Primary.class));
    }
}
