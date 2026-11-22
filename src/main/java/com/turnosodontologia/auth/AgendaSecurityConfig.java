package com.turnosodontologia.auth;

import com.turnosodontologia.infrastructure.persistence.RolPermisoRepository;
import com.turnosodontologia.infrastructure.persistence.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;

/**
 * Cadena de seguridad de la superficie de agenda y catálogos (deuda C-05 / cierre de C-08).
 *
 * <p>Solo se activa fuera de los perfiles {@code dev}/{@code test}, igual que
 * {@link RealActorProvider}: en dev/test la agenda conserva el actor {@code SIMULADO} de
 * C-08 y la cadena de {@code /auth/**} sigue siendo la única. Fuera de esos perfiles toda
 * ruta de agenda exige sesión válida de C-04 y permiso RBAC; el CSRF lo valida
 * {@link AgendaAccessFilter} con el mismo esquema de doble envío de C-04.
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
        AgendaAccessFilter filtro = new AgendaAccessFilter(acceso, usuarios, politica, csrf,
                sessionCookie, csrfCookie);
        return http
                .securityMatcher(AgendaRbacPolicy.PATRONES)
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .addFilterBefore(filtro, AnonymousAuthenticationFilter.class)
                .authorizeHttpRequests(requests -> requests.anyRequest().authenticated())
                .build();
    }
}
