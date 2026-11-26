package com.turnosodontologia.auth;

import com.turnosodontologia.infrastructure.persistence.RolPermisoRepository;
import com.turnosodontologia.infrastructure.persistence.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Cadena de seguridad de la superficie de agenda y catálogos (deuda C-05 / cierre de C-08).
 *
 * <p>Solo se activa fuera de los perfiles {@code dev}/{@code test}, igual que
 * {@link RealActorProvider}: en dev/test la agenda conserva el actor {@code SIMULADO} de
 * C-08 y la cadena de {@code /auth/**} sigue siendo la única. Fuera de esos perfiles toda
 * ruta de agenda exige sesión válida de C-04 y permiso RBAC; el CSRF lo valida
 * {@link RbacAccessFilter} con el mismo esquema de doble envío de C-04.
 */
@Configuration
@Profile("!dev & !test")
public class AgendaSecurityConfig {

    @Bean
    AgendaRbacPolicy agendaRbacPolicy(RolPermisoRepository rolPermisos) {
        return new AgendaRbacPolicy(rolPermisos);
    }

    @Bean
    SecurityFilterChain agendaFilterChain(HttpSecurity http, AccessAuthenticator acceso,
                                          UsuarioRepository usuarios, AgendaRbacPolicy politica,
                                          CsrfTokenStore csrf,
                                          @Value("${auth.session-cookie:auth_session}") String sessionCookie,
                                          @Value("${auth.csrf-cookie:auth_csrf}") String csrfCookie)
            throws Exception {
        return StaffFilterChain.construir(http, politica, acceso, usuarios, csrf, sessionCookie, csrfCookie);
    }
}
