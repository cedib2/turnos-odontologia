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
 * Cadena de seguridad de cumplimiento y métricas (Parte 4). Igual que la de agenda, solo se activa fuera
 * de {@code dev}/{@code test}: exige sesión de C-04, CSRF en escrituras y {@link CumplimientoRbacPolicy}.
 */
@Configuration
@Profile("!dev & !test")
public class CumplimientoSecurityConfig {

    @Bean
    CumplimientoRbacPolicy cumplimientoRbacPolicy(RolPermisoRepository rolPermisos) {
        return new CumplimientoRbacPolicy(rolPermisos);
    }

    @Bean
    SecurityFilterChain cumplimientoFilterChain(HttpSecurity http, AccessAuthenticator acceso,
                                                UsuarioRepository usuarios, CumplimientoRbacPolicy politica,
                                                CsrfTokenStore csrf,
                                                @Value("${auth.session-cookie:auth_session}") String sessionCookie,
                                                @Value("${auth.csrf-cookie:auth_csrf}") String csrfCookie)
            throws Exception {
        return StaffFilterChain.construir(http, politica, acceso, usuarios, csrf, sessionCookie, csrfCookie);
    }
}
